package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnteproyectoRepository extends JpaRepository<Anteproyecto, Long> {
    Optional<Anteproyecto> findBySolicitudId(Long solicitudId);
}
