package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.config.SecurityConfig;
import ec.edu.uteq.presustentaciones.security.RateLimiterService;
import ec.edu.uteq.presustentaciones.security.jwt.JwtTokenProvider;
import ec.edu.uteq.presustentaciones.services.ActaService;
import ec.edu.uteq.presustentaciones.services.PermisoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Corrección de autorización (auditoría 2026-09-04): DELETE /api/v1/actas/{id} solo exigía
 * isAuthenticated(), así que ActaServiceImpl.eliminarActa() -- que reutiliza validarAcceso(),
 * pensado para LECTURA (admin, jurado, tutor o el propio estudiante dueño) -- terminaba
 * autorizando también el borrado permanente del acta a cualquiera de esos participantes. Ahora
 * exige el permiso ACTAS_GESTIONAR (hoy solo ADMIN), igual que el resto de acciones
 * administrativas del controlador (generar/firmar/cambiarEstado).
 */
@WebMvcTest(controllers = ActaController.class)
@Import(SecurityConfig.class)
class ActaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActaService actaService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean(name = "permisoService")
    private PermisoService permisoService;

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository rolUsuarioRepository;

    @MockBean
    private ec.edu.uteq.presustentaciones.repositories.UsuarioRepository usuarioRepository;

    private void autenticarComo(String email, String rol, boolean tienePermiso) {
        String token = "token-" + email;
        UserDetails userDetails = new User(email, "x",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol)));
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(permisoService.tienePermiso(any(), any())).thenReturn(tienePermiso);
    }

    @Test
    void eliminarRechazaAEstudianteAunqueSeaDuenoDeLaSolicitud() throws Exception {
        // Caso que exponía la vulnerabilidad: antes, ser el estudiante dueño/jurado/tutor
        // (via validarAcceso en el service) bastaba para borrar el acta. Ahora ni siquiera
        // llega al service: @PreAuthorize lo rechaza antes.
        autenticarComo("estudiante@uteq.edu.ec", "ESTUDIANTE", false);

        mockMvc.perform(delete("/api/v1/actas/1")
                        .header("Authorization", "Bearer token-estudiante@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(actaService, never()).eliminarActa(any());
    }

    @Test
    void eliminarRechazaADocenteJuradoOTutorSinActasGestionar() throws Exception {
        autenticarComo("docente@uteq.edu.ec", "DOCENTE", false);

        mockMvc.perform(delete("/api/v1/actas/1")
                        .header("Authorization", "Bearer token-docente@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(actaService, never()).eliminarActa(any());
    }

    @Test
    void eliminarPermiteAAdminConActasGestionar() throws Exception {
        autenticarComo("admin@uteq.edu.ec", "ADMIN", true);

        mockMvc.perform(delete("/api/v1/actas/1")
                        .header("Authorization", "Bearer token-admin@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(actaService).eliminarActa(1L);
    }

    @Test
    void eliminarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/v1/actas/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
