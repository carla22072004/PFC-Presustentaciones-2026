package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TutoriaMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutoriaMensajeRepository extends JpaRepository<TutoriaMensaje, Long> {

    List<TutoriaMensaje> findByFaseIdOrderByFechaEnvioAsc(Long faseId);

    long countByFaseIdAndLeidoFalseAndRemitenteIdNot(Long faseId, Long remitenteId);

    List<TutoriaMensaje> findByFaseIdAndLeidoFalseAndRemitenteIdNot(Long faseId, Long remitenteId);
}
