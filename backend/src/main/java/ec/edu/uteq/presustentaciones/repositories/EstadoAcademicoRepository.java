package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EstadoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoAcademicoRepository extends JpaRepository<EstadoAcademico, Short> {
    Optional<EstadoAcademico> findByCodigo(String codigo);
}
