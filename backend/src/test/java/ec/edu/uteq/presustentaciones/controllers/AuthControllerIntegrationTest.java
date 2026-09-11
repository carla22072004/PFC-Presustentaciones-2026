package ec.edu.uteq.presustentaciones.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.presustentaciones.config.SecurityConfig;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.security.PasswordPolicyValidator;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ec.edu.uteq.presustentaciones.security.PasswordRecoveryService passwordRecoveryService;

    @MockBean
    private ec.edu.uteq.presustentaciones.services.SupresionDatosService supresionDatosService;

    @MockBean
    private IUsuarioService usuarioService;

    // Mock, no la implementacion real: un mock sin stub no hace nada en un metodo void
    // (validar()), asi que los tests existentes de /register siguen pasando sin tocar sus
    // fixtures de password. La politica real (longitud, lista de comunes) se prueba en
    // PasswordPolicyValidatorTest, incluido su caso de integracion contra este mismo endpoint.
    @MockBean
    private PasswordPolicyValidator passwordPolicyValidator;

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

    // ── /register: endurecido contra mass-assignment (auditoría 2026-09-04) ────────────────

    /** Autentica como un ADMIN con USUARIOS_GESTIONAR, igual que testAccesoProtegidoConTokenValido. */
    private void autenticarComoAdminConGestionUsuarios() {
        String token = "adminToken";
        UserDetails adminDetails = new User(dummyUsuario.getEmail(), dummyUsuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(dummyUsuario.getEmail());
        when(userDetailsService.loadUserByUsername(dummyUsuario.getEmail())).thenReturn(adminDetails);
        when(permisoService.tienePermiso(any(), any())).thenReturn(true);
    }

    @Test
    void testRegisterExitosoDelegaEnUsuarioServiceConLosCincoCamposPermitidos() throws Exception {
        autenticarComoAdminConGestionUsuarios();
        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"ESTUDIANTE\"}";

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        when(usuarioService.crear(captor.capture())).thenReturn(new Usuario());

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Usuario creado = captor.getValue();
        assertEquals("Carlos", creado.getNombre());
        assertEquals("Mendoza", creado.getApellido());
        assertEquals("cmendoza@uteq.edu.ec", creado.getEmail());
        assertEquals("password123", creado.getPassword());
        assertEquals("ESTUDIANTE", creado.getRol());
        assertEquals(Boolean.TRUE, creado.getActivo());
    }

    @Test
    void testRegisterIgnoraCamposSensiblesEnviadosPorElCliente() throws Exception {
        // Mass-assignment: id, activo=false y rolUsuario no existen en RegisterRequest, así que
        // Jackson los descarta al deserializar -- nunca llegan a la entidad Usuario real.
        autenticarComoAdminConGestionUsuarios();
        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"ESTUDIANTE\"," +
                "\"id\":999,\"activo\":false,\"rolUsuario\":{\"id\":1},\"creadoEn\":\"2020-01-01T00:00:00\"}";

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        when(usuarioService.crear(captor.capture())).thenReturn(new Usuario());

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        Usuario creado = captor.getValue();
        assertNull(creado.getId());
        assertEquals(Boolean.TRUE, creado.getActivo(), "activo debe quedar forzado a true pese al false enviado por el cliente");
    }

    @Test
    void testRegisterDatosInvalidosDevuelve400SinLlamarAUsuarioService() throws Exception {
        autenticarComoAdminConGestionUsuarios();
        // Falta "nombre" (obligatorio) y password muy corta.
        String body = "{\"apellido\":\"Mendoza\",\"email\":\"no-es-un-email\",\"password\":\"123\",\"rol\":\"ESTUDIANTE\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).crear(any());
    }

    @Test
    void testRegisterEmailDuplicadoDevuelve400() throws Exception {
        autenticarComoAdminConGestionUsuarios();
        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"ESTUDIANTE\"}";
        when(usuarioService.crear(any(Usuario.class)))
                .thenThrow(new IllegalArgumentException("Ya existe un usuario con el email: cmendoza@uteq.edu.ec"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un usuario con el email: cmendoza@uteq.edu.ec"));
    }

    @Test
    void testRegisterRolInvalidoDevuelve400() throws Exception {
        autenticarComoAdminConGestionUsuarios();
        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"SUPERUSUARIO_INVENTADO\"}";
        when(usuarioService.crear(any(Usuario.class)))
                .thenThrow(new IllegalArgumentException("Rol inválido o no existe: SUPERUSUARIO_INVENTADO"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegisterSinPermisoUsuariosGestionarDevuelve403() throws Exception {
        String token = "docenteToken";
        UserDetails docenteDetails = new User("docente@uteq.edu.ec", "x",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCENTE")));
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("docente@uteq.edu.ec");
        when(userDetailsService.loadUserByUsername("docente@uteq.edu.ec")).thenReturn(docenteDetails);
        when(permisoService.tienePermiso(any(), any())).thenReturn(false);

        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"ESTUDIANTE\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("Authorization", "Bearer docenteToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).crear(any());
    }

    // ── PATCH /api/v1/auth/usuarios/{id}/password (RF-06: cambio de contraseña propia) ─────

    private void autenticarComoTitular() {
        String token = "selfToken";
        UserDetails userDetails = new User(dummyUsuario.getEmail(), dummyUsuario.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(dummyUsuario.getEmail());
        when(userDetailsService.loadUserByUsername(dummyUsuario.getEmail())).thenReturn(userDetails);
        when(usuarioRepository.findByEmail(dummyUsuario.getEmail())).thenReturn(Optional.of(dummyUsuario));
    }

    @Test
    void cambiarPasswordConLaActualCorrectaDevuelve200YRevocaSesionesSalvoLaActual() throws Exception {
        autenticarComoTitular();
        when(passwordEncoder.matches("actualCorrecta", dummyUsuario.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave#2026")).thenReturn("hashNuevo");

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", "refresh-de-esta-sesion");

        mockMvc.perform(patch("/api/v1/auth/usuarios/1/password")
                        .header("Authorization", "Bearer selfToken")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"actualCorrecta\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(passwordPolicyValidator).validar("NuevaClave#2026");
        verify(usuarioRepository).save(dummyUsuario);
        assertEquals("hashNuevo", dummyUsuario.getPassword());
        verify(jwtTokenProvider).revokeAllUserTokensExcept(dummyUsuario.getEmail(), "refresh-de-esta-sesion");
    }

    @Test
    void cambiarPasswordConLaActualIncorrectaDevuelve401YNoCambiaNada() throws Exception {
        autenticarComoTitular();
        when(passwordEncoder.matches("incorrecta", dummyUsuario.getPassword())).thenReturn(false);

        mockMvc.perform(patch("/api/v1/auth/usuarios/1/password")
                        .header("Authorization", "Bearer selfToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"incorrecta\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(usuarioRepository, never()).save(any());
        verify(jwtTokenProvider, never()).revokeAllUserTokensExcept(any(), any());
    }

    @Test
    void cambiarPasswordIgualALaVigenteDevuelve400() throws Exception {
        autenticarComoTitular();
        when(passwordEncoder.matches("igual", dummyUsuario.getPassword())).thenReturn(true);

        mockMvc.perform(patch("/api/v1/auth/usuarios/1/password")
                        .header("Authorization", "Bearer selfToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"igual\",\"passwordNueva\":\"igual\"}"))
                .andExpect(status().isBadRequest());

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPasswordQueIncumpleLaPoliticaDevuelve400() throws Exception {
        autenticarComoTitular();
        when(passwordEncoder.matches("actualCorrecta", dummyUsuario.getPassword())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Esa contraseña es demasiado común. Elige una diferente."))
                .when(passwordPolicyValidator).validar("comun123");

        mockMvc.perform(patch("/api/v1/auth/usuarios/1/password")
                        .header("Authorization", "Bearer selfToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"actualCorrecta\",\"passwordNueva\":\"comun123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Esa contraseña es demasiado común. Elige una diferente."));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarPasswordDeOtroUsuarioDevuelve403AunqueQuienLoIntentaSeaAdmin() throws Exception {
        // dummyUsuario (id=1, ROLE_ADMIN) intenta cambiar la contraseña del usuario id=2.
        autenticarComoTitular();

        mockMvc.perform(patch("/api/v1/auth/usuarios/2/password")
                        .header("Authorization", "Bearer selfToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"cualquiera\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No puedes cambiar la contraseña de otro usuario"));

        verify(usuarioRepository, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
    }

    @Test
    void cambiarPasswordNuncaDevuelveLaContrasenaEnLaRespuesta() throws Exception {
        autenticarComoTitular();
        when(passwordEncoder.matches("actualCorrecta", dummyUsuario.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("hashNuevo");

        String cuerpo = mockMvc.perform(patch("/api/v1/auth/usuarios/1/password")
                        .header("Authorization", "Bearer selfToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"actualCorrecta\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(cuerpo.contains("NuevaClave#2026"));
        assertFalse(cuerpo.contains("hashNuevo"));
        assertFalse(cuerpo.toLowerCase().contains("password"));
    }

    // ── POST /api/v1/auth/recuperar y /restablecer (RF-05: recuperación sin sesión) ────────

    @Test
    void recuperarDevuelveElMismoCuerpoYCodigoExistaONoLaCuenta() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);

        String cuerpoExiste = mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"estudiante@uteq.edu.ec\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String cuerpoNoExiste = mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie-registrado@uteq.edu.ec\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(cuerpoExiste, cuerpoNoExiste, "el cuerpo debe ser identico para no permitir enumerar cuentas");
        verify(passwordRecoveryService, times(2)).solicitarRecuperacion(anyString());
    }

    @Test
    void recuperarSuperandoElLimiteDeTasaDevuelve429() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"estudiante@uteq.edu.ec\"}"))
                .andExpect(status().isTooManyRequests());

        verify(passwordRecoveryService, never()).solicitarRecuperacion(anyString());
    }

    @Test
    void recuperarConElLimitadorDeTasaCaidoDevuelve503() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyLong()))
                .thenThrow(new ec.edu.uteq.presustentaciones.security.RateLimiterUnavailableException("caido", new RuntimeException()));

        mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"estudiante@uteq.edu.ec\"}"))
                .andExpect(status().isServiceUnavailable());

        verify(passwordRecoveryService, never()).solicitarRecuperacion(anyString());
    }

    @Test
    void restablecerConTokenValidoDevuelve200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"un-token-cualquiera\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(passwordRecoveryService).restablecer("un-token-cualquiera", "NuevaClave#2026");
    }

    @Test
    void restablecerConTokenInvalidoOExpiradoDevuelve400() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("El enlace de recuperación es inválido o ya expiró."))
                .when(passwordRecoveryService).restablecer("token-vencido", "NuevaClave#2026");

        mockMvc.perform(post("/api/v1/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-vencido\",\"passwordNueva\":\"NuevaClave#2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace de recuperación es inválido o ya expiró."));
    }

    // ── POST /api/v1/usuarios (UsuarioController.crear): mismo hallazgo que /register ──────

    @Test
    void testCrearUsuarioIgnoraIdDelClienteEnviadoEnElBody() throws Exception {
        // Mismo patrón que testRegisterIgnoraCamposSensiblesEnviadosPorElCliente: "id" no existe
        // en RegisterRequest, así que Jackson lo descarta -- nunca llega a la entidad Usuario.
        autenticarComoAdminConGestionUsuarios();
        String body = "{\"nombre\":\"Carlos\",\"apellido\":\"Mendoza\",\"email\":\"cmendoza2@uteq.edu.ec\"," +
                "\"password\":\"password123\",\"rol\":\"ESTUDIANTE\",\"id\":1,\"activo\":false}";

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        when(usuarioService.crear(captor.capture())).thenReturn(new Usuario());

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        assertNull(captor.getValue().getId());
        assertEquals(Boolean.TRUE, captor.getValue().getActivo());
    }
}
