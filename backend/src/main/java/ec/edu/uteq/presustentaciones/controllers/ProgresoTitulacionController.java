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

    /**
     * Ruta de titulación del estudiante autenticado. El id sale del token, no de la URL, así
     * que un estudiante nunca puede consultar el progreso de otro.
     *
     * @return 200 con el checklist de progreso del estudiante autenticado
     */
    @GetMapping
    public ResponseEntity<ProgresoTitulacionDTO> miProgreso() {
        return ResponseEntity.ok(progresoService.obtener(usuarioActual.estudiante().getId()));
    }

    /**
     * Actualiza el checklist del estudiante autenticado (marcar/desmarcar hitos).
     *
     * @param request hitos a actualizar, validado con Bean Validation
     * @return 200 con el progreso ya actualizado
     */
    @PutMapping
    public ResponseEntity<ProgresoTitulacionDTO> actualizar(@RequestBody @Valid ActualizarProgresoRequest request) {
        return ResponseEntity.ok(
                progresoService.actualizar(usuarioActual.estudiante().getId(), request.getPasos()));
    }
}
