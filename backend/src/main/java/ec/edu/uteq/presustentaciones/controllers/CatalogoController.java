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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CatalogoController {

    private static final String PERMISO_GESTIONAR = "@permisoService.tienePermiso(authentication, 'CARRERAS_GESTIONAR')";

    private final ModalidadTitulacionRepository modalidadRepo;
    private final ConvocatoriaTitulacionRepository convocatoriaRepo;
    private final LineaInvestigacionRepository lineaInvestigacionRepo;
    private final AreaTematicaRepository areaTematicaRepo;
    private final CarreraRepository carreraRepo;
    private final PeriodoAcademicoRepository periodoAcademicoRepo;
    private final FacultadRepository facultadRepo;
    private final AuditoriaService auditoriaService;
    private final CatalogoAdminService catalogoAdminService;

    /** Lista todas las modalidades de titulación disponibles */
    @GetMapping("/modalidades")
    public ResponseEntity<List<ModalidadTitulacion>> listarModalidades() {
        return ResponseEntity.ok(modalidadRepo.findAll());
    }

    /** Lista todas las líneas de investigación institucionales disponibles */
    @GetMapping("/lineas-investigacion")
    public ResponseEntity<List<LineaInvestigacion>> listarLineasInvestigacion() {
        return ResponseEntity.ok(lineaInvestigacionRepo.findAll());
    }

    /**
     * Lista las áreas temáticas. Si se pasa lineaId, filtra solo las de esa línea
     * (uso típico: poblar el segundo dropdown dependiente del formulario de registro de tema).
     */
    @GetMapping("/areas-tematicas")
    public ResponseEntity<List<AreaTematica>> listarAreasTematicas(
            @RequestParam(required = false) Integer lineaId) {
        if (lineaId != null) {
            return ResponseEntity.ok(areaTematicaRepo.findByLineaInvestigacionId(lineaId));
        }
        return ResponseEntity.ok(areaTematicaRepo.findAll());
    }

    /** Lista todas las convocatorias activas */
    @GetMapping("/convocatorias")
    public ResponseEntity<List<ConvocatoriaTitulacion>> listarConvocatoriasActivas() {
        return ResponseEntity.ok(convocatoriaRepo.findByActivaTrue());
    }

    /** Retorna la convocatoria activa actual (para autocompletar en el formulario) */
    @GetMapping("/convocatoria-activa")
    public ResponseEntity<?> convocatoriaActiva() {
        return convocatoriaRepo.findFirstByActivaTrue()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("error", "No hay convocatoria activa")));
    }

    /** Lista todas las carreras -- usado por Gestión de Estudiantes para elegir la carrera al registrar/editar */
    @GetMapping("/carreras")
    public ResponseEntity<List<Carrera>> listarCarreras() {
        return ResponseEntity.ok(carreraRepo.findAll());
    }

    /** Lista todos los períodos académicos -- usado por Gestión de Estudiantes para el período de ingreso */
    @GetMapping("/periodos-academicos")
    public ResponseEntity<List<PeriodoAcademico>> listarPeriodosAcademicos() {
        return ResponseEntity.ok(periodoAcademicoRepo.findAll());
    }

    // ── Gestión de Carreras (CARRERAS_GESTIONAR, solo ADMIN) ──────────────────────
    // CRUD de la estructura académica base: facultades, carreras, modalidades de
    // titulación y períodos académicos. Los GET de arriba quedan abiertos a cualquier
    // autenticado (@PreAuthorize de clase); estos métodos lo sobrescriben con el
    // permiso dedicado porque en Spring Security el @PreAuthorize de método reemplaza
    // -- no combina con -- el de clase.

    @GetMapping("/facultades")
    public ResponseEntity<List<Facultad>> listarFacultades() {
        return ResponseEntity.ok(facultadRepo.findAll());
    }

    @PostMapping("/facultades")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> crearFacultad(@RequestBody GuardarFacultadRequest req) {
        String codigo = req.getCodigo() == null ? "" : req.getCodigo().trim().toUpperCase();
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código y nombre son obligatorios."));
        }
        if (facultadRepo.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una facultad con ese código."));
        }
        auditoriaService.marcarActorActual();
        Facultad facultad = facultadRepo.save(Facultad.builder().codigo(codigo).nombre(nombre).build());
        return ResponseEntity.ok(facultad);
    }

    @PutMapping("/facultades/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> actualizarFacultad(@PathVariable Integer id, @RequestBody GuardarFacultadRequest req) {
        auditoriaService.marcarActorActual();
        Facultad facultad = facultadRepo.findById(id).orElse(null);
        if (facultad == null) {
            return ResponseEntity.notFound().build();
        }
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío."));
        }
        facultad.setNombre(nombre);
        return ResponseEntity.ok(facultadRepo.save(facultad));
    }

    @DeleteMapping("/facultades/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    public ResponseEntity<?> eliminarFacultad(@PathVariable Integer id) {
        if (!facultadRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            catalogoAdminService.eliminarFacultad(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar: hay carreras u otros registros asociados a esta facultad."));
        }
    }

    @PostMapping("/carreras")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> crearCarrera(@RequestBody GuardarCarreraRequest req) {
        String codigo = req.getCodigo() == null ? "" : req.getCodigo().trim().toUpperCase();
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (codigo.isEmpty() || nombre.isEmpty() || req.getFacultadId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código, nombre y facultad son obligatorios."));
        }
        if (carreraRepo.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una carrera con ese código."));
        }
        Facultad facultad = facultadRepo.findById(req.getFacultadId()).orElse(null);
        if (facultad == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Facultad no encontrada."));
        }
        auditoriaService.marcarActorActual();
        Carrera carrera = carreraRepo.save(Carrera.builder()
                .codigo(codigo).nombre(nombre).facultad(facultad)
                .modalidadEstudio(req.getModalidadEstudio())
                .build());
        return ResponseEntity.ok(carrera);
    }

    @PutMapping("/carreras/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> actualizarCarrera(@PathVariable Integer id, @RequestBody GuardarCarreraRequest req) {
        auditoriaService.marcarActorActual();
        Carrera carrera = carreraRepo.findById(id).orElse(null);
        if (carrera == null) {
            return ResponseEntity.notFound().build();
        }
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío."));
        }
        carrera.setNombre(nombre);
        if (req.getModalidadEstudio() != null) {
            carrera.setModalidadEstudio(req.getModalidadEstudio());
        }
        if (req.getFacultadId() != null) {
            Facultad facultad = facultadRepo.findById(req.getFacultadId()).orElse(null);
            if (facultad == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Facultad no encontrada."));
            }
            carrera.setFacultad(facultad);
        }
        return ResponseEntity.ok(carreraRepo.save(carrera));
    }

    @DeleteMapping("/carreras/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    public ResponseEntity<?> eliminarCarrera(@PathVariable Integer id) {
        if (!carreraRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            catalogoAdminService.eliminarCarrera(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar: hay estudiantes u otros registros asociados a esta carrera."));
        }
    }

    @PostMapping("/modalidades")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> crearModalidad(@RequestBody GuardarModalidadRequest req) {
        String codigo = req.getCodigo() == null ? "" : req.getCodigo().trim().toUpperCase().replaceAll("\\s+", "_");
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código y nombre son obligatorios."));
        }
        if (modalidadRepo.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una modalidad con ese código."));
        }
        auditoriaService.marcarActorActual();
        ModalidadTitulacion modalidad = modalidadRepo.save(ModalidadTitulacion.builder().codigo(codigo).nombre(nombre).build());
        return ResponseEntity.ok(modalidad);
    }

    @PutMapping("/modalidades/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> actualizarModalidad(@PathVariable Short id, @RequestBody GuardarModalidadRequest req) {
        auditoriaService.marcarActorActual();
        ModalidadTitulacion modalidad = modalidadRepo.findById(id).orElse(null);
        if (modalidad == null) {
            return ResponseEntity.notFound().build();
        }
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío."));
        }
        modalidad.setNombre(nombre);
        return ResponseEntity.ok(modalidadRepo.save(modalidad));
    }

    @DeleteMapping("/modalidades/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    public ResponseEntity<?> eliminarModalidad(@PathVariable Short id) {
        if (!modalidadRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            catalogoAdminService.eliminarModalidad(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar: hay solicitudes u otros registros asociados a esta modalidad."));
        }
    }

    @PostMapping("/periodos-academicos")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> crearPeriodo(@RequestBody GuardarPeriodoRequest req) {
        String codigo = req.getCodigo() == null ? "" : req.getCodigo().trim().toUpperCase();
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (codigo.isEmpty() || nombre.isEmpty() || req.getFechaInicio() == null || req.getFechaFin() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código, nombre, fecha de inicio y fecha de fin son obligatorios."));
        }
        if (!req.getFechaFin().isAfter(req.getFechaInicio())) {
            return ResponseEntity.badRequest().body(Map.of("error", "La fecha de fin debe ser posterior a la fecha de inicio."));
        }
        if (periodoAcademicoRepo.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un período académico con ese código."));
        }
        auditoriaService.marcarActorActual();
        PeriodoAcademico periodo = periodoAcademicoRepo.save(PeriodoAcademico.builder()
                .codigo(codigo).nombre(nombre)
                .fechaInicio(req.getFechaInicio()).fechaFin(req.getFechaFin())
                .activo(req.getActivo() != null && req.getActivo())
                .build());
        return ResponseEntity.ok(periodo);
    }

    @PutMapping("/periodos-academicos/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    @Transactional
    public ResponseEntity<?> actualizarPeriodo(@PathVariable Integer id, @RequestBody GuardarPeriodoRequest req) {
        auditoriaService.marcarActorActual();
        PeriodoAcademico periodo = periodoAcademicoRepo.findById(id).orElse(null);
        if (periodo == null) {
            return ResponseEntity.notFound().build();
        }
        String nombre = req.getNombre() == null ? "" : req.getNombre().trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío."));
        }
        LocalDate inicio = req.getFechaInicio() != null ? req.getFechaInicio() : periodo.getFechaInicio();
        LocalDate fin = req.getFechaFin() != null ? req.getFechaFin() : periodo.getFechaFin();
        if (!fin.isAfter(inicio)) {
            return ResponseEntity.badRequest().body(Map.of("error", "La fecha de fin debe ser posterior a la fecha de inicio."));
        }
        periodo.setNombre(nombre);
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        if (req.getActivo() != null) {
            periodo.setActivo(req.getActivo());
        }
        return ResponseEntity.ok(periodoAcademicoRepo.save(periodo));
    }

    @DeleteMapping("/periodos-academicos/{id}")
    @PreAuthorize(PERMISO_GESTIONAR)
    public ResponseEntity<?> eliminarPeriodo(@PathVariable Integer id) {
        if (!periodoAcademicoRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            catalogoAdminService.eliminarPeriodo(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar: hay estudiantes o convocatorias asociadas a este período."));
        }
    }
}
