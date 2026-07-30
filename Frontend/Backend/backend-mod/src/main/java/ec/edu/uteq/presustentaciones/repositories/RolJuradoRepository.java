package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.RolJurado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolJuradoRepository extends JpaRepository<RolJurado, Short> {
    Optional<RolJurado> findByCodigo(String codigo);
}
