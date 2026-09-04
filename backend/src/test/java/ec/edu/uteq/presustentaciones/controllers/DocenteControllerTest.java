package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.config.SecurityConfig;
import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.security.RateLimiterService;
import ec.edu.uteq.presustentaciones.security.jwt.JwtTokenProvider;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Corrección de IDOR (auditoría 2026-09-04): GET /api/docentes/usuario/{usuarioId} exponía el
 * perfil de CUALQUIER docente a cualquier usuario autenticado, sin verificar que el usuarioId
 * consultado correspondiera al propio usuario (el frontend siempre pasa
 * authService.getUserId(), nunca un id ajeno -- ver firmar-acta-docente.component.ts y
 * mis-asignaciones.component.ts). Usa el mismo patrón @WebMvcTest + SecurityConfig real de
 * AuthControllerIntegrationTest para probar los códigos HTTP reales (401/403/200), no solo la
 * lógica de negocio mockeada.
 */
@WebMvcTest(controllers = DocenteController.class)
@Import(SecurityConfig.class)
class DocenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocenteRepository docenteRepository;

    @MockBean
    private UsuarioActualService usuarioActualService;

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

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository rolUsuarioRepository;

    @MockBean
    private ec.edu.uteq.presustentaciones.repositories.UsuarioRepository usuarioRepository;

    private Usuario usuarioDocente;
    private Docente docente;

    @BeforeEach
    void setUp() {
        usuarioDocente = new Usuario();
        usuarioDocente.setId(50L);
        usuarioDocente.setEmail("docente@uteq.edu.ec");
        usuarioDocente.setNombre("Ana");
        usuarioDocente.setApellido("Torres");

        docente = Docente.builder().id(7L).usuario(usuarioDocente).build();

        when(docenteRepository.findByUsuarioId(50L)).thenReturn(Optional.of(docente));
        when(docenteRepository.findByUsuarioId(99L)).thenReturn(Optional.of(
                Docente.builder().id(8L).usuario(new Usuario()).build()));
    }

    private void autenticarComo(String email, String rol) throws Exception {
        String token = "token-" + email;
        UserDetails userDetails = new User(email, "x",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol)));
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
    }

    @Test
    void obtenerPorUsuarioSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/usuario/50").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerPorUsuarioPermiteConsultarElPropioPerfil() throws Exception {
        autenticarComo("docente@uteq.edu.ec", "DOCENTE");
        when(usuarioActualService.usuario()).thenReturn(usuarioDocente);

        mockMvc.perform(get("/api/v1/docentes/usuario/50")
                        .header("Authorization", "Bearer token-docente@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorUsuarioRechazaConsultaDeOtroDocente() throws Exception {
        // Caso IDOR: un docente autenticado (id 50) intenta ver el perfil del docente 99
        // cambiando el usuarioId en la URL.
        autenticarComo("docente@uteq.edu.ec", "DOCENTE");
        when(usuarioActualService.usuario()).thenReturn(usuarioDocente);

        mockMvc.perform(get("/api/v1/docentes/usuario/99")
                        .header("Authorization", "Bearer token-docente@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerPorUsuarioPermiteAAdminConsultarCualquierUsuario() throws Exception {
        autenticarComo("admin@uteq.edu.ec", "ADMIN");

        mockMvc.perform(get("/api/v1/docentes/usuario/99")
                        .header("Authorization", "Bearer token-admin@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorUsuarioPermiteACoordinadorConsultarCualquierUsuario() throws Exception {
        autenticarComo("coord@uteq.edu.ec", "COORDINADOR");

        mockMvc.perform(get("/api/v1/docentes/usuario/99")
                        .header("Authorization", "Bearer token-coord@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void listarSigueFuncionandoParaCualquierAutenticado() throws Exception {
        // No debe cambiar: el directorio completo (usado para seleccionar jurado/tutor) sigue
        // abierto a cualquier autenticado, sin control de propiedad -- no es el mismo caso.
        autenticarComo("docente@uteq.edu.ec", "DOCENTE");
        when(docenteRepository.findAll()).thenReturn(List.of(docente));

        mockMvc.perform(get("/api/v1/docentes")
                        .header("Authorization", "Bearer token-docente@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorIdSigueFuncionandoSinControlDePropiedad() throws Exception {
        autenticarComo("docente@uteq.edu.ec", "DOCENTE");
        when(docenteRepository.findById(7L)).thenReturn(Optional.of(docente));

        mockMvc.perform(get("/api/v1/docentes/7")
                        .header("Authorization", "Bearer token-docente@uteq.edu.ec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
