package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.security.dto.LoginRequest;
import ec.edu.uteq.presustentaciones.security.dto.LoginResponse;
import ec.edu.uteq.presustentaciones.security.jwt.JwtTokenProvider;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión, registro, actualización de tokens y cierre de sesión")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener tokens", description = "Genera el JWT de acceso y el refresh token. Configura cookies HttpOnly + Secure + SameSite=Strict.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Generamos Access Token (JWT de 7 claims) y Refresh Token (UUID en Redis)
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getEmail());

        // Cabeceras para Set-Cookie seguras (Requisito E13 - HttpOnly, Secure, SameSite=Strict)
        // Usamos addHeader en lugar de Cookie de Servlet para tener soporte de SameSite=Strict completo
        response.addHeader(HttpHeaders.SET_COOKIE, 
                String.format("jwtToken=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=86400", token));
        response.addHeader(HttpHeaders.SET_COOKIE, 
                String.format("refreshToken=%s; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Strict; Max-Age=604800", refreshToken));

        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre() + " " + usuario.getApellido())
                .rol(usuario.getRol())
                .emailNotificaciones(usuario.getEmailNotificaciones())
                .build();

        // Creamos una respuesta enriquecida
        Map<String, Object> data = new HashMap<>();
        data.put("auth", loginResponse);
        data.put("refreshToken", refreshToken);

        return ResponseEntity.ok(ResponseWrapper.success(data, "Sesión iniciada correctamente"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar el token de acceso vencido", description = "Recibe el refresh token, lo valida en Redis y rota ambos tokens (Access y Refresh).")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;

        // Extraer refresh token de las cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseWrapper.error("Refresh token no proporcionado"));
        }

        // Buscar el usuario al que corresponde el refresh token
        String email = jwtTokenProvider.getUsernameFromRefreshToken(refreshToken);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseWrapper.error("Refresh token inválido o expirado"));
        }

        // Validar si coincide con el token almacenado en Redis
        if (!jwtTokenProvider.validateRefreshToken(email, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseWrapper.error("Refresh token revocado"));
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Rotación de tokens (Requisito Rotación): eliminar el anterior y generar nuevos
        jwtTokenProvider.deleteRefreshToken(email);
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Actualizar cookies seguras
        response.addHeader(HttpHeaders.SET_COOKIE, 
                String.format("jwtToken=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=86400", newAccessToken));
        response.addHeader(HttpHeaders.SET_COOKIE, 
                String.format("refreshToken=%s; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Strict; Max-Age=604800", newRefreshToken));

        Map<String, Object> data = new HashMap<>();
        data.put("token", newAccessToken);
        data.put("refreshToken", newRefreshToken);

        return ResponseEntity.ok(ResponseWrapper.success(data, "Tokens actualizados correctamente"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión e invalidar tokens", description = "Agrega el token de acceso a la blacklist en Redis y elimina el refresh token.")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;

        // Intentar extraer token del header Authorization
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }

        // Si no está en el header, intentar extraer de las cookies
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            // Blacklist de Access Token (Requisito Blacklist)
            jwtTokenProvider.blacklistToken(token);
            
            try {
                String email = jwtTokenProvider.getUsernameFromToken(token);
                // Eliminar Refresh Token de Redis (Requisito Blacklist)
                jwtTokenProvider.deleteRefreshToken(email);
            } catch (Exception e) {
                log.warn("No se pudo extraer usuario del token para borrar refresh token: {}", e.getMessage());
            }
        }

        // Limpiar Cookies del cliente
        response.addHeader(HttpHeaders.SET_COOKIE, "jwtToken=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0");
        response.addHeader(HttpHeaders.SET_COOKIE, "refreshToken=; Path=/api/auth/refresh; HttpOnly; Secure; SameSite=Strict; Max-Age=0");

        return ResponseEntity.ok(ResponseWrapper.success(null, "Sesión cerrada correctamente"));
    }

    /** Provisión de cuentas: solo un ADMIN puede crear usuarios (incluye poder asignar cualquier rol). */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar nuevo usuario", description = "Permite a un administrador crear nuevos usuarios en el sistema.")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseWrapper.error("El email ya está registrado"));
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(null, "Usuario registrado exitosamente"));
    }
}