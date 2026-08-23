package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("SELECT n FROM Notificacion n JOIN FETCH n.usuario WHERE n.usuario.id = :usuarioId ORDER BY n.fecha DESC")
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Long usuarioId);

    @Query(value = "SELECT n FROM Notificacion n JOIN FETCH n.usuario WHERE n.usuario.id = :usuarioId ORDER BY n.fecha DESC",
           countQuery = "SELECT COUNT(n) FROM Notificacion n WHERE n.usuario.id = :usuarioId")
    Page<Notificacion> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    /**
     * UPDATE en bloque en vez de traer + iterar + volver a guardar cada fila -- marcarTodasLeidas
     * antes cargaba TODAS las notificaciones del usuario (leídas incluidas) solo para reescribirlas.
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasLeidasPorUsuario(@Param("usuarioId") Long usuarioId);
}