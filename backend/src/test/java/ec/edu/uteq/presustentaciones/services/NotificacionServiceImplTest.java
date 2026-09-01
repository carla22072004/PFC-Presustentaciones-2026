package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.NotificacionRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;

/**
 * NotificacionServiceImpl.crearNotificacion() resuelve el remitente desde el contexto de
 * seguridad y solo envia email si el receptor configuro emailNotificaciones -- sin test
 * dedicado pese a ser invocado desde practicamente todos los otros servicios (Solicitud,
 * Acta, Tutoria, Jurado...) para notificar eventos del flujo.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private Usuario receptorCon(String emailNotificaciones) {
        return Usuario.builder().id(1L).nombre("Ana").apellido("Torres")
                .email("atorres@uteq.edu.ec").emailNotificaciones(emailNotificaciones).build();
    }

    @Test
    void crearNotificacionLanzaExcepcionSiElUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificacionService.crearNotificacion(99L, "hola"));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void crearNotificacionGuardaLaNotificacionAunSinEmailConfigurado() {
        Usuario receptor = receptorCon(null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(receptor));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion resultado = notificacionService.crearNotificacion(1L, "Tu solicitud fue aprobada");

        assertEquals("Tu solicitud fue aprobada", resultado.getMensaje());
        assertFalse(resultado.isLeida());
        verify(emailService, never()).enviarNotificacion(any(), any(), any(), any());
    }

    @Test
    void crearNotificacionEnviaEmailSiElReceptorTieneEmailNotificacionesConfigurado() {
        Usuario receptor = receptorCon("atorres.notif@gmail.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(receptor));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        // Sin autenticacion en el contexto -> remitente generico.
        SecurityContextHolder.clearContext();

        notificacionService.crearNotificacion(1L, "Tu solicitud fue aprobada");

        verify(emailService).enviarNotificacion(
                "atorres.notif@gmail.com", "Tu solicitud fue aprobada",
                "Sistema de Pre-Sustentaciones", "noreply@uteq.edu.ec");
    }

    @Test
    void crearNotificacionNoEnviaEmailSiEmailNotificacionesEstaEnBlanco() {
        Usuario receptor = receptorCon("   ");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(receptor));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        notificacionService.crearNotificacion(1L, "hola");

        verify(emailService, never()).enviarNotificacion(any(), any(), any(), any());
    }

    @Test
    void crearNotificacionUsaNombreYEmailDelUsuarioAutenticadoComoRemitente() {
        Usuario receptor = receptorCon("atorres.notif@gmail.com");
        Usuario coordinador = Usuario.builder().id(2L).nombre("Jorge").apellido("Coordinador")
                .email("jcoordinador@uteq.edu.ec").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(receptor));
        when(usuarioRepository.findByEmail("jcoordinador@uteq.edu.ec")).thenReturn(Optional.of(coordinador));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jcoordinador@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_COORDINADOR")));

        notificacionService.crearNotificacion(1L, "Solicitud aprobada");

        verify(emailService).enviarNotificacion(
                "atorres.notif@gmail.com", "Solicitud aprobada", "Jorge Coordinador", "jcoordinador@uteq.edu.ec");
    }

    @Test
    void marcarComoLeidaLanzaExcepcionSiLaNotificacionNoExiste() {
        when(notificacionRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> notificacionService.marcarComoLeida(5L));
    }

    // Hallazgo real (2026-09-01): NotificacionServiceImpl.validarAcceso(Long) es codigo nuevo
    // (control de acceso real agregado por el equipo) que estas pruebas, escritas antes del
    // cambio, no ejercitaban -- exige SecurityContextHolder autenticado, y si no es ADMIN,
    // busca al usuario actual por email (usuarioRepository.findByEmail) para comparar su ID
    // contra el usuario objetivo. Las pruebas de "delegacion simple" (no prueban autorizacion
    // en si) se autentican como ADMIN para saltarse esa busqueda, igual que ya hacia el patron
    // ADMIN en ActaServiceImplTest.

    @Test
    void marcarComoLeidaActualizaElFlagYGuarda() {
        Usuario propietario = Usuario.builder().id(1L).email("atorres@uteq.edu.ec").build();
        Notificacion n = Notificacion.builder().id(5L).mensaje("x").leida(false).usuario(propietario).build();
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        Notificacion resultado = notificacionService.marcarComoLeida(5L);

        assertTrue(resultado.isLeida());
        verify(notificacionRepository).save(n);
    }

    @Test
    void contarNoLeidasDelegaAlRepositorio() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(1L)).thenReturn(3L);
        assertEquals(3L, notificacionService.contarNoLeidas(1L));
    }

    @Test
    void marcarTodasLeidasDelegaAlRepositorio() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        notificacionService.marcarTodasLeidas(1L);
        verify(notificacionRepository).marcarTodasLeidasPorUsuario(1L);
    }

    @Test
    void eliminarNotificacionExitosoSiEsPropietario() {
        Usuario receptor = Usuario.builder().id(1L).email("atorres@uteq.edu.ec").build();
        Notificacion n = Notificacion.builder().id(5L).usuario(receptor).build();
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));
        when(usuarioRepository.findByEmail("atorres@uteq.edu.ec")).thenReturn(Optional.of(receptor));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("atorres@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE")));

        assertDoesNotThrow(() -> notificacionService.eliminarNotificacion(5L));
        verify(notificacionRepository).delete(n);
    }

    @Test
    void eliminarNotificacionLanzaAccessDeniedSiNoEsPropietario() {
        Usuario receptor = Usuario.builder().id(1L).email("atorres@uteq.edu.ec").build();
        Usuario otro = Usuario.builder().id(2L).email("otro@uteq.edu.ec").build();
        Notificacion n = Notificacion.builder().id(5L).usuario(receptor).build();
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));
        when(usuarioRepository.findByEmail("otro@uteq.edu.ec")).thenReturn(Optional.of(otro));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otro@uteq.edu.ec", null,
                        AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE")));

        assertThrows(AccessDeniedException.class, () -> notificacionService.eliminarNotificacion(5L));
        verify(notificacionRepository, never()).delete(any());
    }
}
