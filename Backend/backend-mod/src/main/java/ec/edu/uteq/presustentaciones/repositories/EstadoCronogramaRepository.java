package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EstadoCronograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCronogramaRepository extends JpaRepository<EstadoCronograma, Short> {
    Optional<EstadoCronograma> findByCodigo(String codigo);
}
