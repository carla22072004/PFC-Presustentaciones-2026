package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ActualizarProgresoRequest;
import ec.edu.uteq.presustentaciones.dto.ProgresoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import ec.edu.uteq.presustentaciones.services.ProgresoTitulacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgresoTitulacionControllerTest {

    @Mock private ProgresoTitulacionService progresoService;
    @Mock private UsuarioActualService usuarioActual;

    @InjectMocks private ProgresoTitulacionController controller;

    private final ProgresoTitulacionDTO dto = ProgresoTitulacionDTO.builder()
            .total(8).completados(1).porcentaje(13).build();

    @BeforeEach
    void setUp() {
        Estudiante e = new Estudiante();
        e.setId(3L);
        when(usuarioActual.estudiante()).thenReturn(e);
    }

    @Test
    void miProgresoUsaElEstudianteDelToken() {
        when(progresoService.obtener(3L)).thenReturn(dto);

        ResponseEntity<ProgresoTitulacionDTO> r = controller.miProgreso();

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(13, r.getBody().getPorcentaje());
        verify(progresoService).obtener(3L);
    }

    @Test
    void actualizarPasaElMapaYResuelveElEstudiante() {
        ActualizarProgresoRequest req = new ActualizarProgresoRequest();
        req.setPasos(Map.of("tema_definido", true));
        when(progresoService.actualizar(3L, req.getPasos())).thenReturn(dto);

        ResponseEntity<ProgresoTitulacionDTO> r = controller.actualizar(req);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(progresoService).actualizar(3L, req.getPasos());
    }
}
