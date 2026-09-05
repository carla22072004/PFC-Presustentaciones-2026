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

    /**
     * Listado paginado de estudiantes con búsqueda de texto libre.
     *
     * @param page número de página (0 por defecto)
     * @param size filas por página (20 por defecto)
     * @param q    búsqueda sobre nombre, apellido, correo o expediente, opcional
     * @return 200 con la página de estudiantes
     */
    @GetMapping("/paginado")
    public ResponseEntity<Page<EstudianteDTO>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(estudianteService.listarPaginado(page, size, q));
    }

    /**
     * @param id perfil de estudiante consultado
     * @return 200 con la ficha del estudiante, o el error correspondiente si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudianteService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Registra un estudiante creando de una vez su usuario autenticable y su perfil académico
     * (carrera, período de ingreso, semestre, expediente).
     *
     * @param req datos del usuario y del perfil académico
     * @return 200 con el estudiante creado, o el error de validación correspondiente
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearEstudianteRequest req) {
        try {
            return ResponseEntity.ok(estudianteService.crear(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Edita los datos académicos de un estudiante (carrera, semestre, estado académico).
     *
     * @param id  perfil de estudiante a actualizar
     * @param req campos a modificar
     * @return 200 con el estudiante actualizado, o el error correspondiente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ActualizarEstudianteRequest req) {
        try {
            return ResponseEntity.ok(estudianteService.actualizar(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * @return catálogo de estados académicos posibles, para poblar el selector del formulario
     */
    @GetMapping("/estados-academicos")
    public List<EstadoAcademico> estadosAcademicos() {
        return estudianteService.listarEstadosAcademicos();
    }
}
