package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.SolicitudSupresion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudSupresionRepository extends JpaRepository<SolicitudSupresion, Long> {
    List<SolicitudSupresion> findByUsuarioId(Long usuarioId);
    List<SolicitudSupresion> findAllByOrderByFechaSolicitudDesc();
    boolean existsByUsuarioIdAndEstado(Long usuarioId, String estado);
}
