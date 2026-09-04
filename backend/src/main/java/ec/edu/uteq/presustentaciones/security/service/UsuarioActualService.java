package ec.edu.uteq.presustentaciones.security.service;

import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resuelve el usuario / estudiante autenticado a partir del SecurityContext.
 * Centraliza el patrón que ya usaban TutoriaController y TemaController para
 * evitar IDOR: nunca se confía en un id que venga por la URL para operaciones
 * "sobre mí mismo".
 */
@Service
@RequiredArgsConstructor
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;

    public Usuario usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado en el sistema"));
    }

    public Estudiante estudiante() {
        return estudianteRepository.findByUsuarioId(usuario().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no tiene un perfil de estudiante asociado"));
    }

    /** Id del estudiante autenticado, o null si quien consulta no es estudiante. */
    public Long estudianteIdOrNull() {
        try {
            return estudianteRepository.findByUsuarioId(usuario().getId())
                    .map(Estudiante::getId)
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
