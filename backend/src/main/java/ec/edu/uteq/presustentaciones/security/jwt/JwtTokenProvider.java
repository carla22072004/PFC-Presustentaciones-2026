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

    // Generar y almacenar Refresh Token en Redis (Requisito Refresh Token)
    public String generateRefreshToken(String username) {
        String refreshToken = UUID.randomUUID().toString();
        if (redisTemplate != null) {
            String key = "refresh:" + username;
            String reverseKey = "refresh_token:" + refreshToken;
            redisTemplate.opsForValue().set(key, refreshToken, jwtRefreshExpiration, TimeUnit.MILLISECONDS);
            redisTemplate.opsForValue().set(reverseKey, username, jwtRefreshExpiration, TimeUnit.MILLISECONDS);
            log.info("Refresh token generado y guardado en Redis para el usuario: {}", username);
        } else {
            log.warn("StringRedisTemplate no está disponible. Refresh token no guardado.");
        }
        return refreshToken;
    }

    public String getUsernameFromRefreshToken(String token) {
        if (redisTemplate == null) {
            return null;
        }
        String reverseKey = "refresh_token:" + token;
        return redisTemplate.opsForValue().get(reverseKey);
    }

    public boolean validateRefreshToken(String username, String token) {
        if (redisTemplate == null) {
            return false;
        }
        String key = "refresh:" + username;
        String storedToken = redisTemplate.opsForValue().get(key);
        return storedToken != null && storedToken.equals(token);
    }

    public void deleteRefreshToken(String username) {
        if (redisTemplate == null) {
            return;
        }
        String key = "refresh:" + username;
        String token = redisTemplate.opsForValue().get(key);
        if (token != null) {
            redisTemplate.delete(key);
            redisTemplate.delete("refresh_token:" + token);
        }
        log.info("Refresh token eliminado de Redis para el usuario: {}", username);
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
        try {
            if (isTokenBlacklisted(token)) {
                log.warn("Token JWT rechazado: se encuentra en la blacklist.");
                return false;
            }
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Error validando token JWT: {}", e.getMessage());
            return false;
        }
    }
}
