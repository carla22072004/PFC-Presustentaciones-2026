package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.HistorialEstadosSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadosSolicitudRepository extends JpaRepository<HistorialEstadosSolicitud, Long> {
    List<HistorialEstadosSolicitud> findBySolicitudIdOrderByFechaCambioDesc(Long solicitudId);
}
