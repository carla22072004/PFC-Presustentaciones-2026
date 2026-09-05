package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarTemaPropuestoRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Centro de Orientación y Titulación — catálogo de temas propuestos y la lista de
 * temas que cada estudiante guarda para sí mismo.
 *
 * Autorización: la exploración del catálogo usa el permiso dinámico
 * {@code ORIENTACION_TEMAS_VER} (gestionable desde "Gestionar Permisos", igual que
 * el resto del sistema — ver PermisoService). Las acciones sobre la lista personal
 * (guardar / quitar / listar guardados) son exclusivas del rol ESTUDIANTE y operan
 * SIEMPRE sobre el estudiante autenticado: el id se resuelve desde el JWT, nunca se
 * recibe por la URL (evita IDOR).
 */
@RestController
@RequestMapping("/api/v1/orientacion/temas")
@RequiredArgsConstructor
public class TemaController {

    private final TemaService temaService;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;

    // ── Catálogo (cualquier usuario con permiso ORIENTACION_TEMAS_VER) ────────

    /**
     * Explora el catalogo de temas propuestos con filtros combinables. Si quien consulta es
     * estudiante, el resultado marca ademas cuales tiene ya guardados.
     *
     * @param carreraId            filtra por carrera, opcional
     * @param lineaInvestigacionId filtra por linea de investigacion, opcional
     * @param areaId               filtra por area tematica, opcional
     * @param nivelDificultad      filtra por nivel (BASICO, INTERMEDIO, AVANZADO), opcional
     * @return 200 con los temas que cumplen los filtros
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_TEMAS_VER')")
    public ResponseEntity<List<TemaPropuestoDTO>> explorar(
            @RequestParam(required = false) Integer carreraId,
            @RequestParam(required = false) Integer lineaInvestigacionId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) String nivelDificultad) {
        Long estudianteId = estudianteActualIdOrNull();
        return ResponseEntity.ok(temaService.explorar(
                carreraId, lineaInvestigacionId, areaId, nivelDificultad, estudianteId));
    }

    /**
     * @param temaId tema consultado
     * @return 200 con el detalle del tema
     */
    @GetMapping("/{temaId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_TEMAS_VER')")
    public ResponseEntity<TemaPropuestoDTO> detalle(@PathVariable Integer temaId) {
        return ResponseEntity.ok(temaService.obtenerDetalle(temaId));
    }

    /**
     * Sugiere ideas de tema a partir de la carrera, linea y area que indique el estudiante.
     *
     * @param request criterios sobre los que generar las sugerencias
     * @return 200 con las ideas propuestas
     */
    @PostMapping("/generar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_TEMAS_VER')")
    public ResponseEntity<List<TemaPropuestoDTO>> generarIdeas(@RequestBody @Valid GenerarTemaRequest request) {
        return ResponseEntity.ok(temaService.generarIdeas(request));
    }

    // ── Lista personal del estudiante (solo ESTUDIANTE, siempre sobre sí mismo) ─

    /**
     * Lista personal de temas guardados. Opera siempre sobre el estudiante autenticado; el id
     * sale del JWT y nunca de la URL.
     *
     * @return 200 con los temas que el estudiante autenticado guardo
     */
    @GetMapping("/guardados")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<TemaPropuestoDTO>> misTemasGuardados() {
        return ResponseEntity.ok(temaService.obtenerTemasGuardados(estudianteActual().getId()));
    }

    /**
     * Guarda un tema en la lista personal del estudiante autenticado.
     *
     * @param temaId tema a guardar
     * @return 200 sin cuerpo
     */
    @PostMapping("/{temaId}/guardar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> guardar(@PathVariable Integer temaId) {
        temaService.guardarTemaEstudiante(estudianteActual().getId(), temaId);
        return ResponseEntity.status(201).build();
    }

    /**
     * Quita un tema de la lista personal del estudiante autenticado.
     *
     * @param temaId tema a quitar
     * @return 200 sin cuerpo
     */
    @DeleteMapping("/{temaId}/guardar")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<Void> quitarGuardado(@PathVariable Integer temaId) {
        temaService.quitarTemaGuardado(estudianteActual().getId(), temaId);
        return ResponseEntity.noContent().build();
    }

    // ── Gestión del catálogo (permiso ORIENTACION_CATALOGO_GESTIONAR) ────────

    /**
     * Publica un tema nuevo en el catalogo (gestion, no lista personal).
     *
     * @param request datos del tema, validados con Bean Validation
     * @return 200 con el tema creado
     */
    @PostMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<TemaPropuestoDTO> crear(@RequestBody @Valid GuardarTemaPropuestoRequest request) {
        return ResponseEntity.status(201).body(temaService.crear(request));
    }

    /**
     * Edita un tema del catalogo.
     *
     * @param temaId  tema a actualizar
     * @param request nuevos datos del tema
     * @return 200 con el tema actualizado
     */
    @PutMapping("/{temaId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<TemaPropuestoDTO> actualizar(@PathVariable Integer temaId,
                                                       @RequestBody @Valid GuardarTemaPropuestoRequest request) {
        return ResponseEntity.ok(temaService.actualizar(temaId, request));
    }

    /**
     * Retira un tema del catalogo.
     *
     * @param temaId tema a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/{temaId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer temaId) {
        temaService.eliminar(temaId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers de identidad ─────────────────────────────────────────────────

    /**
     * Resuelve el usuario de la sesion actual a partir del token.
     *
     * @return el usuario autenticado
     * @throws IllegalStateException si no hay sesion, es anonima, o el usuario del token ya
     *                               no existe en la base
     */
    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado en el sistema"));
    }

    /**
     * Resuelve el perfil de estudiante del usuario autenticado.
     *
     * @return el estudiante autenticado
     * @throws IllegalArgumentException si el usuario autenticado no tiene perfil de estudiante
     */
    private Estudiante estudianteActual() {
        return estudianteRepository.findByUsuarioId(usuarioActual().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no tiene un perfil de estudiante asociado"));
    }

    /**
     * Variante tolerante de {@link #estudianteActual()} para el catalogo publico: permite que
     * un docente o coordinador explore los temas sin perfil de estudiante asociado.
     *
     * @return id del estudiante autenticado, o null si quien consulta no es estudiante
     */
    private Long estudianteActualIdOrNull() {
        try {
            return estudianteRepository.findByUsuarioId(usuarioActual().getId())
                    .map(Estudiante::getId)
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
