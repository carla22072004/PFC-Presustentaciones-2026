package ec.edu.uteq.presustentaciones;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.security.RateLimiterService;
import ec.edu.uteq.presustentaciones.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class Phase4FeaturesTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimiterService rateLimiterService;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimiterService = new RateLimiterService(redisTemplate);

        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpiration", 604800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "redisTemplate", redisTemplate);
    }

    @Test
    void testResponseWrapperSuccess() {
        ResponseWrapper<String> wrapper = ResponseWrapper.success("Hello", "Success Message");
        assertTrue(wrapper.isSuccess());
        assertEquals("Hello", wrapper.getData());
        assertEquals("Success Message", wrapper.getMessage());
        assertNull(wrapper.getErrors());
    }

    @Test
    void testResponseWrapperError() {
        ResponseWrapper<Void> wrapper = ResponseWrapper.error("Error Message", "Details");
        assertFalse(wrapper.isSuccess());
        assertNull(wrapper.getData());
        assertEquals("Error Message", wrapper.getMessage());
        assertEquals("Details", wrapper.getErrors());
    }

    @Test
    void testRateLimiterAllowed() {
        when(valueOperations.get(anyString())).thenReturn(null);
        assertTrue(rateLimiterService.isAllowed("127.0.0.1"));
    }

    @Test
    void testRateLimiterBlockedAfterMaxAttempts() {
        when(valueOperations.get(anyString())).thenReturn("6");
        assertFalse(rateLimiterService.isAllowed("127.0.0.1"));
    }

    @Test
    void testJwtTokenProviderGenerateAndBlacklist() {
        String token = jwtTokenProvider.generateTokenFromUsername("admin@uteq.edu.ec");
        assertNotNull(token);

        // Mock para simular que el token ha sido añadido a la blacklist
        String jti = jwtTokenProvider.getClaimsFromToken(token).getId();
        String blacklistKey = "blacklist:token:" + jti;
        when(redisTemplate.hasKey(blacklistKey)).thenReturn(true);

        assertTrue(jwtTokenProvider.isTokenBlacklisted(token));
        assertFalse(jwtTokenProvider.validateToken(token)); // Debe ser inválido porque está en blacklist
    }

    @Test
    void testRefreshTokens() {
        String username = "test@uteq.edu.ec";
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        assertNotNull(refreshToken);

        String reverseKey = "refresh_token:" + refreshToken;
        when(redisTemplate.opsForValue().get(reverseKey)).thenReturn(username);

        assertEquals(username, jwtTokenProvider.getUsernameFromRefreshToken(refreshToken));

        String key = "refresh:" + username;
        when(redisTemplate.opsForValue().get(key)).thenReturn(refreshToken);
        boolean isValid = jwtTokenProvider.validateRefreshToken(refreshToken);
        assertTrue(isValid);
    }
}
