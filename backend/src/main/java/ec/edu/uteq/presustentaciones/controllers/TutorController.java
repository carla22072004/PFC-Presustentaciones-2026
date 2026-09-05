package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.MiEstudianteTutoradoDTO;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.TutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/tutores")
public class TutorController {

    private final TutorService tutorService;
    private final UsuarioRepository usuarioRepository;

    public TutorController(TutorService tutorService, UsuarioRepository usuarioRepository) {
        this.tutorService = tutorService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * "Mis Estudiantes" (docente): roster de los estudiantes que el docente autenticado
     * tiene asignados como tutor. Sin permiso dedicado porque cualquier DOCENTE debe poder
     * consultar sus propios estudiantes (mismo criterio que /api/tutorias/docente/{id});
     * el usuario se resuelve desde el token, nunca desde un parámetro del cliente.
     *
     * @return 200 con el roster de estudiantes tutorados por el docente autenticado
     * @throws RuntimeException si el token es válido pero su usuario ya no existe en la base
     */
    @GetMapping("/mis-estudiantes")
    public ResponseEntity<List<MiEstudianteTutoradoDTO>> misEstudiantes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
        return ResponseEntity.ok(tutorService.misEstudiantes(usuario.getId()));
    }

    /**
     * Asigna un docente como tutor de una solicitud.
     *
     * @param solicitudId solicitud a tutorar
     * @param docenteId   docente que asumirá la tutoría
     * @return 200 con el {@link Tutor} creado, o 400 sin cuerpo si el servicio lo rechaza
     *         (por ejemplo, si la solicitud ya tiene tutor)
     */
    @PostMapping("/asignar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Tutor> asignar(@RequestParam Long solicitudId,
                                         @RequestParam Long docenteId) {
        try {
            return ResponseEntity.ok(tutorService.asignarTutor(solicitudId, docenteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * @param solicitudId solicitud consultada
     * @return 200 con el tutor asignado, o 404 si la solicitud aún no tiene tutor
     */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Tutor> porSolicitud(@PathVariable Long solicitudId) {
        return tutorService.buscarPorSolicitud(solicitudId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * @param pageable página y tamaño solicitados
     * @return 200 con la página de tutorías asignadas
     */
    @GetMapping
    public ResponseEntity<Page<Tutor>> listar(Pageable pageable) {
        return ResponseEntity.ok(tutorService.listarTodos(pageable));
    }

    /**
     * Retira la asignación de tutoría.
     *
     * @param id tutoría a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tutorService.eliminarTutor(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * SP (Fase 3): Estadísticas consolidadas del desempeño de tutores.
     * Llama a presus.sp_obtener_estadisticas_tutores().
     * Flujo: GET → TutorController → TutorService → TutorRepository → SP → PostgreSQL
     *
     * @return 200 con una fila por docente (id, nombre, tutorías activas, completadas y
     *         fases aprobadas), o 400 con el error si el procedimiento falla en la base
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'EVALUACION_RUBRICA_REGISTRAR')")
    public ResponseEntity<?> estadisticas() {
        try {
            List<Map<String, Object>> stats = tutorService.obtenerEstadisticasTutoresSP();
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
