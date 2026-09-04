package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
import ec.edu.uteq.presustentaciones.security.service.UsuarioActualService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/docentes")
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class DocenteController {

    private final DocenteRepository docenteRepository;
    private final UsuarioActualService usuarioActualService;

    public DocenteController(DocenteRepository docenteRepository, UsuarioActualService usuarioActualService) {
        this.docenteRepository = docenteRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @GetMapping
    public List<Docente> listar() {
        return docenteRepository.findAll();
    }

    /**
     * Versión paginada -- misma convención que /api/v1/solicitudes/paginado y
     * /api/v1/usuarios/paginado. ERR-02: agrega búsqueda de texto libre ("q") para
     * alimentar un combobox con typeahead en vez de listar los 9,807 docentes de una vez.
     */
    @GetMapping("/paginado")
    public Page<Docente> listarPaginado(@RequestParam(required = false) String q, Pageable pageable) {
        return docenteRepository.buscarPaginado(q, pageable);
    }

    @GetMapping("/disponibles")
    public List<Docente> disponibles() {
        return docenteRepository.findByDisponibleTrue();
    }

    /**
     * Directorio interno por id de Docente (no de Usuario): mismo dato ya expuesto en bloque
     * por {@code /}, {@code /paginado} y {@code /disponibles} (selección de jurado/tutor,
     * asignación de sala, etc.), así que no aplica un control de propiedad aquí -- restringirlo
     * rompería esos flujos sin cerrar ninguna fuga real, ya que el mismo docente ya es visible
     * listando todos.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Docente> obtener(@PathVariable Long id) {
        return docenteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * A diferencia de {@code /{id}}, este endpoint resuelve el perfil de Docente a partir de un
     * usuarioId -- pensado para que un docente autenticado consulte su propio perfil (así lo usa
     * el frontend: firmar-acta-docente y mis-asignaciones siempre pasan authService.getUserId()).
     * Sin control de propiedad, cualquier autenticado podía enumerar usuarioId ajenos. ADMIN y
     * COORDINADOR conservan acceso completo (mismo criterio administrativo que el resto del
     * sistema); cualquier otro usuario solo puede consultar su propio usuarioId.
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Docente> obtenerPorUsuario(@PathVariable Long usuarioId) {
        validarAccesoPropioOAdministrativo(usuarioId);
        return docenteRepository.findByUsuarioId(usuarioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private void validarAccesoPropioOAdministrativo(Long usuarioIdObjetivo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdminOCoordinador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_COORDINADOR"));
        if (esAdminOCoordinador) {
            return;
        }
        Long usuarioActualId = usuarioActualService.usuario().getId();
        if (!usuarioActualId.equals(usuarioIdObjetivo)) {
            throw new AccessDeniedException("No tienes permiso para consultar el perfil de otro docente");
        }
    }
}
//