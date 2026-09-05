package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Auditoria;
import ec.edu.uteq.presustentaciones.repositories.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Consulta del historial de auditoría (ver V15__auditoria.sql) -- solo lectura, los triggers escriben. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'AUDITORIA_VER')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    /**
     * Historial de auditoría paginado y filtrable. El tamaño de página se acota entre 1 y
     * 100 para que un cliente no pueda pedir la tabla completa de una vez, y el orden es
     * siempre por fecha descendente.
     *
     * @param page      número de página (0 por defecto); los negativos se tratan como 0
     * @param size      filas por página (20 por defecto); se limita a un máximo de 100
     * @param tabla     filtra por tabla auditada, opcional
     * @param accion    filtra por acción (CREAR, MODIFICAR, ELIMINAR, ...), opcional
     * @param usuarioId filtra por el usuario que ejecutó el cambio, opcional
     * @param q         búsqueda de texto libre sobre el registro, opcional
     * @return 200 con la página de eventos de auditoría
     */
    @GetMapping("/paginado")
    public ResponseEntity<Page<Auditoria>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String q) {
        int tamanioSeguro = Math.min(Math.max(size, 1), 100);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0), tamanioSeguro, Sort.by(Sort.Direction.DESC, "fecha"));
        return ResponseEntity.ok(auditoriaRepository.buscarConFiltros(tabla, accion, usuarioId, q, pageable));
    }

    /**
     * Distintos valores de "tabla" ya registrados, para poblar el filtro del frontend sin
     * hardcodear la lista de tablas auditadas.
     *
     * @return nombres de tabla presentes hoy en el historial de auditoría
     */
    @GetMapping("/tablas")
    public java.util.List<String> tablasAuditadas() {
        return java.util.List.of("usuarios", "roles_usuario", "permisos", "rol_permisos", "estudiante", "solicitud", "actas", "evaluaciones_finales", "facultades", "carreras", "modalidades_titulacion", "periodos_academicos");
    }
}
