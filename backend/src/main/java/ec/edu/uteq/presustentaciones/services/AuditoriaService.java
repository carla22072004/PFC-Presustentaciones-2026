package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Identifica al "quién" para los triggers de auditoría (V15__auditoria.sql). Se llama
 * al inicio de cada método @Transactional que hace una escritura auditable, ANTES del
 * save()/delete() que dispara el trigger -- set_config(..., true) fija el GUC solo para
 * la transacción/conexión actual, así que tiene que correr en la misma transacción que
 * la escritura para que el trigger lo vea (por eso no se puede hacer una sola vez por
 * request en un filtro: el filtro corre antes de que Spring abra la transacción).
 *
 * IMPORTANTE -- debe ser literalmente la PRIMERA operación del método, antes de mutar
 * cualquier entidad ya gestionada (ej. antes de solicitud.setEstado(...)): esta llamada
 * ejecuta una query nativa, y Hibernate hace auto-flush de los cambios pendientes ANTES
 * de correr cualquier query nativa (no puede saber si esa query depende de ellos). Si el
 * set_config corre después de modificar la entidad, el flush -- y el trigger -- ya
 * dispararon con el GUC todavía sin fijar, y la fila de auditoría queda sin autor.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public void marcarActorActual() {
        Long usuarioId = resolverUsuarioIdActual();
        String valor = usuarioId != null ? usuarioId.toString() : "";
        try {
            entityManager.createNativeQuery("SELECT set_config('presus.usuario_actual', :valor, true)")
                    .setParameter("valor", valor)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("No se pudo fijar el actor de auditoría: {}", e.getMessage());
        }
    }

    private Long resolverUsuarioIdActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                return null;
            }
            return usuarioRepository.findByEmail(auth.getName()).map(u -> u.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
