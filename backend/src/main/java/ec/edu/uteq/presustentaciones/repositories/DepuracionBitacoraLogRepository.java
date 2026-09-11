package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.DepuracionBitacoraLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepuracionBitacoraLogRepository extends JpaRepository<DepuracionBitacoraLog, Long> {
    List<DepuracionBitacoraLog> findTop50ByOrderByFechaEjecucionDesc();
}
