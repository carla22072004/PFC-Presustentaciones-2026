package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EvaluacionJurado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionJuradoRepository extends JpaRepository<EvaluacionJurado, Long> {
    
    Optional<EvaluacionJurado> findBySolicitudIdAndJuradoId(Long solicitudId, Long juradoId);
    
    List<EvaluacionJurado> findBySolicitudId(Long solicitudId);
    
    boolean existsBySolicitudIdAndJuradoId(Long solicitudId, Long juradoId);
}
