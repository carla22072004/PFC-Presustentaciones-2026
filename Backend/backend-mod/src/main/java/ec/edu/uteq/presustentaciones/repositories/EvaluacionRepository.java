package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    @Query("SELECT ev FROM Evaluacion ev JOIN ev.solicitud s JOIN s.estudiante e WHERE e.id = :estudianteId")
    List<Evaluacion> findByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT ev FROM Evaluacion ev JOIN ev.solicitud s JOIN s.estudiante e JOIN e.usuario u WHERE u.id = :usuarioId")
    List<Evaluacion> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    Optional<Evaluacion> findBySolicitudId(Long solicitudId);
}
