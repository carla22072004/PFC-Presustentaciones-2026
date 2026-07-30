package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.LineaInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineaInvestigacionRepository extends JpaRepository<LineaInvestigacion, Integer> {
    Optional<LineaInvestigacion> findByCodigo(String codigo);
    List<LineaInvestigacion> findByFacultadId(Integer facultadId);
}
