package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EstadoProceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoProcesoRepository extends JpaRepository<EstadoProceso, Short> {
    Optional<EstadoProceso> findByCodigo(String codigo);
}
