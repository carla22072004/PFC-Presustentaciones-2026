package ec.edu.uteq.presustentaciones.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.presustentaciones.config.SecurityConfig;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.security.RateLimiterService;
import ec.edu.uteq.presustentaciones.security.dto.LoginRequest;
import ec.edu.uteq.presustentaciones.security.jwt.JwtAuthenticationFilter;
import ec.edu.uteq.presustentaciones.security.jwt.JwtTokenProvider;
import ec.edu.uteq.presustentaciones.services.IUsuarioService;
import ec.edu.uteq.presustentaciones.services.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, UsuarioController.class})
@Import({SecurityConfig.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private IUsuarioService usuarioService;

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository rolUsuarioRepository;

    @MockBean(name = "permisoService")
    private PermisoService permisoService;

    private Usuario dummyUsuario;

    @BeforeEach
    void setUp() {
        dummyUsuario = new Usuario();
        dummyUsuario.setId(1L);
        dummyUsuario.setEmail("test@uteq.edu.ec");
        dummyUsuario.setPassword("encodedPassword");
        dummyUsuario.setNombre("Carla");
        dummyUsuario.setApellido("Perez");
        dummyUsuario.setRol("ADMIN");
        dummyUsuario.setActivo(true);

        // Permitir peticiones en rate limiter
        when(rateLimiterService.isAllowed(any())).thenReturn(true);
    }

    @Test
    void testLoginExitoso() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@uteq.edu.ec");
        loginRequest.setPassword("correctPassword");

        Authentication dummyAuth = Mockito.mock(Authentication.class);
        UserDetails userDetails = new User(dummyUsuario.getEmail(), dummyUsuario.getPassword(), 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(dummyAuth.getPrincipal()).thenReturn(userDetails);

        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(dummyUsuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(dummyAuth);
        when(jwtTokenProvider.generateToken(dummyAuth)).thenReturn("dummyAccessToken");
        when(jwtTokenProvider.generateRefreshToken(dummyUsuario.getEmail())).thenReturn("dummyRefreshToken");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sesión iniciada correctamente"))
                .andExpect(jsonPath("$.data.refreshToken").value("dummyRefreshToken"));
    }

    @Test
    void testLoginContrasenaIncorrecta() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@uteq.edu.ec");
        loginRequest.setPassword("wrongPassword");

        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(dummyUsuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void testAccesoProtegidoSinToken() throws Exception {
        // 401, no 403: ver fix(seguridad) "responder 401 en vez de 403 cuando la sesion expira"
        // (SecurityConfig.authenticationEntryPoint / GlobalExceptionHandler).
        mockMvc.perform(get("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccesoProtegidoConTokenValido() throws Exception {
        String token = "validToken";
        UserDetails userDetails = new User(dummyUsuario.getEmail(), dummyUsuario.getPassword(), 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(dummyUsuario.getEmail());
        when(userDetailsService.loadUserByUsername(dummyUsuario.getEmail())).thenReturn(userDetails);
        when(usuarioService.listarTodos()).thenReturn(Collections.emptyList());
        when(permisoService.tienePermiso(any(), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginUsuarioInexistente() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("noexiste@uteq.edu.ec");
        loginRequest.setPassword("password");

        when(usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                // Si el Controller arroja RuntimeException("Usuario no encontrado") es atrapado por el ExceptionHandler.
                // Si asume BadCredentials o algo, verificamos el código. 
                // Segun AuthController linea 48 lanza RuntimeException. El GlobalExceptionHandler (si existe) lo puede manejar a 400 o 500, pero al ser login y fallar, el handler de Spring Security lo ataja o el GlobalExceptionHandler.
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRefreshConTokenValido() throws Exception {
        String refreshToken = "validRefresh";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);

        when(jwtTokenProvider.getUsernameFromUsedRefreshToken(refreshToken)).thenReturn(null); // No reutilizado
        when(jwtTokenProvider.getUsernameFromRefreshToken(refreshToken)).thenReturn(dummyUsuario.getEmail());
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(usuarioRepository.findByEmail(dummyUsuario.getEmail())).thenReturn(Optional.of(dummyUsuario));
        
        when(jwtTokenProvider.generateTokenFromUsername(dummyUsuario.getEmail())).thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(dummyUsuario.getEmail())).thenReturn(newRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(newAccessToken))
                .andExpect(jsonPath("$.data.refreshToken").value(newRefreshToken));
    }

    @Test
    void testRefreshConTokenReutilizado() throws Exception {
        String refreshToken = "stolenRefresh";
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);

        when(jwtTokenProvider.getUsernameFromUsedRefreshToken(refreshToken)).thenReturn(dummyUsuario.getEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token de seguridad comprometido. Todas las sesiones han sido cerradas."));
    }

    @Test
    void testRefreshConTokenInvalido() throws Exception {
        String refreshToken = "invalidRefresh";
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);

        when(jwtTokenProvider.getUsernameFromUsedRefreshToken(refreshToken)).thenReturn(null);
        when(jwtTokenProvider.getUsernameFromRefreshToken(refreshToken)).thenReturn(dummyUsuario.getEmail());
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(false); // token invalido o expirado

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token inválido o expirado"));
    }

    @Test
    void testLogout() throws Exception {
        String accessToken = "validAccessToken";
        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("jwtToken", accessToken);
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", "refreshT");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(accessCookie, refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sesión cerrada correctamente"));
    }
}
