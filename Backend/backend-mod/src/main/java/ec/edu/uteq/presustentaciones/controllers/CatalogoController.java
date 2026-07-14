package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.ConvocatoriaTitulacion;
import ec.edu.uteq.presustentaciones.entities.ModalidadTitulacion;
import ec.edu.uteq.presustentaciones.repositories.ConvocatoriaTitulacionRepository;
import ec.edu.uteq.presustentaciones.repositories.ModalidadTitulacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final ModalidadTitulacionRepository modalidadRepo;
    private final ConvocatoriaTitulacionRepository convocatoriaRepo;

    /** Lista todas las modalidades de titulación disponibles */
    @GetMapping("/modalidades")
    public ResponseEntity<List<ModalidadTitulacion>> listarModalidades() {
        return ResponseEntity.ok(modalidadRepo.findAll());
    }

    /** Lista todas las convocatorias activas */
    @GetMapping("/convocatorias")
    public ResponseEntity<List<ConvocatoriaTitulacion>> listarConvocatoriasActivas() {
        return ResponseEntity.ok(convocatoriaRepo.findByActivaTrue());
    }

    /** Retorna la convocatoria activa actual (para autocompletar en el formulario) */
    @GetMapping("/convocatoria-activa")
    public ResponseEntity<?> convocatoriaActiva() {
        return convocatoriaRepo.findFirstByActivaTrue()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of("error", "No hay convocatoria activa")));
    }
}
