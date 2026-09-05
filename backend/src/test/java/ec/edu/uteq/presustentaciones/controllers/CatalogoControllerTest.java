package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.GuardarCarreraRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarFacultadRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarModalidadRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarPeriodoRequest;
import ec.edu.uteq.presustentaciones.entities.AreaTematica;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.ConvocatoriaTitulacion;
import ec.edu.uteq.presustentaciones.entities.Facultad;
import ec.edu.uteq.presustentaciones.entities.LineaInvestigacion;
import ec.edu.uteq.presustentaciones.entities.ModalidadTitulacion;
import ec.edu.uteq.presustentaciones.entities.PeriodoAcademico;
import ec.edu.uteq.presustentaciones.repositories.AreaTematicaRepository;
import ec.edu.uteq.presustentaciones.repositories.CarreraRepository;
import ec.edu.uteq.presustentaciones.repositories.ConvocatoriaTitulacionRepository;
import ec.edu.uteq.presustentaciones.repositories.FacultadRepository;
import ec.edu.uteq.presustentaciones.repositories.LineaInvestigacionRepository;
import ec.edu.uteq.presustentaciones.repositories.ModalidadTitulacionRepository;
import ec.edu.uteq.presustentaciones.repositories.PeriodoAcademicoRepository;
import ec.edu.uteq.presustentaciones.services.AuditoriaService;
import ec.edu.uteq.presustentaciones.services.CatalogoAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CatalogoController concentraba la mayor cantidad de ramas sin ejercitar de todo el
 * paquete de controladores (1 de 136 líneas cubiertas y 102 ramas en cero). Casi toda
 * esa complejidad son validaciones de entrada del CRUD de la estructura académica
 * (facultad, carrera, modalidad, período), que es exactamente el tipo de código donde
 * un fallo silencioso deja crear catálogos inconsistentes.
 *
 * Se cubren las tres salidas de cada operación: éxito, rechazo por validación
 * (campos vacíos, duplicados, referencias inexistentes, rangos de fecha inválidos) y
 * el conflicto de integridad referencial al eliminar un catálogo que ya está en uso.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock private ModalidadTitulacionRepository modalidadRepo;
    @Mock private ConvocatoriaTitulacionRepository convocatoriaRepo;
    @Mock private LineaInvestigacionRepository lineaInvestigacionRepo;
    @Mock private AreaTematicaRepository areaTematicaRepo;
    @Mock private CarreraRepository carreraRepo;
    @Mock private PeriodoAcademicoRepository periodoAcademicoRepo;
    @Mock private FacultadRepository facultadRepo;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CatalogoAdminService catalogoAdminService;

    @InjectMocks
    private CatalogoController controller;

    @SuppressWarnings("unchecked")
    private String errorDe(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }

    private GuardarFacultadRequest facultadReq(String codigo, String nombre) {
        GuardarFacultadRequest req = new GuardarFacultadRequest();
        req.setCodigo(codigo);
        req.setNombre(nombre);
        return req;
    }

    private GuardarCarreraRequest carreraReq(String codigo, String nombre, Integer facultadId, String modalidad) {
        GuardarCarreraRequest req = new GuardarCarreraRequest();
        req.setCodigo(codigo);
        req.setNombre(nombre);
        req.setFacultadId(facultadId);
        req.setModalidadEstudio(modalidad);
        return req;
    }

    private GuardarModalidadRequest modalidadReq(String codigo, String nombre) {
        GuardarModalidadRequest req = new GuardarModalidadRequest();
        req.setCodigo(codigo);
        req.setNombre(nombre);
        return req;
    }

    private GuardarPeriodoRequest periodoReq(String codigo, String nombre, LocalDate inicio, LocalDate fin, Boolean activo) {
        GuardarPeriodoRequest req = new GuardarPeriodoRequest();
        req.setCodigo(codigo);
        req.setNombre(nombre);
        req.setFechaInicio(inicio);
        req.setFechaFin(fin);
        req.setActivo(activo);
        return req;
    }

    // ── Consultas de catálogo (abiertas a cualquier autenticado) ──────────────

    @Test
    void listarModalidadesDevuelveLoQueEntregaElRepositorio() {
        List<ModalidadTitulacion> esperado = List.of(ModalidadTitulacion.builder().id((short) 1).build());
        when(modalidadRepo.findAll()).thenReturn(esperado);

        assertSame(esperado, controller.listarModalidades().getBody());
    }

    @Test
    void listarLineasInvestigacionDevuelveLoQueEntregaElRepositorio() {
        List<LineaInvestigacion> esperado = List.of(new LineaInvestigacion());
        when(lineaInvestigacionRepo.findAll()).thenReturn(esperado);

        assertSame(esperado, controller.listarLineasInvestigacion().getBody());
    }

    @Test
    void listarAreasTematicasSinLineaIdDevuelveTodas() {
        List<AreaTematica> todas = List.of(new AreaTematica());
        when(areaTematicaRepo.findAll()).thenReturn(todas);

        assertSame(todas, controller.listarAreasTematicas(null).getBody());
        verify(areaTematicaRepo, never()).findByLineaInvestigacionId(any());
    }

    @Test
    void listarAreasTematicasConLineaIdFiltraPorEsaLinea() {
        List<AreaTematica> filtradas = List.of(new AreaTematica());
        when(areaTematicaRepo.findByLineaInvestigacionId(7)).thenReturn(filtradas);

        assertSame(filtradas, controller.listarAreasTematicas(7).getBody());
        verify(areaTematicaRepo, never()).findAll();
    }

    @Test
    void listarConvocatoriasActivasDevuelveSoloLasActivas() {
        List<ConvocatoriaTitulacion> activas = List.of(ConvocatoriaTitulacion.builder().id(1).build());
        when(convocatoriaRepo.findByActivaTrue()).thenReturn(activas);

        assertSame(activas, controller.listarConvocatoriasActivas().getBody());
    }

    @Test
    void convocatoriaActivaDevuelveLaConvocatoriaCuandoExiste() {
        ConvocatoriaTitulacion activa = ConvocatoriaTitulacion.builder().id(1).codigo("2026-1").build();
        when(convocatoriaRepo.findFirstByActivaTrue()).thenReturn(Optional.of(activa));

        assertSame(activa, controller.convocatoriaActiva().getBody());
    }

    @Test
    void convocatoriaActivaDevuelve200ConMensajeCuandoNoHayNinguna() {
        when(convocatoriaRepo.findFirstByActivaTrue()).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.convocatoriaActiva();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No hay convocatoria activa", errorDe(response));
    }

    @Test
    void listarCarrerasYPeriodosDeleganEnSusRepositorios() {
        when(carreraRepo.findAll()).thenReturn(List.of(Carrera.builder().id(1).build()));
        when(periodoAcademicoRepo.findAll()).thenReturn(List.of(PeriodoAcademico.builder().id(1).build()));

        assertEquals(1, controller.listarCarreras().getBody().size());
        assertEquals(1, controller.listarPeriodosAcademicos().getBody().size());
    }

    @Test
    void listarFacultadesDelegaEnElRepositorio() {
        when(facultadRepo.findAll()).thenReturn(List.of(Facultad.builder().id(1).build()));

        assertEquals(1, controller.listarFacultades().getBody().size());
    }

    // ── Facultades ────────────────────────────────────────────────────────────

    @Test
    void crearFacultadNormalizaElCodigoAMayusculasYLoGuarda() {
        when(facultadRepo.findByCodigo("FCI")).thenReturn(Optional.empty());
        when(facultadRepo.save(any(Facultad.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.crearFacultad(facultadReq("  fci  ", "  Ciencias de la Ingeniería  "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Facultad guardada = (Facultad) response.getBody();
        assertEquals("FCI", guardada.getCodigo());
        assertEquals("Ciencias de la Ingeniería", guardada.getNombre());
        verify(auditoriaService).marcarActorActual();
    }

    @Test
    void crearFacultadRechazaCodigoONombreVacios() {
        ResponseEntity<?> sinCodigo = controller.crearFacultad(facultadReq("   ", "Ciencias"));
        ResponseEntity<?> sinNombre = controller.crearFacultad(facultadReq("FCI", null));

        assertEquals(HttpStatus.BAD_REQUEST, sinCodigo.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, sinNombre.getStatusCode());
        assertEquals("Código y nombre son obligatorios.", errorDe(sinNombre));
        verify(facultadRepo, never()).save(any());
    }

    @Test
    void crearFacultadRechazaCodigoDuplicado() {
        when(facultadRepo.findByCodigo("FCI")).thenReturn(Optional.of(Facultad.builder().id(1).build()));

        ResponseEntity<?> response = controller.crearFacultad(facultadReq("FCI", "Ciencias"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Ya existe una facultad con ese código.", errorDe(response));
        verify(facultadRepo, never()).save(any());
    }

    @Test
    void actualizarFacultadCambiaElNombre() {
        Facultad existente = Facultad.builder().id(1).codigo("FCI").nombre("Antiguo").build();
        when(facultadRepo.findById(1)).thenReturn(Optional.of(existente));
        when(facultadRepo.save(existente)).thenReturn(existente);

        ResponseEntity<?> response = controller.actualizarFacultad(1, facultadReq(null, " Nuevo nombre "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Nuevo nombre", existente.getNombre());
    }

    @Test
    void actualizarFacultadInexistenteDevuelve404() {
        when(facultadRepo.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND,
                controller.actualizarFacultad(99, facultadReq(null, "X")).getStatusCode());
    }

    @Test
    void actualizarFacultadRechazaNombreVacio() {
        when(facultadRepo.findById(1)).thenReturn(Optional.of(Facultad.builder().id(1).build()));

        ResponseEntity<?> response = controller.actualizarFacultad(1, facultadReq(null, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El nombre no puede estar vacío.", errorDe(response));
        verify(facultadRepo, never()).save(any());
    }

    @Test
    void eliminarFacultadDevuelve204CuandoExiste() {
        when(facultadRepo.existsById(1)).thenReturn(true);

        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarFacultad(1).getStatusCode());
        verify(catalogoAdminService).eliminarFacultad(1);
    }

    @Test
    void eliminarFacultadInexistenteDevuelve404() {
        when(facultadRepo.existsById(99)).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND, controller.eliminarFacultad(99).getStatusCode());
        verify(catalogoAdminService, never()).eliminarFacultad(any());
    }

    @Test
    void eliminarFacultadConCarrerasAsociadasDevuelveErrorLegible() {
        when(facultadRepo.existsById(1)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK")).when(catalogoAdminService).eliminarFacultad(1);

        ResponseEntity<?> response = controller.eliminarFacultad(1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(errorDe(response).contains("carreras u otros registros asociados"));
    }

    // ── Carreras ──────────────────────────────────────────────────────────────

    @Test
    void crearCarreraGuardaConLaFacultadResuelta() {
        Facultad facultad = Facultad.builder().id(2).build();
        when(carreraRepo.findByCodigo("SW")).thenReturn(Optional.empty());
        when(facultadRepo.findById(2)).thenReturn(Optional.of(facultad));
        when(carreraRepo.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.crearCarrera(carreraReq("sw", "Software", 2, "PRESENCIAL"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Carrera guardada = (Carrera) response.getBody();
        assertEquals("SW", guardada.getCodigo());
        assertSame(facultad, guardada.getFacultad());
        assertEquals("PRESENCIAL", guardada.getModalidadEstudio());
    }

    @Test
    void crearCarreraRechazaCamposObligatoriosFaltantes() {
        ResponseEntity<?> sinFacultad = controller.crearCarrera(carreraReq("SW", "Software", null, null));

        assertEquals(HttpStatus.BAD_REQUEST, sinFacultad.getStatusCode());
        assertEquals("Código, nombre y facultad son obligatorios.", errorDe(sinFacultad));
        verify(carreraRepo, never()).save(any());
    }

    @Test
    void crearCarreraRechazaCodigoDuplicado() {
        when(carreraRepo.findByCodigo("SW")).thenReturn(Optional.of(Carrera.builder().id(1).build()));

        ResponseEntity<?> response = controller.crearCarrera(carreraReq("SW", "Software", 2, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Ya existe una carrera con ese código.", errorDe(response));
    }

    @Test
    void crearCarreraConFacultadInexistenteEsRechazada() {
        when(carreraRepo.findByCodigo("SW")).thenReturn(Optional.empty());
        when(facultadRepo.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.crearCarrera(carreraReq("SW", "Software", 99, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Facultad no encontrada.", errorDe(response));
        verify(carreraRepo, never()).save(any());
    }

    @Test
    void actualizarCarreraCambiaNombreModalidadYFacultad() {
        Carrera existente = Carrera.builder().id(1).nombre("Antiguo").build();
        Facultad nuevaFacultad = Facultad.builder().id(3).build();
        when(carreraRepo.findById(1)).thenReturn(Optional.of(existente));
        when(facultadRepo.findById(3)).thenReturn(Optional.of(nuevaFacultad));
        when(carreraRepo.save(existente)).thenReturn(existente);

        ResponseEntity<?> response = controller.actualizarCarrera(1, carreraReq(null, "Software", 3, "VIRTUAL"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Software", existente.getNombre());
        assertEquals("VIRTUAL", existente.getModalidadEstudio());
        assertSame(nuevaFacultad, existente.getFacultad());
    }

    @Test
    void actualizarCarreraSinModalidadNiFacultadConservaLosValoresPrevios() {
        Facultad facultadPrevia = Facultad.builder().id(1).build();
        Carrera existente = Carrera.builder().id(1).nombre("Antiguo")
                .modalidadEstudio("PRESENCIAL").facultad(facultadPrevia).build();
        when(carreraRepo.findById(1)).thenReturn(Optional.of(existente));
        when(carreraRepo.save(existente)).thenReturn(existente);

        controller.actualizarCarrera(1, carreraReq(null, "Software", null, null));

        assertEquals("PRESENCIAL", existente.getModalidadEstudio());
        assertSame(facultadPrevia, existente.getFacultad());
        verify(facultadRepo, never()).findById(any());
    }

    @Test
    void actualizarCarreraConFacultadInexistenteEsRechazada() {
        when(carreraRepo.findById(1)).thenReturn(Optional.of(Carrera.builder().id(1).build()));
        when(facultadRepo.findById(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.actualizarCarrera(1, carreraReq(null, "Software", 99, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Facultad no encontrada.", errorDe(response));
        verify(carreraRepo, never()).save(any());
    }

    @Test
    void actualizarCarreraInexistenteDevuelve404YNombreVacioEsRechazado() {
        when(carreraRepo.findById(99)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.actualizarCarrera(99, carreraReq(null, "X", null, null)).getStatusCode());

        when(carreraRepo.findById(1)).thenReturn(Optional.of(Carrera.builder().id(1).build()));
        ResponseEntity<?> vacio = controller.actualizarCarrera(1, carreraReq(null, null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, vacio.getStatusCode());
        assertEquals("El nombre no puede estar vacío.", errorDe(vacio));
    }

    @Test
    void eliminarCarreraCubreExitoNoEncontradaEIntegridad() {
        when(carreraRepo.existsById(1)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarCarrera(1).getStatusCode());

        when(carreraRepo.existsById(99)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.eliminarCarrera(99).getStatusCode());

        when(carreraRepo.existsById(2)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK")).when(catalogoAdminService).eliminarCarrera(2);
        ResponseEntity<?> conflicto = controller.eliminarCarrera(2);
        assertEquals(HttpStatus.BAD_REQUEST, conflicto.getStatusCode());
        assertTrue(errorDe(conflicto).contains("estudiantes u otros registros asociados"));
    }

    // ── Modalidades ───────────────────────────────────────────────────────────

    @Test
    void crearModalidadReemplazaEspaciosPorGuionBajoEnElCodigo() {
        when(modalidadRepo.findByCodigo("PROYECTO_DE_TITULACION")).thenReturn(Optional.empty());
        when(modalidadRepo.save(any(ModalidadTitulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.crearModalidad(
                modalidadReq(" proyecto de titulacion ", "Proyecto de Titulación"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PROYECTO_DE_TITULACION", ((ModalidadTitulacion) response.getBody()).getCodigo());
    }

    @Test
    void crearModalidadRechazaVaciosYDuplicados() {
        ResponseEntity<?> vacia = controller.crearModalidad(modalidadReq(null, "Proyecto"));
        assertEquals(HttpStatus.BAD_REQUEST, vacia.getStatusCode());

        when(modalidadRepo.findByCodigo("EXAMEN")).thenReturn(Optional.of(ModalidadTitulacion.builder().id((short) 1).build()));
        ResponseEntity<?> duplicada = controller.crearModalidad(modalidadReq("examen", "Examen"));
        assertEquals(HttpStatus.BAD_REQUEST, duplicada.getStatusCode());
        assertEquals("Ya existe una modalidad con ese código.", errorDe(duplicada));
    }

    @Test
    void actualizarModalidadCubreExitoNoEncontradaYNombreVacio() {
        ModalidadTitulacion existente = ModalidadTitulacion.builder().id((short) 1).nombre("Antiguo").build();
        when(modalidadRepo.findById((short) 1)).thenReturn(Optional.of(existente));
        when(modalidadRepo.save(existente)).thenReturn(existente);
        assertEquals(HttpStatus.OK, controller.actualizarModalidad((short) 1, modalidadReq(null, "Nuevo")).getStatusCode());
        assertEquals("Nuevo", existente.getNombre());

        when(modalidadRepo.findById((short) 99)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.actualizarModalidad((short) 99, modalidadReq(null, "X")).getStatusCode());

        when(modalidadRepo.findById((short) 2)).thenReturn(Optional.of(ModalidadTitulacion.builder().id((short) 2).build()));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.actualizarModalidad((short) 2, modalidadReq(null, "  ")).getStatusCode());
    }

    @Test
    void eliminarModalidadCubreExitoNoEncontradaEIntegridad() {
        when(modalidadRepo.existsById((short) 1)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarModalidad((short) 1).getStatusCode());

        when(modalidadRepo.existsById((short) 99)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.eliminarModalidad((short) 99).getStatusCode());

        when(modalidadRepo.existsById((short) 2)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK")).when(catalogoAdminService).eliminarModalidad((short) 2);
        ResponseEntity<?> conflicto = controller.eliminarModalidad((short) 2);
        assertEquals(HttpStatus.BAD_REQUEST, conflicto.getStatusCode());
        assertTrue(errorDe(conflicto).contains("solicitudes u otros registros asociados"));
    }

    // ── Períodos académicos ───────────────────────────────────────────────────

    @Test
    void crearPeriodoGuardaConActivoExplicito() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 6, 30);
        when(periodoAcademicoRepo.findByCodigo("2026-1")).thenReturn(Optional.empty());
        when(periodoAcademicoRepo.save(any(PeriodoAcademico.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.crearPeriodo(
                periodoReq("2026-1", "Primer semestre 2026", inicio, fin, true));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PeriodoAcademico guardado = (PeriodoAcademico) response.getBody();
        assertEquals("2026-1", guardado.getCodigo());
        assertTrue(guardado.getActivo());
    }

    @Test
    void crearPeriodoConActivoNuloLoGuardaComoInactivo() {
        when(periodoAcademicoRepo.findByCodigo("2026-2")).thenReturn(Optional.empty());
        when(periodoAcademicoRepo.save(any(PeriodoAcademico.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.crearPeriodo(periodoReq("2026-2", "Segundo",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), null));

        assertFalse(((PeriodoAcademico) response.getBody()).getActivo());
    }

    @Test
    void crearPeriodoRechazaCamposFaltantesFechasInvalidasYDuplicados() {
        ResponseEntity<?> sinFechas = controller.crearPeriodo(periodoReq("2026-1", "Primer", null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, sinFechas.getStatusCode());
        assertTrue(errorDe(sinFechas).contains("obligatorios"));

        LocalDate inicio = LocalDate.of(2026, 6, 30);
        LocalDate finAnterior = LocalDate.of(2026, 1, 1);
        ResponseEntity<?> fechasInvertidas = controller.crearPeriodo(
                periodoReq("2026-1", "Primer", inicio, finAnterior, null));
        assertEquals(HttpStatus.BAD_REQUEST, fechasInvertidas.getStatusCode());
        assertEquals("La fecha de fin debe ser posterior a la fecha de inicio.", errorDe(fechasInvertidas));

        when(periodoAcademicoRepo.findByCodigo("2026-1")).thenReturn(Optional.of(PeriodoAcademico.builder().id(1).build()));
        ResponseEntity<?> duplicado = controller.crearPeriodo(
                periodoReq("2026-1", "Primer", finAnterior, inicio, null));
        assertEquals(HttpStatus.BAD_REQUEST, duplicado.getStatusCode());
        assertEquals("Ya existe un período académico con ese código.", errorDe(duplicado));
    }

    @Test
    void actualizarPeriodoSinFechasNuevasConservaLasExistentes() {
        PeriodoAcademico existente = PeriodoAcademico.builder().id(1).nombre("Antiguo")
                .fechaInicio(LocalDate.of(2026, 1, 1)).fechaFin(LocalDate.of(2026, 6, 30))
                .activo(false).build();
        when(periodoAcademicoRepo.findById(1)).thenReturn(Optional.of(existente));
        when(periodoAcademicoRepo.save(existente)).thenReturn(existente);

        ResponseEntity<?> response = controller.actualizarPeriodo(1,
                periodoReq(null, "Nuevo nombre", null, null, true));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Nuevo nombre", existente.getNombre());
        assertEquals(LocalDate.of(2026, 1, 1), existente.getFechaInicio());
        assertEquals(LocalDate.of(2026, 6, 30), existente.getFechaFin());
        assertTrue(existente.getActivo());
    }

    @Test
    void actualizarPeriodoConRangoDeFechasInvalidoEsRechazado() {
        PeriodoAcademico existente = PeriodoAcademico.builder().id(1)
                .fechaInicio(LocalDate.of(2026, 1, 1)).fechaFin(LocalDate.of(2026, 6, 30)).build();
        when(periodoAcademicoRepo.findById(1)).thenReturn(Optional.of(existente));

        ResponseEntity<?> response = controller.actualizarPeriodo(1,
                periodoReq(null, "Nombre", LocalDate.of(2026, 12, 1), null, null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("La fecha de fin debe ser posterior a la fecha de inicio.", errorDe(response));
        verify(periodoAcademicoRepo, never()).save(any());
    }

    @Test
    void actualizarPeriodoInexistenteDevuelve404YNombreVacioEsRechazado() {
        when(periodoAcademicoRepo.findById(99)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                controller.actualizarPeriodo(99, periodoReq(null, "X", null, null, null)).getStatusCode());

        when(periodoAcademicoRepo.findById(1)).thenReturn(Optional.of(PeriodoAcademico.builder().id(1).build()));
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.actualizarPeriodo(1, periodoReq(null, "   ", null, null, null)).getStatusCode());
    }

    @Test
    void eliminarPeriodoCubreExitoNoEncontradoEIntegridad() {
        when(periodoAcademicoRepo.existsById(1)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, controller.eliminarPeriodo(1).getStatusCode());

        when(periodoAcademicoRepo.existsById(99)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, controller.eliminarPeriodo(99).getStatusCode());

        when(periodoAcademicoRepo.existsById(2)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK")).when(catalogoAdminService).eliminarPeriodo(2);
        ResponseEntity<?> conflicto = controller.eliminarPeriodo(2);
        assertEquals(HttpStatus.BAD_REQUEST, conflicto.getStatusCode());
        assertTrue(errorDe(conflicto).contains("estudiantes o convocatorias asociadas"));
    }
}
