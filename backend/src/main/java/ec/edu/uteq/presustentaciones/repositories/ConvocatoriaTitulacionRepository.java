package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.ConvocatoriaTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConvocatoriaTitulacionRepository extends JpaRepository<ConvocatoriaTitulacion, Integer> {
    Optional<ConvocatoriaTitulacion> findByCodigo(String codigo);
    List<ConvocatoriaTitulacion> findByPeriodoAcademicoId(Integer periodoId);
    /** Devuelve la convocatoria activa (activa = true) */
    Optional<ConvocatoriaTitulacion> findFirstByActivaTrue();
    List<ConvocatoriaTitulacion> findByActivaTrue();
}
