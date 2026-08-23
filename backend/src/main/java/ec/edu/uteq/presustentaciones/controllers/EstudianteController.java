package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ActualizarEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.CrearEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.EstudianteDTO;
import ec.edu.uteq.presustentaciones.entities.EstadoAcademico;
import ec.edu.uteq.presustentaciones.services.EstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Gestión de estudiantes: registrar (usuario + perfil académico) y editar carrera,
 * semestre, período de ingreso y estado académico. */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'ESTUDIANTES_GESTIONAR')")
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping("/paginado")
    public ResponseEntity<Page<EstudianteDTO>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(estudianteService.listarPaginado(page, size, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudianteService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearEstudianteRequest req) {
        try {
            return ResponseEntity.ok(estudianteService.crear(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ActualizarEstudianteRequest req) {
        try {
            return ResponseEntity.ok(estudianteService.actualizar(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estados-academicos")
    public List<EstadoAcademico> estadosAcademicos() {
        return estudianteService.listarEstadosAcademicos();
    }
}
