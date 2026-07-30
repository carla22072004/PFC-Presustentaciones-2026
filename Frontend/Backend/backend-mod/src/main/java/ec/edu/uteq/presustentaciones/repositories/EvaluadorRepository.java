package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Evaluador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluadorRepository extends JpaRepository<Evaluador, Long> {
    List<Evaluador> findBySolicitudId(Long solicitudId);
    Optional<Evaluador> findBySolicitudIdAndDocenteIdAndTipoEvaluadorCodigo(Long solicitudId, Long docenteId, String tipoEvaluadorCodigo);
}
