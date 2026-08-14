package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.UniversityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalApiServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ExternalApiServiceImpl externalApiService;

    @BeforeEach
    void setUp() {
        // Reemplazar el WebClient real con el mock inyectado
        ReflectionTestUtils.setField(externalApiService, "webClient", webClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUniversitiesOfEcuadorSuccess() {
        UniversityDto mockUniversity = UniversityDto.builder()
                .name("Universidad Técnica Estatal de Quevedo")
                .country("Ecuador")
                .domains(Collections.singletonList("uteq.edu.ec"))
                .webPages(Collections.singletonList("https://www.uteq.edu.ec"))
                .build();

        // Mapear los métodos encadenados de WebClient
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(UniversityDto.class)).thenReturn(Flux.just(mockUniversity));

        List<UniversityDto> result = externalApiService.getUniversitiesOfEcuador();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Universidad Técnica Estatal de Quevedo", result.get(0).getName());
        assertEquals("Ecuador", result.get(0).getCountry());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetUniversitiesOfEcuadorFallback() {
        // Simulamos una excepción para gatillar el fallback (onErrorResume)
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(UniversityDto.class)).thenReturn(Flux.error(new RuntimeException("Simulated API failure")));

        List<UniversityDto> result = externalApiService.getUniversitiesOfEcuador();

        assertNotNull(result);
        assertEquals(1, result.size());
        // El nombre debe contener la leyenda "Fallback Local"
        assertTrue(result.get(0).getName().contains("Fallback Local"));
        assertEquals("Ecuador", result.get(0).getCountry());
    }
}
