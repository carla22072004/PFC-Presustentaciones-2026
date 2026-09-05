package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.RolDTO;
import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RolController tenía 2 de 47 líneas cubiertas y 16 ramas en cero. Concentra dos
 * salvaguardas que el resto del sistema da por hechas: los 4 roles base (ADMIN,
 * DOCENTE, COORDINADOR, ESTUDIANTE) no se pueden eliminar porque el frontend y
 * Usuario.rol todavía distinguen casos por esos códigos exactos, y ningún rol con
 * usuarios asignados puede borrarse.
 */
@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock private RolUsuarioRepository rolUsuarioRepository;
    @Mock private PermisoRepository permisoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private RolController controller;

    @SuppressWarnings("unchecked")
    private String errorDe(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }

    private RolUsuario rol(short id, String codigo, String nombre) {
        return RolUsuario.builder().id(id).codigo(codigo).nombre(nombre).build();
    }

    @Test
    void listarArmaElDtoConUsuariosAsignadosYPermisosDeCadaRol() {
        when(rolUsuarioRepository.findAll()).thenReturn(List.of(rol((short) 1, "ADMIN", "Administrador")));
        when(usuarioRepository.findByRol("ADMIN")).thenReturn(List.of(
                Usuario.builder().id(1L).build(), Usuario.builder().id(2L).build()));
        when(permisoRepository.findCodigosPorRol((short) 1))
                .thenReturn(List.of("ROLES_PERMISOS_GESTIONAR", "SOLICITUDES_REVISAR"));

        List<RolDTO> roles = controller.listar();

        assertEquals(1, roles.size());
        RolDTO dto = roles.get(0);
        assertEquals("ADMIN", dto.getCodigo());
        assertEquals("Administrador", dto.getNombre());
        assertEquals(2, dto.getUsuariosAsignados());
        assertEquals(2, dto.getPermisos().size());
    }

    // ── Creación ──────────────────────────────────────────────────────────────

    @Test
    void crearNormalizaElCodigoYCalculaElSiguienteIdDisponible() {
        when(rolUsuarioRepository.findByCodigo("SECRETARIA_ACADEMICA")).thenReturn(Optional.empty());
        when(rolUsuarioRepository.findAll()).thenReturn(List.of(
                rol((short) 1, "ADMIN", "Administrador"), rol((short) 4, "ESTUDIANTE", "Estudiante")));
        when(rolUsuarioRepository.save(any(RolUsuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByRol("SECRETARIA_ACADEMICA")).thenReturn(List.of());
        when(permisoRepository.findCodigosPorRol((short) 5)).thenReturn(List.of());

        ResponseEntity<?> response = controller.crear(Map.of(
                "codigo", " secretaria academica ", "nombre", " Secretaría Académica "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        RolDTO dto = (RolDTO) response.getBody();
        assertEquals("SECRETARIA_ACADEMICA", dto.getCodigo());
        assertEquals("Secretaría Académica", dto.getNombre());
        // El mayor id existente es 4, así que el nuevo rol toma el 5
        assertEquals((short) 5, dto.getId());
        verify(auditoriaService).marcarActorActual();
    }

    @Test
    void crearRechazaCodigoONombreVacios() {
        ResponseEntity<?> sinCodigo = controller.crear(Map.of("nombre", "Secretaría"));
        ResponseEntity<?> sinNombre = controller.crear(Map.of("codigo", "SECRETARIA"));

        assertEquals(HttpStatus.BAD_REQUEST, sinCodigo.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, sinNombre.getStatusCode());
        assertEquals("Código y nombre son obligatorios.", errorDe(sinCodigo));
        verify(rolUsuarioRepository, never()).save(any());
    }

    @Test
    void crearRechazaCodigoDuplicado() {
        when(rolUsuarioRepository.findByCodigo("ADMIN"))
                .thenReturn(Optional.of(rol((short) 1, "ADMIN", "Administrador")));

        ResponseEntity<?> response = controller.crear(Map.of("codigo", "admin", "nombre", "Otro admin"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Ya existe un rol con ese código.", errorDe(response));
        verify(rolUsuarioRepository, never()).save(any());
    }

    // ── Renombrado ────────────────────────────────────────────────────────────

    @Test
    void renombrarCambiaSoloElNombreVisibleNoElCodigo() {
        RolUsuario existente = rol((short) 5, "SECRETARIA", "Secretaria");
        when(rolUsuarioRepository.findById((short) 5)).thenReturn(Optional.of(existente));
        when(rolUsuarioRepository.save(existente)).thenReturn(existente);
        when(usuarioRepository.findByRol("SECRETARIA")).thenReturn(List.of());
        when(permisoRepository.findCodigosPorRol((short) 5)).thenReturn(List.of());

        ResponseEntity<?> response = controller.renombrar((short) 5,
                Map.of("nombre", " Secretaría Académica ", "codigo", "OTRO_CODIGO"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        RolDTO dto = (RolDTO) response.getBody();
        assertEquals("Secretaría Académica", dto.getNombre());
        // El código no se toca aunque venga en el body: lo usan @PreAuthorize y Usuario.rol
        assertEquals("SECRETARIA", dto.getCodigo());
    }

    @Test
    void renombrarRolInexistenteDevuelve404() {
        when(rolUsuarioRepository.findById((short) 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND,
                controller.renombrar((short) 99, Map.of("nombre", "X")).getStatusCode());
    }

    @Test
    void renombrarRechazaNombreVacio() {
        when(rolUsuarioRepository.findById((short) 5))
                .thenReturn(Optional.of(rol((short) 5, "SECRETARIA", "Secretaria")));

        ResponseEntity<?> response = controller.renombrar((short) 5, Map.of("nombre", "   "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El nombre no puede estar vacío.", errorDe(response));
        verify(rolUsuarioRepository, never()).save(any());
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    @Test
    void noSePuedeEliminarNingunoDeLosCuatroRolesBase() {
        for (String codigo : List.of("ADMIN", "DOCENTE", "COORDINADOR", "ESTUDIANTE")) {
            when(rolUsuarioRepository.findById((short) 1))
                    .thenReturn(Optional.of(rol((short) 1, codigo, codigo)));

            ResponseEntity<?> response = controller.eliminar((short) 1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(errorDe(response).contains("roles base del sistema"));
        }
        verify(rolUsuarioRepository, never()).delete(any());
    }

    @Test
    void noSePuedeEliminarUnRolConUsuariosAsignados() {
        when(rolUsuarioRepository.findById((short) 5))
                .thenReturn(Optional.of(rol((short) 5, "SECRETARIA", "Secretaría")));
        when(usuarioRepository.findByRol("SECRETARIA")).thenReturn(List.of(
                Usuario.builder().id(1L).build(), Usuario.builder().id(2L).build()));

        ResponseEntity<?> response = controller.eliminar((short) 5);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorDe(response).contains("hay 2 usuario(s) con este rol"));
        verify(rolUsuarioRepository, never()).delete(any());
    }

    @Test
    void eliminarRolInexistenteDevuelve404() {
        when(rolUsuarioRepository.findById((short) 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, controller.eliminar((short) 99).getStatusCode());
    }

    @Test
    void unRolNuevoSinUsuariosSiSePuedeEliminar() {
        RolUsuario rol = rol((short) 5, "SECRETARIA", "Secretaría");
        when(rolUsuarioRepository.findById((short) 5)).thenReturn(Optional.of(rol));
        when(usuarioRepository.findByRol("SECRETARIA")).thenReturn(List.of());

        ResponseEntity<?> response = controller.eliminar((short) 5);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(auditoriaService).marcarActorActual();
        verify(rolUsuarioRepository).delete(rol);
    }

    @Test
    void siLaBaseRechazaElBorradoPorReferenciasSeDevuelveUnMensajeLegible() {
        RolUsuario rol = rol((short) 5, "SECRETARIA", "Secretaría");
        when(rolUsuarioRepository.findById((short) 5)).thenReturn(Optional.of(rol));
        when(usuarioRepository.findByRol("SECRETARIA")).thenReturn(List.of());
        doThrow(new org.springframework.dao.DataIntegrityViolationException("FK rol_permisos"))
                .when(rolUsuarioRepository).delete(rol);

        ResponseEntity<?> response = controller.eliminar((short) 5);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorDe(response).contains("referencias asociadas"));
    }
}
