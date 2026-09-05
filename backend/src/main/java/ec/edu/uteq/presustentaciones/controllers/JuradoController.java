package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.services.JuradoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/jurados")
public class JuradoController {

    private final JuradoService juradoService;

    public JuradoController(JuradoService juradoService) {
        this.juradoService = juradoService;
    }

    // ── Jurados ───────────────────────────────────────────────────────────────

    /**
     * Asigna manualmente un docente como jurado de una solicitud, con un rol concreto.
     *
     * @param solicitudId solicitud a la que se asigna el tribunal
     * @param docenteId   docente que actuará como jurado
     * @param rol         código de rol en el tribunal (PRESIDENTE, VOCAL_1, VOCAL_2)
     * @return 200 con el {@link Jurado} asignado, o 400 con el motivo si el servicio lo
     *         rechaza (docente ya asignado, rol inexistente, etc.)
     */
    @PostMapping("/asignar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarJurado(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId,
            @RequestParam String rol) {
        try {
            Jurado j = juradoService.asignarJurado(solicitudId, docenteId, rol);
            return ResponseEntity.ok(ResponseWrapper.success(j, "Jurado asignado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * Asigna automáticamente los tres jurados del tribunal repartiendo carga entre los
     * docentes disponibles.
     *
     * @param solicitudId solicitud a la que se asigna el tribunal
     * @return 200 con la lista de jurados resultante, o 400 con el motivo si no hay
     *         suficientes docentes disponibles
     */
    @PostMapping("/asignar-automatico/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarAutomaticamente(@PathVariable Long solicitudId) {
        try {
            juradoService.asignarJuradosAutomaticamente(solicitudId);
            List<Jurado> jurados = juradoService.listarPorSolicitud(solicitudId);
            return ResponseEntity.ok(ResponseWrapper.success(jurados, "Jurados asignados automáticamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param solicitudId solicitud consultada
     * @return 200 con los jurados asignados a esa solicitud
     */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> listarPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarPorSolicitud(solicitudId)));
    }

    /**
     * @param pageable página y tamaño solicitados
     * @return 200 con la página de todas las asignaciones de tribunal
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> listarTodos(Pageable pageable) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarTodos(pageable)));
    }

    /**
     * Retira a un docente del tribunal.
     *
     * @param juradoId asignación de jurado a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/{juradoId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Void> eliminarJurado(@PathVariable Long juradoId) {
        juradoService.eliminarJurado(juradoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sugiere docentes candidatos para completar el tribunal, excluyendo a los ya asignados.
     *
     * @param solicitudId solicitud para la que se buscan candidatos
     * @param cantidad    número máximo de sugerencias (5 por defecto)
     * @return 200 con la lista de docentes sugeridos
     */
    @GetMapping("/sugerencias/{solicitudId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> sugerirDocentes(
            @PathVariable Long solicitudId,
            @RequestParam(defaultValue = "5") int cantidad) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.sugerirDocentes(solicitudId, cantidad)));
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    /**
     * Asigna un docente como tutor de la solicitud.
     *
     * @param solicitudId solicitud a tutorar
     * @param docenteId   docente que asumirá la tutoría
     * @return 200 con el {@link Tutor} creado, o 400 con el motivo si ya tiene tutor
     */
    @PostMapping("/tutor/asignar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarTutor(
            @RequestParam Long solicitudId,
            @RequestParam Long docenteId) {
        try {
            Tutor t = juradoService.asignarTutor(solicitudId, docenteId);
            return ResponseEntity.ok(ResponseWrapper.success(t, "Tutor asignado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * @param solicitudId solicitud consultada
     * @return 200 con el tutor activo, o 404 si la solicitud no tiene tutor asignado
     */
    @GetMapping("/tutor/solicitud/{solicitudId}")
    public ResponseEntity<?> obtenerTutor(@PathVariable Long solicitudId) {
        return juradoService.obtenerTutorDeSolicitud(solicitudId)
                .map(t -> ResponseEntity.ok(ResponseWrapper.success(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retira la tutoría asignada.
     *
     * @param tutorId tutoría a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/tutor/{tutorId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<Void> eliminarTutor(@PathVariable Long tutorId) {
        juradoService.eliminarTutor(tutorId);
        return ResponseEntity.noContent().build();
    }

    // ── Vistas del docente como jurado ────────────────────────────────────────

    /**
     * @param docenteId docente consultado
     * @return 200 con todas las solicitudes en las que ese docente es jurado
     */
    @GetMapping("/docente/{docenteId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR') or @permisoService.esPropioDocente(authentication, #docenteId)")
    public ResponseEntity<?> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarPorDocente(docenteId)));
    }

    /**
     * @param docenteId docente consultado
     * @return 200 con todas las tutorías a cargo de ese docente
     */
    @GetMapping("/tutor/docente/{docenteId}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR') or @permisoService.esPropioDocente(authentication, #docenteId)")
    public ResponseEntity<?> listarTutoriasPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(ResponseWrapper.success(juradoService.listarTutoriasPorDocente(docenteId)));
    }

    /**
     * Datos del jurado que un usuario concreto ocupa en una solicitud, usados por la
     * pantalla de calificación para saber con qué rol firma quien está viendo la página.
     *
     * @param solicitudId solicitud consultada
     * @param usuarioId   usuario del que se quiere conocer su rol en ese tribunal
     * @return 200 con id, rol, confirmación y nombre del docente; o 200 con datos nulos
     *         si ese usuario no es jurado de la solicitud
     */
    @GetMapping("/info/{solicitudId}/{usuarioId}")
    public ResponseEntity<?> obtenerInfoJurado(@PathVariable Long solicitudId, @PathVariable Long usuarioId) {
        Optional<Jurado> juradoOpt = juradoService.obtenerInfoJurado(solicitudId, usuarioId);
        if (juradoOpt.isPresent()) {
            Jurado jurado = juradoOpt.get();
            String nombreDocente = "";
            if (jurado.getDocente() != null && jurado.getDocente().getUsuario() != null) {
                nombreDocente = jurado.getDocente().getUsuario().getNombre() + " " 
                        + jurado.getDocente().getUsuario().getApellido();
            }
            return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                    "id", jurado.getId(),
                    "rol", jurado.getRol() != null ? jurado.getRol() : "",
                    "confirmado", jurado.isConfirmado(),
                    "nombreDocente", nombreDocente
            )));
        }
        return ResponseEntity.ok(ResponseWrapper.success(null));
    }

    /**
     * SP (Fase 3): Asignación masiva de jurados por rol.
     * Llama a presus.sp_asignar_jurado_masivo(p_solicitud_ids, p_docente_ids, p_rol)
     * Flujo: POST → JuradoController → JuradoService → JuradoRepository → SP → PostgreSQL
     *
     * Body esperado: { "solicitudIds": [1,2,3], "docenteIds": [4,5,6], "rol": "PRESIDENTE" }
     *
     * @param body mapa con solicitudIds, docenteIds y rol; los ids llegan como enteros JSON
     *             y se convierten a Long para el procedimiento
     * @return 200 con el número de asignaciones ejecutadas y el rol aplicado; 400 si falta
     *         alguna de las tres claves o si el procedimiento rechaza el lote (en cuyo caso
     *         la transacción revierte el lote completo)
     */
    @PostMapping("/asignar-masivo")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'TRIBUNAL_TUTOR_ASIGNAR')")
    public ResponseEntity<?> asignarMasivo(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> solicitudIdsList = (List<Integer>) body.get("solicitudIds");
            @SuppressWarnings("unchecked")
            List<Integer> docenteIdsList   = (List<Integer>) body.get("docenteIds");
            String rol = (String) body.get("rol");

            if (solicitudIdsList == null || docenteIdsList == null || rol == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Se requieren 'solicitudIds', 'docenteIds' y 'rol'"));
            }
            Long[] solicitudIds = solicitudIdsList.stream().map(i -> i.longValue()).toArray(Long[]::new);
            Long[] docenteIds   = docenteIdsList.stream().map(i -> i.longValue()).toArray(Long[]::new);

            juradoService.asignarJuradoMasivoSP(solicitudIds, docenteIds, rol);
            return ResponseEntity.ok(ResponseWrapper.success(Map.of(
                    "mensaje", "Asignación masiva ejecutada correctamente",
                    "asignados", solicitudIds.length,
                    "rol", rol
            ), "Asignación masiva ejecutada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }
}
