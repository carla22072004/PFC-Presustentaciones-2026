package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.PerfilRequest;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.security.dto.RegisterRequest;
import ec.edu.uteq.presustentaciones.services.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> listarTodos() {
        log.info("GET /api/usuarios - Listando todos los usuarios");
        try {
            return ResponseEntity.ok(ResponseWrapper.success(usuarioService.listarTodos()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
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
        try {
            var resultado = usuarioService.listarPaginado(page, size, q);
            return ResponseEntity.ok(ResponseWrapper.success(java.util.Map.of(
                    "content", resultado.getContent(),
                    "totalElements", resultado.getTotalElements(),
                    "totalPages", resultado.getTotalPages(),
                    "page", resultado.getNumber(),
                    "size", resultado.getSize()
            )));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID (propio usuario o ADMIN)")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        if (!esUsuarioActualOAdmin(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseWrapper.error("No tienes permiso para ver este usuario"));
        }
        log.info("GET /api/usuarios/{} - Obteniendo usuario", id);
        return usuarioService.obtenerPorId(id)
                .<ResponseEntity<?>>map(usuario -> ResponseEntity.ok(ResponseWrapper.success(usuario)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseWrapper.error("Usuario no encontrado")));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Buscar usuario por email (solo ADMIN)")
    public ResponseEntity<?> buscarPorEmail(@PathVariable String email) {
        log.info("GET /api/usuarios/email/{} - Buscando usuario", email);
        return usuarioService.obtenerPorEmail(email)
                .<ResponseEntity<?>>map(usuario -> ResponseEntity.ok(ResponseWrapper.success(usuario)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseWrapper.error("Usuario no encontrado")));
    }

    @GetMapping("/activos")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Listar usuarios activos (solo ADMIN)")
    public ResponseEntity<?> listarActivos() {
        log.info("GET /api/usuarios/activos - Listando usuarios activos");
        try {
            return ResponseEntity.ok(ResponseWrapper.success(usuarioService.listarActivos()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    /**
     * Hallazgo real de auditoría (2026-09-04), mismo criterio que AuthController.register():
     * recibir la entidad Usuario cruda permitía mass-assignment de "activo"/"rolUsuario"/"id" —
     * este último es el más grave, porque un "id" de un usuario ya existente hacía que
     * UsuarioServiceImpl.crear() sobrescribiera esa fila en vez de crear una nueva (ver su
     * comentario). Se usa RegisterRequest (mismo DTO, mismos 5 campos que ya envía
     * gestion-usuarios.component.ts) en vez de crear un DTO nuevo.
     */
    @PostMapping
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Crear nuevo usuario (solo ADMIN)")
    public ResponseEntity<?> crear(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/usuarios - Creando usuario: {}", request.getEmail());
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setRol(request.getRol());
        usuario.setActivo(true);
        try {
            Usuario creado = usuarioService.crear(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.success(creado, "Usuario creado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Actualizar usuario, incluido su rol (solo ADMIN)")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario
    ) {
        log.info("PUT /api/usuarios/{} - Actualizando usuario", id);
        try {
            Usuario actualizado = usuarioService.actualizar(id, usuario);
            return ResponseEntity.ok(ResponseWrapper.success(actualizado, "Usuario actualizado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Activar usuario (solo ADMIN)")
    public ResponseEntity<?> activar(@PathVariable Long id) {
        log.info("PATCH /api/usuarios/{}/activar", id);
        try {
            usuarioService.activar(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Usuario activado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Desactivar usuario (solo ADMIN)")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        log.info("PATCH /api/usuarios/{}/desactivar", id);
        try {
            usuarioService.desactivar(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Usuario desactivado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/perfil")
    @Operation(summary = "Actualizar correo de notificaciones y teléfono del perfil propio")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody PerfilRequest req
    ) {
        if (!esUsuarioActual(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseWrapper.error("No puedes editar el perfil de otro usuario"));
        }
        try {
            Usuario actualizado = usuarioService.actualizarPerfil(id, req.getEmailNotificaciones(), req.getTelefono());
            return ResponseEntity.ok(ResponseWrapper.success(actualizado, "Perfil actualizado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermiso(authentication, 'USUARIOS_GESTIONAR')")
    @Operation(summary = "Eliminar usuario (solo ADMIN)")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/usuarios/{}", id);
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Usuario eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error(e.getMessage()));
        }
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