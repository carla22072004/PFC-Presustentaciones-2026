package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.EvaluacionJuradoDTO;
import ec.edu.uteq.presustentaciones.services.EvaluacionJuradoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * EvaluacionJuradoController no tenía ningún test dedicado (0% de cobertura pese a que su
 * servicio ya está probado al 94% tras el bloque de IDOR de 2026-09-04) -- brecha identificada
 * en la auditoría de cobertura. Cubre también, a nivel de controller, que AccessDeniedException
 * se relanza sin caer en el catch(RuntimeException) genérico (regresión del fix de IDOR: antes
 * de esa corrección, el catch(RuntimeException) convertía un 403 real en 400).
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionJuradoControllerTest {

    @Mock private EvaluacionJuradoService service;

    @InjectMocks
    private EvaluacionJuradoController controller;

    @Test
    void guardarDelegaConLosCamposDelMapaYDevuelveElDto() {
        Map<String, Object> body = Map.of(
                "solicitudId", 7, "juradoId", 3, "notaJurado", 8.5, "observaciones", "Buen trabajo");
        EvaluacionJuradoDTO dto = EvaluacionJuradoDTO.builder().id(1L).notaJurado(8.5).build();
        when(service.guardarEvaluacion(7L, 3L, 8.5, "Buen trabajo")).thenReturn(dto);

        ResponseEntity<?> response = controller.guardar(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void guardarPropagaAccessDeniedSinConvertirloEn400() {
        Map<String, Object> body = Map.of("solicitudId", 7, "juradoId", 3, "notaJurado", 8.5);
        when(service.guardarEvaluacion(7L, 3L, 8.5, "")).thenThrow(new AccessDeniedException("sin permiso"));

        assertThrows(AccessDeniedException.class, () -> controller.guardar(body));
    }

    @Test
    void guardarConvierteUnRuntimeExceptionGenericoEn400() {
        Map<String, Object> body = Map.of("solicitudId", 7, "juradoId", 3, "notaJurado", 8.5);
        when(service.guardarEvaluacion(7L, 3L, 8.5, "")).thenThrow(new RuntimeException("Solicitud no encontrada"));

        ResponseEntity<?> response = controller.guardar(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void obtenerDevuelveElDtoDelServicio() {
        EvaluacionJuradoDTO dto = EvaluacionJuradoDTO.builder().id(1L).notaJurado(9.0).build();
        when(service.obtenerEvaluacion(7L, 3L)).thenReturn(dto);

        ResponseEntity<?> response = controller.obtener(7L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void obtenerDevuelve200ConCuerpoNuloSiAunNoHayEvaluacion() {
        when(service.obtenerEvaluacion(7L, 3L)).thenReturn(null);

        ResponseEntity<?> response = controller.obtener(7L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void obtenerTribunalDelegaEnElServicio() {
        EvaluacionJuradoDTO dto = EvaluacionJuradoDTO.builder().id(1L).build();
        when(service.obtenerTribunal(7L)).thenReturn(List.of(dto));

        ResponseEntity<List<EvaluacionJuradoDTO>> response = controller.obtenerTribunal(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
