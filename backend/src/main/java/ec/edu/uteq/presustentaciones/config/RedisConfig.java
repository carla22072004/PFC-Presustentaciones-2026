package ec.edu.uteq.presustentaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // El ObjectMapper por defecto de GenericJackson2JsonRedisSerializer no trae
        // registrado el módulo de fechas de Java 8 (JavaTimeModule): cualquier valor
        // cacheado que contenga un LocalDateTime (prácticamente todas las entidades:
        // Solicitud, Usuario, Estudiante, ...) fallaba al serializarse hacia Redis con
        // InvalidDefinitionException, y esa excepción se propagaba fuera del método
        // @Cacheable. Bug real detectado: /api/v1/solicitudes/mis-solicitudes devolvía
        // silenciosamente una lista vacía (el controlador atajaba la excepción) aunque
        // el estudiante sí tenía solicitudes registradas en la base de datos.
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Configuración por defecto: 5 minutos
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // Configuración personalizada de TTL por cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // universidades: TTL de 10 minutos (Requisito E2)
        cacheConfigurations.put("universidades", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // solicitudes: TTL de 5 minutos
        cacheConfigurations.put("solicitudes", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
