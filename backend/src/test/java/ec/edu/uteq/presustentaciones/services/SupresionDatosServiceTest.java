package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.SolicitudSupresion;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.SolicitudSupresionRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RNF-19, tercer criterio: procedimiento de supresión. La resolución aceptada seudonimiza (no
 * borra) al titular -- el expediente académico enlazado al mismo id debe seguir existiendo.
 */
class SupresionDatosServiceTest {

    private UsuarioRepository usuarioRepository;
    private SolicitudSupresionRepository solicitudRepository;
    private SupresionDatosService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        solicitudRepository = mock(SolicitudSupresionRepository.class);
        service = new SupresionDatosService(usuarioRepository, solicitudRepository);
        // save() devuelve lo que recibe, como un repositorio real
        when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void solicitarCreaLaSolicitudEnEstadoPendiente() {
        when(usuarioRepository.existsById(5L)).thenReturn(true);
        when(solicitudRepository.existsByUsuarioIdAndEstado(5L, "PENDIENTE")).thenReturn(false);

        SolicitudSupresion solicitud = service.solicitar(5L);

        assertEquals(5L, solicitud.getUsuarioId());
        assertEquals("PENDIENTE", solicitud.getEstado());
        assertNotNull(solicitud.getFechaSolicitud());
    }

    @Test
    void noSePuedeSolicitarDosVecesMientrasHayaUnaPendiente() {
        when(solicitudRepository.existsByUsuarioIdAndEstado(5L, "PENDIENTE")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.solicitar(5L));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void resolverAceptandoSeudonimizaAlTitularSinBorrarLaFila() {
        Usuario titular = new Usuario();
        titular.setId(5L);
        titular.setNombre("Ana");
        titular.setApellido("Perez");
        titular.setEmail("ana.perez@uteq.edu.ec");
        titular.setTelefono("0999999999");
        titular.setActivo(true);

        SolicitudSupresion solicitud = SolicitudSupresion.builder()
                .id(1L).usuarioId(5L).estado("PENDIENTE").build();
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(titular));

        SolicitudSupresion resuelta = service.resolver(1L, true, 99L, "Solicitud legítima");

        assertEquals("RESUELTA", resuelta.getEstado());
        assertEquals("SEUDONIMIZACION", resuelta.getTipoResolucion());
        assertEquals(99L, resuelta.getResueltoPor());
        assertNotNull(resuelta.getFechaResolucion());

        // El usuario NUNCA se borra -- se muta y se guarda, mismo id.
        verify(usuarioRepository, never()).delete(any());
        verify(usuarioRepository, never()).deleteById(any());
        verify(usuarioRepository).save(titular);
        assertEquals(5L, titular.getId()); // el expediente enlazado a este id sigue siendo valido
        assertFalse(titular.getNombre().equals("Ana"));
        assertFalse(titular.getApellido().equals("Perez"));
        assertNotEquals("ana.perez@uteq.edu.ec", titular.getEmail());
        assertNull(titular.getTelefono());
        assertEquals(Boolean.FALSE, titular.getActivo());
    }

    @Test
    void resolverRechazandoNoTocaAlUsuario() {
        SolicitudSupresion solicitud = SolicitudSupresion.builder()
                .id(2L).usuarioId(5L).estado("PENDIENTE").build();
        when(solicitudRepository.findById(2L)).thenReturn(Optional.of(solicitud));

        SolicitudSupresion resuelta = service.resolver(2L, false, 99L, "Proceso de titulación en curso");

        assertEquals("RECHAZADA", resuelta.getEstado());
        assertEquals("RECHAZADA", resuelta.getTipoResolucion());
        verify(usuarioRepository, never()).findById(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noSePuedeResolverDosVeces() {
        SolicitudSupresion yaResuelta = SolicitudSupresion.builder()
                .id(3L).usuarioId(5L).estado("RESUELTA").build();
        when(solicitudRepository.findById(3L)).thenReturn(Optional.of(yaResuelta));

        assertThrows(IllegalStateException.class, () -> service.resolver(3L, true, 99L, "x"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void elRegistroDeLaSolicitudNuncaContieneElDatoSuprimido() {
        // La entidad SolicitudSupresion (ver su clase) no tiene ningun campo de
        // nombre/correo/telefono -- solo usuarioId. Esta prueba documenta esa garantia
        // estructural: intentar guardar el dato ahi no compila.
        SolicitudSupresion s = SolicitudSupresion.builder().usuarioId(5L).estado("PENDIENTE").build();
        assertNotNull(s.getUsuarioId());
        // No existe s.getNombre()/getEmail()/getTelefono() -- la ausencia del getter es la prueba.
    }
}
