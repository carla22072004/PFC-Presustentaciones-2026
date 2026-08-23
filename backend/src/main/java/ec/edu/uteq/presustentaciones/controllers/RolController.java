package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.RolDTO;
import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD de roles del sistema. La asignación de permisos a cada rol vive en
 * PermisoController -- aquí solo se administra el catálogo de roles en sí
 * (codigo, nombre) y su eliminación.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("@permisoService.tienePermiso(authentication, 'ROLES_PERMISOS_GESTIONAR')")
public class RolController {

    private final RolUsuarioRepository rolUsuarioRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    /** Los 4 roles con los que arranca el sistema -- no se pueden eliminar ni renombrar
     * el código porque el resto de la aplicación (frontend roleGuard, Usuario.rol,
     * flujos de negocio) todavía distingue casos por estos 4 nombres exactos. Roles
     * nuevos que se creen desde aquí sí se pueden eliminar libremente. */
    private static final Set<String> ROLES_PROTEGIDOS = Set.of("ADMIN", "DOCENTE", "COORDINADOR", "ESTUDIANTE");

    @GetMapping
    public List<RolDTO> listar() {
        return rolUsuarioRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        String codigo = body.getOrDefault("codigo", "").trim().toUpperCase().replaceAll("\\s+", "_");
        String nombre = body.getOrDefault("nombre", "").trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código y nombre son obligatorios."));
        }
        if (rolUsuarioRepository.findByCodigo(codigo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un rol con ese código."));
        }
        short siguienteId = (short) (rolUsuarioRepository.findAll().stream()
                .mapToInt(RolUsuario::getId).max().orElse(0) + 1);
        auditoriaService.marcarActorActual();
        RolUsuario rol = rolUsuarioRepository.save(
                RolUsuario.builder().id(siguienteId).codigo(codigo).nombre(nombre).build());
        return ResponseEntity.ok(toDto(rol));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> renombrar(@PathVariable Short id, @RequestBody Map<String, String> body) {
        auditoriaService.marcarActorActual();
        RolUsuario rol = rolUsuarioRepository.findById(id).orElse(null);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        String nombre = body.getOrDefault("nombre", "").trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre no puede estar vacío."));
        }
        // El código (usado por @PreAuthorize y por el rol legado en Usuario.rol) no se
        // renombra para no dejar huérfanas las referencias existentes -- solo el nombre visible.
        rol.setNombre(nombre);
        return ResponseEntity.ok(toDto(rolUsuarioRepository.save(rol)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Short id) {
        RolUsuario rol = rolUsuarioRepository.findById(id).orElse(null);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        if (ROLES_PROTEGIDOS.contains(rol.getCodigo())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "El rol " + rol.getCodigo() + " es uno de los 4 roles base del sistema y no se puede eliminar."));
        }
        long usuariosConEsteRol = usuarioRepository.findByRol(rol.getCodigo()).size();
        if (usuariosConEsteRol > 0) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "No se puede eliminar: hay " + usuariosConEsteRol + " usuario(s) con este rol asignado."));
        }
        try {
            auditoriaService.marcarActorActual();
            rolUsuarioRepository.delete(rol);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar el rol: tiene referencias asociadas."));
        }
    }

    private RolDTO toDto(RolUsuario rol) {
        return RolDTO.builder()
                .id(rol.getId())
                .codigo(rol.getCodigo())
                .nombre(rol.getNombre())
                .usuariosAsignados(usuarioRepository.findByRol(rol.getCodigo()).size())
                .permisos(permisoRepository.findCodigosPorRol(rol.getId()))
                .build();
    }
}
