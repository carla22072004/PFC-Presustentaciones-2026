package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Sala;
import ec.edu.uteq.presustentaciones.repositories.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/salas")
@RequiredArgsConstructor
public class SalaController {
    private final SalaRepository salaRepository;

    /**
     * @return todas las salas registradas
     */
    @GetMapping
    public List<Sala> listar() { return salaRepository.findAll(); }

    /**
     * Versión paginada, misma convención que /api/v1/solicitudes/paginado y
     * /api/v1/usuarios/paginado.
     *
     * @param pageable página y tamaño solicitados
     * @return página de salas
     */
    @GetMapping("/paginado")
    public Page<Sala> listarPaginado(Pageable pageable) {
        return salaRepository.findAll(pageable);
    }

    /**
     * Registra una sala nueva para programar defensas.
     *
     * @param sala datos de la sala (código, nombre, capacidad, disponibilidad)
     * @return la sala persistida, con su id asignado
     */
    @PostMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SALA_GESTIONAR')")
    public Sala crear(@RequestBody Sala sala) { return salaRepository.save(sala); }

    /**
     * Elimina una sala del catálogo.
     *
     * @param id sala a eliminar
     * @return 204 sin cuerpo
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'SALA_GESTIONAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        salaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
