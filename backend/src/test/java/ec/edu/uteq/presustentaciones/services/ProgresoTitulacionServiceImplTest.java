package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ProgresoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.ProgresoEstudiante;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.ProgresoEstudianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgresoTitulacionServiceImplTest {

    @Mock private ProgresoEstudianteRepository progresoRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks private ProgresoTitulacionServiceImpl service;

    @Test
    void obtenerSinRegistroDevuelveTodoEnCero() {
        when(progresoRepository.findByEstudianteId(1L)).thenReturn(Optional.empty());

        ProgresoTitulacionDTO dto = service.obtener(1L);

        assertEquals(8, dto.getTotal());
        assertEquals(0, dto.getCompletados());
        assertEquals(0, dto.getPorcentaje());
        assertTrue(dto.getPasos().stream().noneMatch(ProgresoTitulacionDTO.PasoDTO::isCompletado));
    }

    @Test
    void obtenerConEstadoGuardadoCalculaPorcentaje() {
        ProgresoEstudiante pe = ProgresoEstudiante.builder()
                .pasosJson("{\"tema_definido\":true,\"tutor_asignado\":true}")
                .build();
        when(progresoRepository.findByEstudianteId(1L)).thenReturn(Optional.of(pe));

        ProgresoTitulacionDTO dto = service.obtener(1L);

        assertEquals(2, dto.getCompletados());
        assertEquals(25, dto.getPorcentaje()); // 2 / 8
    }

    @Test
    void actualizarFusionaConLoGuardadoYPersiste() {
        Estudiante est = new Estudiante();
        est.setId(1L);
        ProgresoEstudiante pe = ProgresoEstudiante.builder()
                .estudiante(est).pasosJson("{\"tema_definido\":true}").build();
        when(progresoRepository.findByEstudianteId(1L)).thenReturn(Optional.of(pe));

        ProgresoTitulacionDTO dto = service.actualizar(1L, Map.of("tutor_asignado", true));

        assertEquals(2, dto.getCompletados());
        verify(progresoRepository).save(pe);
        assertTrue(pe.getPasosJson().contains("tutor_asignado"));
        assertTrue(pe.getPasosJson().contains("tema_definido"));
    }

    @Test
    void actualizarIgnoraClavesFueraDelCatalogo() {
        Estudiante est = new Estudiante();
        est.setId(1L);
        when(progresoRepository.findByEstudianteId(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(est));
        when(progresoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProgresoTitulacionDTO dto = service.actualizar(1L, Map.of("paso_inventado", true, "tema_definido", true));

        assertEquals(1, dto.getCompletados());
        assertTrue(dto.getPasos().stream()
                .noneMatch(p -> "paso_inventado".equals(p.getClave())));
    }

    @Test
    void actualizarCreaRegistroSiNoExisteYFallaSiEstudianteNoExiste() {
        when(progresoRepository.findByEstudianteId(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(1L, Map.of("tema_definido", true)));
    }
}
