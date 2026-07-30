package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.CriterioRubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CriterioRubricaRepository extends JpaRepository<CriterioRubrica, Long> {
    List<CriterioRubrica> findByRubricaIdOrderByOrdenAsc(Long rubricaId);
}
