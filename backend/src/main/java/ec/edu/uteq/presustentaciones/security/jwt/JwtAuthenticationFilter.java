package ec.edu.uteq.presustentaciones.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            boolean desdeHeader = StringUtils.hasText(request.getHeader("Authorization"));

            if (StringUtils.hasText(jwt)) {
                try {
                    if (jwtTokenProvider.validateToken(jwt)) {
                        String username = jwtTokenProvider.getUsernameFromToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                    log.warn("Token expirado: {}", ex.getMessage());
                    // Un token caducado NUNCA debe cortar la petición en endpoints públicos
                    // (/auth/login, /auth/refresh) ni cuando llega solo por la cookie HttpOnly
                    // ambiental: hacerlo dejaba al usuario sin poder ni siquiera reautenticarse
                    // (la cookie jwtToken vieja bloqueaba el propio login). Se limpia la cookie
                    // caducada y se deja seguir la cadena sin autenticación -- si el endpoint es
                    // protegido, la capa de autorización responderá 401 por el entry point.
                    limpiarCookieJwt(response);
                    if (desdeHeader && !esRutaPublicaDeAuth(request)) {
                        sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
                        return;
                    }
                } catch (Exception ex) {
                    log.error("Token inválido: {}", ex.getMessage());
                    limpiarCookieJwt(response);
                    if (desdeHeader && !esRutaPublicaDeAuth(request)) {
                        sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("No se pudo establecer la autenticación: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean esRutaPublicaDeAuth(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.contains("/auth/login")
                || uri.contains("/auth/refresh")
                || uri.contains("/auth/logout")
                || uri.contains("/auth/register"));
    }

    /** Borra la cookie jwtToken caducada/ inválida del navegador para que no vuelva a estorbar. */
    private void limpiarCookieJwt(HttpServletResponse response) {
        jakarta.servlet.http.Cookie c = new jakarta.servlet.http.Cookie("jwtToken", "");
        c.setPath("/");
        c.setMaxAge(0);
        c.setHttpOnly(true);
        response.addCookie(c);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Soporte para Cookie de seguridad HTTP-Only
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"success\": false, \"message\": \"%s\"}", message));
    }
}
