package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Acta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    Optional<Acta> findBySolicitudId(Long solicitudId);
}
