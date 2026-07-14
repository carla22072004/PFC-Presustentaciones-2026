package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EvaluacionCriterio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluacionCriterioRepository extends JpaRepository<EvaluacionCriterio, Long> {

    List<EvaluacionCriterio> findBySolicitudId(Long solicitudId);

    List<EvaluacionCriterio> findBySolicitudIdAndJuradoId(Long solicitudId, Long juradoId);

    boolean existsBySolicitudIdAndJuradoId(Long solicitudId, Long juradoId);

    void deleteBySolicitudIdAndJuradoId(Long solicitudId, Long juradoId);

    /** Promedio de notas de todos los jurados para una solicitud por criterio */
    @Query("SELECT ec.criterio.id, AVG(ec.notaObtenida) FROM EvaluacionCriterio ec " +
           "WHERE ec.solicitud.id = :solicitudId GROUP BY ec.criterio.id")
    List<Object[]> promediosPorCriterio(@Param("solicitudId") Long solicitudId);

    /** Nota total promedio del tribunal: promedio de (suma por jurado) usando dos pasos en Java */
    @Query("SELECT ec.jurado.id, SUM(ec.notaObtenida) " +
           "FROM EvaluacionCriterio ec " +
           "WHERE ec.solicitud.id = :solicitudId " +
           "GROUP BY ec.jurado.id")
    List<Object[]> sumaPorJurado(@Param("solicitudId") Long solicitudId);
}
