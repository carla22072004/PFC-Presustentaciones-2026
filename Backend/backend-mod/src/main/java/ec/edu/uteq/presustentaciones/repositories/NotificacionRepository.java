package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("SELECT n FROM Notificacion n JOIN FETCH n.usuario WHERE n.usuario.id = :usuarioId ORDER BY n.fecha DESC")
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}