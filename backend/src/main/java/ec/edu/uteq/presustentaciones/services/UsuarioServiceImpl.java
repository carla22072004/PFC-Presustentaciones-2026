package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    /** rol (string) y rolUsuario (FK a roles_usuario) son dos columnas paralelas para el mismo dato — hay que mantenerlas sincronizadas. */
    private RolUsuario resolverRol(String codigoRol) {
        if (codigoRol == null || codigoRol.trim().isEmpty()) {
            throw new IllegalArgumentException("El rol del usuario es requerido");
        }
        return rolUsuarioRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol inválido o no existe: " + codigoRol));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Usuario> listarPaginado(int page, int size, String q) {
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), 100);
        return usuarioRepository.buscarPaginado(q, PageRequest.of(paginaSegura, tamanioSeguro));
    }

    @Override
    public Usuario crear(Usuario usuario) {
        log.info("Creando usuario con email: {}", usuario.getEmail());

        if (existePorEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + usuario.getEmail());
        }

        auditoriaService.marcarActorActual();
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRolUsuario(resolverRol(usuario.getRol()));
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario actualizar(Long id, Usuario usuario) {
        log.info("Actualizando usuario con ID: {}", id);

        auditoriaService.marcarActorActual();
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        existente.setNombre(usuario.getNombre());
        existente.setApellido(usuario.getApellido());
        existente.setEmail(usuario.getEmail());
        if (usuario.getRol() != null && !usuario.getRol().equals(existente.getRol())) {
            existente.setRol(usuario.getRol());
            existente.setRolUsuario(resolverRol(usuario.getRol()));
        }
        if (usuario.getTelefono() != null) {
            existente.setTelefono(usuario.getTelefono());
        }

        return usuarioRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando usuario con ID: {}", id);

        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
        }

        auditoriaService.marcarActorActual();
        usuarioRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    public void activar(Long id) {
        auditoriaService.marcarActorActual();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    @Override
    public void desactivar(Long id) {
        auditoriaService.marcarActorActual();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizarPerfil(Long id, String emailNotificaciones, String telefono) {
        int updated = usuarioRepository.actualizarPerfil(id, emailNotificaciones, telefono);
        if (updated == 0) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
}
