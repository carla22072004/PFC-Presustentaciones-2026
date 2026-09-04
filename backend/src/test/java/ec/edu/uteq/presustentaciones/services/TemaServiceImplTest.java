package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.LineaInvestigacion;
import ec.edu.uteq.presustentaciones.entities.TemaGuardadoEstudiante;
import ec.edu.uteq.presustentaciones.entities.TemaPropuesto;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.TemaGuardadoEstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.TemaPropuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemaServiceImplTest {

    @Mock private TemaPropuestoRepository temaPropuestoRepository;
    @Mock private TemaGuardadoEstudianteRepository temaGuardadoEstudianteRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks private TemaServiceImpl temaService;

    private TemaPropuesto temaMock;
    private Estudiante estudianteMock;

    @BeforeEach
    void setUp() {
        Carrera carrera = new Carrera();
        carrera.setId(1);
        carrera.setNombre("Ingeniería en Software");

        LineaInvestigacion linea = new LineaInvestigacion();
        linea.setId(1);
        linea.setNombre("Ingeniería de Software y Calidad");

        temaMock = TemaPropuesto.builder()
                .id(1).titulo("Tema Prueba").nivelDificultad("BASICO")
                .carrera(carrera).lineaInvestigacion(linea)
                .build();

        estudianteMock = new Estudiante();
        estudianteMock.setId(1L);
    }

    @Test
    void generarIdeasPorCarreraYLinea() {
        GenerarTemaRequest request = new GenerarTemaRequest();
        request.setCarreraId(1);
        request.setLineaInvestigacionId(1);
        when(temaPropuestoRepository.findByCarreraIdAndLineaInvestigacionId(1, 1))
                .thenReturn(Collections.singletonList(temaMock));

        List<TemaPropuestoDTO> resultados = temaService.generarIdeas(request);

        assertFalse(resultados.isEmpty());
        assertEquals("Tema Prueba", resultados.get(0).getTitulo());
        assertEquals("Ingeniería en Software", resultados.get(0).getCarreraNombre());
        verify(temaPropuestoRepository).findByCarreraIdAndLineaInvestigacionId(1, 1);
    }

    @Test
    void generarIdeasSoloPorCarreraCuandoNoHayLinea() {
        GenerarTemaRequest request = new GenerarTemaRequest();
        request.setCarreraId(1);
        when(temaPropuestoRepository.findByCarreraId(1)).thenReturn(Collections.singletonList(temaMock));

        List<TemaPropuestoDTO> resultados = temaService.generarIdeas(request);

        assertEquals(1, resultados.size());
        verify(temaPropuestoRepository).findByCarreraId(1);
        verify(temaPropuestoRepository, never()).findByCarreraIdAndLineaInvestigacionId(anyInt(), anyInt());
    }

    @Test
    void explorarMarcaLosTemasYaGuardadosDelEstudiante() {
        when(temaPropuestoRepository.buscarConFiltros(1, null, null, null))
                .thenReturn(Collections.singletonList(temaMock));
        when(temaGuardadoEstudianteRepository.findTemaIdsByEstudianteId(1L))
                .thenReturn(List.of(1));

        List<TemaPropuestoDTO> resultados = temaService.explorar(1, null, null, null, 1L);

        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).getGuardado());
    }

    @Test
    void explorarSinEstudianteNoConsultaGuardados() {
        when(temaPropuestoRepository.buscarConFiltros(null, null, null, null))
                .thenReturn(Collections.singletonList(temaMock));

        List<TemaPropuestoDTO> resultados = temaService.explorar(null, null, null, null, null);

        assertNull(resultados.get(0).getGuardado());
        verify(temaGuardadoEstudianteRepository, never()).findTemaIdsByEstudianteId(any());
    }

    @Test
    void obtenerDetalleLanzaSiNoExiste() {
        when(temaPropuestoRepository.findByIdConCatalogos(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> temaService.obtenerDetalle(99));
    }

    @Test
    void guardarTemaEstudianteExitoso() {
        when(temaGuardadoEstudianteRepository.existsByEstudianteIdAndTemaPropuestoId(1L, 1)).thenReturn(false);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteMock));
        when(temaPropuestoRepository.findById(1)).thenReturn(Optional.of(temaMock));

        temaService.guardarTemaEstudiante(1L, 1);

        verify(temaGuardadoEstudianteRepository).save(any(TemaGuardadoEstudiante.class));
    }

    @Test
    void guardarTemaYaGuardadoLanzaIllegalState() {
        when(temaGuardadoEstudianteRepository.existsByEstudianteIdAndTemaPropuestoId(1L, 1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> temaService.guardarTemaEstudiante(1L, 1));
        verify(temaGuardadoEstudianteRepository, never()).save(any());
    }

    @Test
    void quitarTemaGuardadoExitoso() {
        when(temaGuardadoEstudianteRepository.deleteByEstudianteIdAndTemaPropuestoId(1L, 1)).thenReturn(1);

        assertDoesNotThrow(() -> temaService.quitarTemaGuardado(1L, 1));
    }

    @Test
    void quitarTemaGuardadoInexistenteLanza() {
        when(temaGuardadoEstudianteRepository.deleteByEstudianteIdAndTemaPropuestoId(1L, 1)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> temaService.quitarTemaGuardado(1L, 1));
    }

    @Test
    void obtenerTemasGuardadosMapeaYMarcaGuardado() {
        TemaGuardadoEstudiante guardado = TemaGuardadoEstudiante.builder()
                .id(1).estudiante(estudianteMock).temaPropuesto(temaMock).build();
        when(temaGuardadoEstudianteRepository.findByEstudianteIdOrderByFechaGuardadoDesc(1L))
                .thenReturn(List.of(guardado));

        List<TemaPropuestoDTO> resultados = temaService.obtenerTemasGuardados(1L);

        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).getGuardado());
    }
}
