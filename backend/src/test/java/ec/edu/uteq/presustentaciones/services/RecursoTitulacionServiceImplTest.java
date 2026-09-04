package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GuardarRecursoRequest;
import ec.edu.uteq.presustentaciones.dto.RecursoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.RecursoTitulacion;
import ec.edu.uteq.presustentaciones.repositories.CarreraRepository;
import ec.edu.uteq.presustentaciones.repositories.RecursoTitulacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoTitulacionServiceImplTest {

    @Mock private RecursoTitulacionRepository recursoRepository;
    @Mock private CarreraRepository carreraRepository;

    @InjectMocks private RecursoTitulacionServiceImpl service;

    private RecursoTitulacion recurso() {
        Carrera c = new Carrera();
        c.setId(1);
        c.setNombre("Ingeniería en Software");
        return RecursoTitulacion.builder()
                .id(5).titulo("Plantilla").categoria("Plantillas")
                .urlArchivo("http://x/y.docx").carrera(c).build();
    }

    @Test
    void listarSinCarreraUsaListarTodos() {
        when(recursoRepository.listarTodos()).thenReturn(List.of(recurso()));

        List<RecursoTitulacionDTO> r = service.listar(null);

        assertEquals(1, r.size());
        assertEquals("Ingeniería en Software", r.get(0).getCarreraNombre());
        verify(recursoRepository).listarTodos();
        verify(recursoRepository, never()).listarVisiblesParaCarrera(any());
    }

    @Test
    void listarConCarreraFiltra() {
        when(recursoRepository.listarVisiblesParaCarrera(1)).thenReturn(List.of(recurso()));

        assertEquals(1, service.listar(1).size());
        verify(recursoRepository).listarVisiblesParaCarrera(1);
    }

    @Test
    void crearRecursoGeneralSinCarrera() {
        GuardarRecursoRequest req = new GuardarRecursoRequest();
        req.setTitulo("  Guía  "); req.setCategoria("Guías"); req.setUrlArchivo("http://x");
        when(recursoRepository.save(any(RecursoTitulacion.class))).thenAnswer(i -> i.getArgument(0));

        RecursoTitulacionDTO dto = service.crear(req);

        assertEquals("Guía", dto.getTitulo());
        assertNull(dto.getCarreraId());
        verify(carreraRepository, never()).findById(any());
    }

    @Test
    void crearConCarreraInexistenteLanza() {
        GuardarRecursoRequest req = new GuardarRecursoRequest();
        req.setTitulo("X"); req.setCategoria("Y"); req.setUrlArchivo("Z"); req.setCarreraId(99);
        when(carreraRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.crear(req));
    }

    @Test
    void actualizarInexistenteLanza() {
        when(recursoRepository.findById(7)).thenReturn(Optional.empty());
        GuardarRecursoRequest req = new GuardarRecursoRequest();
        req.setTitulo("X"); req.setCategoria("Y"); req.setUrlArchivo("Z");

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(7, req));
    }

    @Test
    void eliminarInexistenteLanza() {
        when(recursoRepository.existsById(7)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.eliminar(7));
        verify(recursoRepository, never()).deleteById(any());
    }

    @Test
    void eliminarExistenteBorra() {
        when(recursoRepository.existsById(5)).thenReturn(true);
        service.eliminar(5);
        verify(recursoRepository).deleteById(5);
    }
}
