package ec.edu.uteq.presustentaciones.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String ipAddress) {
        String key = "ratelimit:login:" + ipAddress;
        String currentVal = redisTemplate.opsForValue().get(key);
        
        if (currentVal == null) {
            // Primer intento, inicializa con expiración de 60 segundos
            redisTemplate.opsForValue().set(key, "1", 60, TimeUnit.SECONDS);
            return true;
        }
        
        int attempts = Integer.parseInt(currentVal);
        if (attempts >= 6) {
            return false; // Bloquea si ya superó los 6 intentos por minuto
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
