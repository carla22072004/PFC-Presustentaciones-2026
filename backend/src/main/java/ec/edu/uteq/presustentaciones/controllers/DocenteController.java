package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/docentes")
public class DocenteController {

    private final DocenteRepository docenteRepository;

    public DocenteController(DocenteRepository docenteRepository) {
        this.docenteRepository = docenteRepository;
    }

    @GetMapping
    public List<Docente> listar() {
        return docenteRepository.findAll();
    }

    /** Versión paginada -- misma convención que /api/v1/solicitudes/paginado y /api/v1/usuarios/paginado. */
    @GetMapping("/paginado")
    public Page<Docente> listarPaginado(Pageable pageable) {
        return docenteRepository.findAll(pageable);
    }

    @GetMapping("/disponibles")
    public List<Docente> disponibles() {
        return docenteRepository.findByDisponibleTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Docente> obtener(@PathVariable Long id) {
        return docenteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Docente> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return docenteRepository.findByUsuarioId(usuarioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
//