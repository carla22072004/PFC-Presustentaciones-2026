package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EvaluacionFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionFinalRepository extends JpaRepository<EvaluacionFinal, Long> {
    Optional<EvaluacionFinal> findBySolicitudId(Long solicitudId);

    @Query("SELECT ef FROM EvaluacionFinal ef WHERE ef.solicitud.estudiante.id = :estudianteId")
    List<EvaluacionFinal> findByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT ef FROM EvaluacionFinal ef WHERE ef.solicitud.estudiante.usuario.id = :usuarioId")
    List<EvaluacionFinal> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
