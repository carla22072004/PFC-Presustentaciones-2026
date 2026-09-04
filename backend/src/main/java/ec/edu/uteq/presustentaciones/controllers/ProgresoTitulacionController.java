package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ActualizarProgresoRequest;
import ec.edu.uteq.presustentaciones.dto.ProgresoTitulacionDTO;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import ec.edu.uteq.presustentaciones.services.ProgresoTitulacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Ruta de titulación / checklist del estudiante. Exclusivo del rol ESTUDIANTE y
 * siempre sobre el estudiante autenticado (el id sale del JWT, no de la URL).
 */
@RestController
@RequestMapping("/api/v1/orientacion/progreso")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ESTUDIANTE')")
public class ProgresoTitulacionController {

    private final ProgresoTitulacionService progresoService;
    private final UsuarioActualService usuarioActual;

    @GetMapping
    public ResponseEntity<ProgresoTitulacionDTO> miProgreso() {
        return ResponseEntity.ok(progresoService.obtener(usuarioActual.estudiante().getId()));
    }

    @PutMapping
    public ResponseEntity<ProgresoTitulacionDTO> actualizar(@RequestBody @Valid ActualizarProgresoRequest request) {
        return ResponseEntity.ok(
                progresoService.actualizar(usuarioActual.estudiante().getId(), request.getPasos()));
    }
}
