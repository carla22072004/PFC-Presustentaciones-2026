package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Permiso;
import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
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
 * PermisoController tenía 1 de 20 líneas cubiertas y 12 ramas en cero, pese a contener
 * la salvaguarda que impide dejar al sistema sin ningún rol capaz de gestionar permisos.
 *
 * Ese es el caso crítico que se cubre aquí: si se acepta quitar ROLES_PERMISOS_GESTIONAR
 * del último rol que lo tiene, nadie puede volver a asignarlo desde la interfaz y el
 * sistema queda bloqueado sin más salida que tocar la base de datos a mano.
 */
@ExtendWith(MockitoExtension.class)
class PermisoControllerTest {

    private static final String GESTION = "ROLES_PERMISOS_GESTIONAR";

    @Mock private PermisoRepository permisoRepository;
    @Mock private RolUsuarioRepository rolUsuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private PermisoController controller;

    @SuppressWarnings("unchecked")
    private String errorDe(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }

    private Permiso permiso(short id, String codigo) {
        return Permiso.builder().id(id).codigo(codigo).build();
    }

    @Test
    void listarDevuelveElCatalogoOrdenadoPorCategoriaYNombre() {
        List<Permiso> catalogo = List.of(permiso((short) 1, "SOLICITUDES_REVISAR"));
        when(permisoRepository.findAllByOrderByCategoriaAscNombreAsc()).thenReturn(catalogo);

        assertSame(catalogo, controller.listar());
    }

    @Test
    void actualizarPermisosDeRolInexistenteDevuelve404() {
        when(rolUsuarioRepository.findById((short) 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND,
                controller.actualizarPermisosDeRol((short) 99, List.of("SOLICITUDES_REVISAR")).getStatusCode());
        verify(permisoRepository, never()).eliminarPermisosDeRol(any());
    }

    @Test
    void actualizarPermisosRechazaCodigosQueNoExisten() {
        when(rolUsuarioRepository.findById((short) 1))
                .thenReturn(Optional.of(RolUsuario.builder().id((short) 1).build()));
        // Se piden 2 códigos pero el repositorio solo resuelve 1: hay uno inventado
        when(permisoRepository.findByCodigoIn(List.of("SOLICITUDES_REVISAR", "INVENTADO")))
                .thenReturn(List.of(permiso((short) 1, "SOLICITUDES_REVISAR")));

        ResponseEntity<?> response = controller.actualizarPermisosDeRol(
                (short) 1, List.of("SOLICITUDES_REVISAR", "INVENTADO"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Uno o más códigos de permiso no existen.", errorDe(response));
        verify(permisoRepository, never()).eliminarPermisosDeRol(any());
    }

    @Test
    void actualizarPermisosAceptaCodigosDuplicadosEnLaPeticion() {
        // El frontend puede mandar el mismo código repetido; el conteo se hace sobre
        // los distintos, así que no debe tratarse como "código inexistente".
        when(rolUsuarioRepository.findById((short) 1))
                .thenReturn(Optional.of(RolUsuario.builder().id((short) 1).build()));
        when(permisoRepository.findByCodigoIn(List.of(GESTION, GESTION)))
                .thenReturn(List.of(permiso((short) 1, GESTION)));
        when(permisoRepository.findCodigosPorRol((short) 1)).thenReturn(List.of(GESTION));

        ResponseEntity<?> response = controller.actualizarPermisosDeRol((short) 1, List.of(GESTION, GESTION));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void noSePuedeQuitarLaGestionDePermisosAlUltimoRolQueLaTiene() {
        when(rolUsuarioRepository.findById((short) 1))
                .thenReturn(Optional.of(RolUsuario.builder().id((short) 1).build()));
        when(permisoRepository.findByCodigoIn(List.of("SOLICITUDES_REVISAR")))
                .thenReturn(List.of(permiso((short) 1, "SOLICITUDES_REVISAR")));
        // El único rol que hoy tiene el permiso es el que se está editando
        when(permisoRepository.findRolIdsConPermiso(GESTION)).thenReturn(List.of((short) 1));

        ResponseEntity<?> response = controller.actualizarPermisosDeRol(
                (short) 1, List.of("SOLICITUDES_REVISAR"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorDe(response).contains("ningún otro rol lo tendría"));
        verify(permisoRepository, never()).eliminarPermisosDeRol(any());
        verify(auditoriaService, never()).marcarActorActual();
    }

    @Test
    void siOtroRolConservaLaGestionDePermisosSiSePuedeQuitarDeEste() {
        when(rolUsuarioRepository.findById((short) 2))
                .thenReturn(Optional.of(RolUsuario.builder().id((short) 2).build()));
        when(permisoRepository.findByCodigoIn(List.of("SOLICITUDES_REVISAR")))
                .thenReturn(List.of(permiso((short) 5, "SOLICITUDES_REVISAR")));
        // El rol 1 (ADMIN) también lo tiene, así que quitárselo al 2 no bloquea el sistema
        when(permisoRepository.findRolIdsConPermiso(GESTION)).thenReturn(List.of((short) 1, (short) 2));
        when(permisoRepository.findCodigosPorRol((short) 2)).thenReturn(List.of("SOLICITUDES_REVISAR"));

        ResponseEntity<?> response = controller.actualizarPermisosDeRol(
                (short) 2, List.of("SOLICITUDES_REVISAR"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditoriaService).marcarActorActual();
        verify(permisoRepository).eliminarPermisosDeRol((short) 2);
        verify(permisoRepository).asignarPermiso((short) 2, (short) 5);
    }

    @Test
    void actualizarPermisosReemplazaElConjuntoCompletoDelRol() {
        when(rolUsuarioRepository.findById((short) 1))
                .thenReturn(Optional.of(RolUsuario.builder().id((short) 1).build()));
        List<String> codigos = List.of(GESTION, "SOLICITUDES_REVISAR", "ACTAS_GESTIONAR");
        when(permisoRepository.findByCodigoIn(codigos)).thenReturn(List.of(
                permiso((short) 1, GESTION),
                permiso((short) 2, "SOLICITUDES_REVISAR"),
                permiso((short) 3, "ACTAS_GESTIONAR")));
        when(permisoRepository.findCodigosPorRol((short) 1)).thenReturn(codigos);

        ResponseEntity<?> response = controller.actualizarPermisosDeRol((short) 1, codigos);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(codigos, response.getBody());
        // Primero borra todo y luego reinserta: es un reemplazo, no un delta
        var orden = inOrder(permisoRepository);
        orden.verify(permisoRepository).eliminarPermisosDeRol((short) 1);
        orden.verify(permisoRepository).asignarPermiso((short) 1, (short) 1);
        orden.verify(permisoRepository).asignarPermiso((short) 1, (short) 2);
        orden.verify(permisoRepository).asignarPermiso((short) 1, (short) 3);
        // Incluye el permiso de gestión, así que no hace falta consultar los otros roles
        verify(permisoRepository, never()).findRolIdsConPermiso(any());
    }
}
