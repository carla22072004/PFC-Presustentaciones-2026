package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.ProgresoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgresoEstudianteRepository extends JpaRepository<ProgresoEstudiante, Integer> {

    Optional<ProgresoEstudiante> findByEstudianteId(Long estudianteId);
}
