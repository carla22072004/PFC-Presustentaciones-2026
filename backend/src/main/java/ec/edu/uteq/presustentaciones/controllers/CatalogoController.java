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

    /**
     * @return 200 con todas las modalidades de titulación disponibles para elegir al crear
     *         una solicitud
     */
    @GetMapping("/modalidades")
    public ResponseEntity<List<ModalidadTitulacion>> listarModalidades() {
        return ResponseEntity.ok(modalidadRepo.findAll());
    }

    /**
     * @return 200 con las líneas de investigación institucionales
     */
    @GetMapping("/lineas-investigacion")
    public ResponseEntity<List<LineaInvestigacion>> listarLineasInvestigacion() {
        return ResponseEntity.ok(lineaInvestigacionRepo.findAll());
    }

    /**
     * Lista las áreas temáticas. Si se pasa lineaId, filtra solo las de esa línea
     * (uso típico: poblar el segundo dropdown dependiente del formulario de registro de tema).
     *
     * @param lineaId línea de investigación por la que filtrar; si es null devuelve todas
     * @return 200 con las áreas temáticas correspondientes
     */
    @GetMapping("/areas-tematicas")
    public ResponseEntity<List<AreaTematica>> listarAreasTematicas(
            @RequestParam(required = false) Integer lineaId) {
        if (lineaId != null) {
            return ResponseEntity.ok(areaTematicaRepo.findByLineaInvestigacionId(lineaId));
        }
        return ResponseEntity.ok(areaTematicaRepo.findAll());
    }

    /**
     * @return 200 con las convocatorias de titulación marcadas como activas
     */
    @GetMapping("/convocatorias")
    public ResponseEntity<List<ConvocatoriaTitulacion>> listarConvocatoriasActivas() {
        return ResponseEntity.ok(convocatoriaRepo.findByActivaTrue());
    }

    /**
     * Convocatoria vigente, para autocompletar el formulario de solicitud.
     *
     * @return 200 con la convocatoria activa; si no hay ninguna devuelve igualmente 200 con
     *         un mensaje de error en el cuerpo, no un 404, para que el formulario pueda
     *         mostrar el aviso sin tratarlo como fallo de red
     */
    @GetMapping("/convocatoria-activa")
    public ResponseEntity<?> convocatoriaActiva() {
        return convocatoriaRepo.findFirstByActivaTrue()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("error", "No hay convocatoria activa")));
    }

    /**
     * @return 200 con todas las carreras; lo usa Gestión de Estudiantes para elegir la
     *         carrera al registrar o editar un estudiante
     */
    @GetMapping("/carreras")
    public ResponseEntity<List<Carrera>> listarCarreras() {
        return ResponseEntity.ok(carreraRepo.findAll());
    }

    /**
     * @return 200 con todos los períodos académicos; lo usa Gestión de Estudiantes para
     *         asignar el período de ingreso
     */
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

    /**
     * @return 200 con todas las facultades
     */
    @GetMapping("/facultades")
    public ResponseEntity<List<Facultad>> listarFacultades() {
        return ResponseEntity.ok(facultadRepo.findAll());
    }

    /**
     * Crea una facultad. El código se normaliza a mayúsculas sin espacios sobrantes.
     *
     * @param req código y nombre de la facultad; ambos obligatorios
     * @return 200 con la facultad creada, o 400 si falta algún campo o el código ya existe
     */
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

    /**
     * Renombra una facultad. El código no se modifica para no dejar huérfanas las
     * referencias existentes.
     *
     * @param id  facultad a actualizar
     * @param req nuevo nombre
     * @return 200 con la facultad actualizada, 404 si no existe, o 400 si el nombre viene vacío
     */
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

    /**
     * Elimina una facultad.
     *
     * @param id facultad a eliminar
     * @return 204 si se eliminó, 404 si no existe, o 400 si tiene carreras u otros registros
     *         asociados que impiden el borrado
     */
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

    /**
     * Crea una carrera dentro de una facultad existente.
     *
     * @param req código, nombre, facultadId y modalidad de estudio; los tres primeros obligatorios
     * @return 200 con la carrera creada, o 400 si falta un campo, el código ya existe o la
     *         facultad indicada no existe
     */
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

    /**
     * Actualiza una carrera. La modalidad y la facultad sólo se tocan si vienen en el cuerpo;
     * el código nunca se modifica.
     *
     * @param id  carrera a actualizar
     * @param req nombre (obligatorio) y, opcionalmente, modalidad de estudio y facultadId
     * @return 200 con la carrera actualizada, 404 si no existe, o 400 si el nombre viene
     *         vacío o la facultad indicada no existe
     */
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

    /**
     * Elimina una carrera.
     *
     * @param id carrera a eliminar
     * @return 204 si se eliminó, 404 si no existe, o 400 si tiene estudiantes u otros
     *         registros asociados
     */
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

    /**
     * Crea una modalidad de titulación. El código se normaliza a mayúsculas y los espacios
     * internos se sustituyen por guiones bajos.
     *
     * @param req código y nombre; ambos obligatorios
     * @return 200 con la modalidad creada, o 400 si falta un campo o el código ya existe
     */
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

    /**
     * Renombra una modalidad, sin tocar su código.
     *
     * @param id  modalidad a actualizar
     * @param req nuevo nombre
     * @return 200 con la modalidad actualizada, 404 si no existe, o 400 si el nombre viene vacío
     */
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

    /**
     * Elimina una modalidad de titulación.
     *
     * @param id modalidad a eliminar
     * @return 204 si se eliminó, 404 si no existe, o 400 si hay solicitudes asociadas
     */
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

    /**
     * Crea un período académico validando que el rango de fechas tenga sentido.
     *
     * @param req código, nombre, fecha de inicio y fecha de fin (obligatorios) más el
     *            indicador de activo (si no viene, el período se crea inactivo)
     * @return 200 con el período creado, o 400 si falta un campo, si la fecha de fin no es
     *         posterior a la de inicio, o si el código ya existe
     */
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

    /**
     * Actualiza un período académico. Las fechas que no vengan en el cuerpo conservan su
     * valor actual, y el rango resultante se vuelve a validar.
     *
     * @param id  período a actualizar
     * @param req nombre (obligatorio) y, opcionalmente, fechas y estado activo
     * @return 200 con el período actualizado, 404 si no existe, o 400 si el nombre viene
     *         vacío o el rango de fechas resultante es inválido
     */
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

    /**
     * Elimina un período académico.
     *
     * @param id período a eliminar
     * @return 204 si se eliminó, 404 si no existe, o 400 si hay estudiantes o convocatorias
     *         asociadas
     */
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
