package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.PerfilRequest;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.services.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * La gestión de usuarios (listar/crear/editar rol/activar/eliminar) es exclusiva de ADMIN.
 * Cada usuario autenticado solo puede ver/editar su propio perfil vía {@code /{id}} y {@code /{id}/perfil}.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
@Slf4j
@Tag(name = "Usuarios", description = "API para gestión de usuarios del sistema")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Listar todos los usuarios (solo ADMIN) — sin paginar, uso interno/pequeñas instalaciones")
    public ResponseEntity<List<Usuario>> listarTodos() {
        log.info("GET /api/usuarios - Listando todos los usuarios");
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /** La tabla puede tener decenas de miles de filas (datos de carga k6) — el panel de admin siempre usa este endpoint paginado. */
    @GetMapping("/paginado")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Listar usuarios paginado, con búsqueda opcional (solo ADMIN)")
    public ResponseEntity<?> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        var resultado = usuarioService.listarPaginado(page, size, q);
        return ResponseEntity.ok(java.util.Map.of(
                "content", resultado.getContent(),
                "totalElements", resultado.getTotalElements(),
                "totalPages", resultado.getTotalPages(),
                "page", resultado.getNumber(),
                "size", resultado.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID (propio usuario o ADMIN)")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        if (!esUsuarioActualOAdmin(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "No tienes permiso para ver este usuario"));
        }
        log.info("GET /api/usuarios/{} - Obteniendo usuario", id);
        return usuarioService.obtenerPorId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Buscar usuario por email (solo ADMIN)")
    public ResponseEntity<Usuario> buscarPorEmail(@PathVariable String email) {
        log.info("GET /api/usuarios/email/{} - Buscando usuario", email);
        return usuarioService.obtenerPorEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activos")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Listar usuarios activos (solo ADMIN)")
    public ResponseEntity<List<Usuario>> listarActivos() {
        log.info("GET /api/usuarios/activos - Listando usuarios activos");
        return ResponseEntity.ok(usuarioService.listarActivos());
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Crear nuevo usuario (solo ADMIN)")
    public ResponseEntity<Usuario> crear(@RequestBody Usuario usuario) {
        log.info("POST /api/usuarios - Creando usuario: {}", usuario.getEmail());
        Usuario creado = usuarioService.crear(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Actualizar usuario, incluido su rol (solo ADMIN)")
    public ResponseEntity<Usuario> actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario
    ) {
        log.info("PUT /api/usuarios/{} - Actualizando usuario", id);
        Usuario actualizado = usuarioService.actualizar(id, usuario);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Activar usuario (solo ADMIN)")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        log.info("PATCH /api/usuarios/{}/activar", id);
        usuarioService.activar(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Desactivar usuario (solo ADMIN)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        log.info("PATCH /api/usuarios/{}/desactivar", id);
        usuarioService.desactivar(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/perfil")
    @Operation(summary = "Actualizar correo de notificaciones y teléfono del perfil propio")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody PerfilRequest req
    ) {
        if (!esUsuarioActual(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "No puedes editar el perfil de otro usuario"));
        }
        try {
            Usuario actualizado = usuarioService.actualizarPerfil(id, req.getEmailNotificaciones(), req.getTelefono());
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Eliminar usuario (solo ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/usuarios/{}", id);
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** El id del path coincide con el usuario autenticado (resuelto por email del JWT, no por el id recibido). */
    private boolean esUsuarioActual(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByEmail(auth.getName())
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }

    private boolean esUsuarioActualOAdmin(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return esAdmin || esUsuarioActual(id);
    }
}