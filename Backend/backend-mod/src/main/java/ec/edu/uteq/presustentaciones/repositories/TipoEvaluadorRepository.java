package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TipoEvaluador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoEvaluadorRepository extends JpaRepository<TipoEvaluador, Short> {
    Optional<TipoEvaluador> findByCodigo(String codigo);
}
