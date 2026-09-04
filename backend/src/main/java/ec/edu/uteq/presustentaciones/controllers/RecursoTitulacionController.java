package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.GuardarRecursoRequest;
import ec.edu.uteq.presustentaciones.dto.RecursoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import ec.edu.uteq.presustentaciones.services.RecursoTitulacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Centro de Recursos de Titulación. La consulta está abierta a cualquier usuario
 * autenticado (si es estudiante, por defecto ve los recursos generales + los de su
 * carrera); la gestión requiere el permiso {@code ORIENTACION_CATALOGO_GESTIONAR}.
 */
@RestController
@RequestMapping("/api/v1/orientacion/recursos")
@RequiredArgsConstructor
public class RecursoTitulacionController {

    private final RecursoTitulacionService recursoService;
    private final UsuarioActualService usuarioActual;
    private final EstudianteRepository estudianteRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecursoTitulacionDTO>> listar(
            @RequestParam(required = false) Integer carreraId) {
        Integer efectivo = carreraId;
        if (efectivo == null) {
            Long estudianteId = usuarioActual.estudianteIdOrNull();
            if (estudianteId != null) {
                efectivo = estudianteRepository.findById(estudianteId)
                        .map(Estudiante::getCarreraEntidad)
                        .map(c -> c.getId())
                        .orElse(null);
            }
        }
        return ResponseEntity.ok(recursoService.listar(efectivo));
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<RecursoTitulacionDTO> crear(@RequestBody @Valid GuardarRecursoRequest request) {
        return ResponseEntity.status(201).body(recursoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<RecursoTitulacionDTO> actualizar(@PathVariable Integer id,
                                                           @RequestBody @Valid GuardarRecursoRequest request) {
        return ResponseEntity.ok(recursoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'ORIENTACION_CATALOGO_GESTIONAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        recursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
