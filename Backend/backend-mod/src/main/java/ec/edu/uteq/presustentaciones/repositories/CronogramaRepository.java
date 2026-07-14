package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CronogramaRepository extends JpaRepository<Cronograma, Long> {

    @Query("SELECT c FROM Cronograma c JOIN c.solicitud s JOIN s.estudiante e WHERE e.id = :estudianteId")
    List<Cronograma> findByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT c FROM Cronograma c JOIN c.solicitud s JOIN s.estudiante e JOIN e.usuario u WHERE u.id = :usuarioId")
    List<Cronograma> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    Optional<Cronograma> findBySolicitudId(Long solicitudId);

    /** RF-04: Conflictos en sala: cualquier cronograma que se solape con la franja propuesta */
    @Query("""
        SELECT c FROM Cronograma c
        WHERE c.sala.id = :salaId
          AND c.estado = 'ACTIVO'
          AND c.fechaInicio < :fin
          AND FUNCTION('TIMESTAMPADD', MINUTE, c.duracionMin, c.fechaInicio) > :inicio
    """)
    List<Cronograma> findConflictos(@Param("salaId") Long salaId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    /** Todos los cronogramas activos de una fecha */
    @Query("SELECT c FROM Cronograma c WHERE c.estado = 'ACTIVO' AND CAST(c.fechaInicio AS date) = CAST(:fecha AS date)")
    List<Cronograma> findActivosPorFecha(@Param("fecha") LocalDateTime fecha);
}
