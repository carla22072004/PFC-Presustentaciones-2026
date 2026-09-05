package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.GuardarRecursoRequest;
import ec.edu.uteq.presustentaciones.dto.RecursoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import ec.edu.uteq.presustentaciones.services.RecursoTitulacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RecursoTitulacionController no tenía ningún test dedicado (0% de cobertura pese a que su
 * servicio ya está probado al 76%) -- brecha identificada en la auditoría de cobertura de
 * 2026-09-04. Cubre la resolución de carreraId por defecto (comportamiento real y no trivial
 * del endpoint /listar) y que el resto de métodos delega correctamente.
 */
@ExtendWith(MockitoExtension.class)
class RecursoTitulacionControllerTest {

    @Mock private RecursoTitulacionService recursoService;
    @Mock private UsuarioActualService usuarioActual;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks
    private RecursoTitulacionController controller;

    private RecursoTitulacionDTO recursoMock;

    @BeforeEach
    void setUp() {
        recursoMock = RecursoTitulacionDTO.builder().id(1).titulo("Guía de formato APA").build();
    }

    @Test
    void listarConCarreraIdExplicitoNoConsultaAlEstudianteActual() {
        when(recursoService.listar(5)).thenReturn(List.of(recursoMock));

        ResponseEntity<List<RecursoTitulacionDTO>> response = controller.listar(5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(usuarioActual, never()).estudianteIdOrNull();
        verify(recursoService).listar(5);
    }

    @Test
    void listarSinCarreraIdResuelveLaCarreraDelEstudianteAutenticado() {
        Carrera carrera = Carrera.builder().id(3).build();
        Estudiante estudiante = Estudiante.builder().id(7L).carreraEntidad(carrera).build();
        when(usuarioActual.estudianteIdOrNull()).thenReturn(7L);
        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(recursoService.listar(3)).thenReturn(List.of(recursoMock));

        ResponseEntity<List<RecursoTitulacionDTO>> response = controller.listar(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(recursoService).listar(3);
    }

    @Test
    void listarSinCarreraIdYSinPerfilDeEstudianteListaTodoSinFiltrar() {
        // Caller autenticado que no es estudiante (ej. ADMIN/DOCENTE): sin id de estudiante,
        // el filtro efectivo debe quedar en null (recursos generales de todas las carreras).
        when(usuarioActual.estudianteIdOrNull()).thenReturn(null);
        when(recursoService.listar(null)).thenReturn(Collections.emptyList());

        ResponseEntity<List<RecursoTitulacionDTO>> response = controller.listar(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(estudianteRepository, never()).findById(anyLong());
        verify(recursoService).listar(null);
    }

    @Test
    void crearDelegaYDevuelve201() {
        GuardarRecursoRequest req = new GuardarRecursoRequest();
        req.setTitulo("Guía de formato APA");
        req.setCategoria("GUIA");
        req.setUrlArchivo("https://example.com/guia.pdf");
        when(recursoService.crear(req)).thenReturn(recursoMock);

        ResponseEntity<RecursoTitulacionDTO> response = controller.crear(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Guía de formato APA", response.getBody().getTitulo());
        verify(recursoService).crear(req);
    }

    @Test
    void actualizarDelegaYDevuelve200() {
        GuardarRecursoRequest req = new GuardarRecursoRequest();
        req.setTitulo("Guía actualizada");
        req.setCategoria("GUIA");
        req.setUrlArchivo("https://example.com/guia-v2.pdf");
        when(recursoService.actualizar(eq(1), eq(req))).thenReturn(recursoMock);

        ResponseEntity<RecursoTitulacionDTO> response = controller.actualizar(1, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(recursoService).actualizar(1, req);
    }

    @Test
    void eliminarDelegaYDevuelve204() {
        ResponseEntity<Void> response = controller.eliminar(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(recursoService).eliminar(1);
    }
}
