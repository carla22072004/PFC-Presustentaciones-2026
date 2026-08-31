package ec.edu.uteq.presustentaciones.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpiration; // 7 días por defecto

    @Autowired(required = false) // Hacemos opcional para tests unitarios simples
    private StringRedisTemplate redisTemplate;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userDetails.getUsername());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti
                .issuer("PFC-Presustentaciones-UTEQ") // iss
                .subject(username) // sub
                .audience().add("PFC-Frontend-Angular").and() // aud
                .issuedAt(now) // iat
                .notBefore(now) // nbf
                .expiration(expiryDate) // exp
                .signWith(getSigningKey())
                .compact();
    }

    // Generar y almacenar Refresh Token en Redis (Multi-device support)
    public String generateRefreshToken(String username) {
        String refreshToken = UUID.randomUUID().toString();
        if (redisTemplate != null) {
            String tokenKey = "refresh_token:" + refreshToken;
            String userSetKey = "user_refresh_tokens:" + username;
            
            // Guardar token -> username
            redisTemplate.opsForValue().set(tokenKey, username, jwtRefreshExpiration, TimeUnit.MILLISECONDS);
            // Agregar a la lista de tokens activos del usuario
            redisTemplate.opsForSet().add(userSetKey, refreshToken);
            redisTemplate.expire(userSetKey, jwtRefreshExpiration, TimeUnit.MILLISECONDS);
            
            log.info("Refresh token generado y guardado en Redis para el usuario: {}", username);
        }
        return refreshToken;
    }

    public String getUsernameFromRefreshToken(String token) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForValue().get("refresh_token:" + token);
    }
    
    public String getUsernameFromUsedRefreshToken(String token) {
        if (redisTemplate == null) return null;
        return redisTemplate.opsForValue().get("used_refresh_token:" + token);
    }

    public boolean validateRefreshToken(String token) {
        if (redisTemplate == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey("refresh_token:" + token));
    }

    public void rotateRefreshToken(String oldToken, String username) {
        if (redisTemplate == null) return;
        
        // Mover a "usados" para detectar reutilización
        redisTemplate.delete("refresh_token:" + oldToken);
        redisTemplate.opsForValue().set("used_refresh_token:" + oldToken, username, jwtRefreshExpiration, TimeUnit.MILLISECONDS);
        
        // Quitar de la lista de activos
        redisTemplate.opsForSet().remove("user_refresh_tokens:" + username, oldToken);
    }
    
    public void revokeAllUserTokens(String username) {
        if (redisTemplate == null) return;
        String userSetKey = "user_refresh_tokens:" + username;
        java.util.Set<String> activeTokens = redisTemplate.opsForSet().members(userSetKey);
        if (activeTokens != null) {
            for (String t : activeTokens) {
                redisTemplate.delete("refresh_token:" + t);
            }
        }
        redisTemplate.delete(userSetKey);
        log.warn("Todos los refresh tokens han sido revocados para el usuario: {}", username);
    }

    public void deleteRefreshToken(String token) {
        if (redisTemplate == null) return;
        String username = getUsernameFromRefreshToken(token);
        if (username != null) {
            redisTemplate.delete("refresh_token:" + token);
            redisTemplate.opsForSet().remove("user_refresh_tokens:" + username, token);
        }
    }

    // Invalidar token JWT (Blacklist en Redis - Requisito Blacklist)
    public void blacklistToken(String token) {
        if (redisTemplate == null) {
            log.warn("StringRedisTemplate no está disponible. Blacklist omitida.");
            return;
        }
        try {
            Claims claims = getClaimsFromToken(token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();

            if (remainingTime > 0) {
                String key = "blacklist:token:" + jti;
                redisTemplate.opsForValue().set(key, "revoked", remainingTime, TimeUnit.MILLISECONDS);
                log.info("Token JWT blacklisted (JTI: {}) por los siguientes {} ms", jti, remainingTime);
            }
        } catch (Exception e) {
            log.error("No se pudo agregar el token a la blacklist: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Claims claims = getClaimsFromToken(token);
            String jti = claims.getId();
            if (jti == null) {
                return false;
            }
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:token:" + jti));
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) {
            log.warn("Token JWT rechazado: se encuentra en la blacklist.");
            throw new io.jsonwebtoken.JwtException("Token en blacklist");
        }
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
        return true;
    }
}
