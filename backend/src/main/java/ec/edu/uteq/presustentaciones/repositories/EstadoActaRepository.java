package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EstadoActa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoActaRepository extends JpaRepository<EstadoActa, Short> {
    Optional<EstadoActa> findByCodigo(String codigo);
}
