package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Sala;
import ec.edu.uteq.presustentaciones.repositories.SalaRepository;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public List<Sala> listar() { return salaRepository.findAll(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public Sala crear(@RequestBody Sala sala) { return salaRepository.save(sala); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        salaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
