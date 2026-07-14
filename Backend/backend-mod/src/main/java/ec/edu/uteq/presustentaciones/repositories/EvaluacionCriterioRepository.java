package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.EvaluacionCriterio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EvaluacionCriterioRepository extends JpaRepository<EvaluacionCriterio, Long> {

    List<EvaluacionCriterio> findBySolicitudId(Long solicitudId);

    List<EvaluacionCriterio> findBySolicitudIdAndEvaluadorId(Long solicitudId, Long evaluadorId);

    boolean existsBySolicitudIdAndEvaluadorId(Long solicitudId, Long evaluadorId);

    void deleteBySolicitudIdAndEvaluadorId(Long solicitudId, Long evaluadorId);

    /** Promedio de notas de todos los jurados para una solicitud por criterio */
    @Query("SELECT ec.criterio.id, AVG(ec.notaObtenida) FROM EvaluacionCriterio ec " +
           "WHERE ec.solicitud.id = :solicitudId GROUP BY ec.criterio.id")
    List<Object[]> promediosPorCriterio(@Param("solicitudId") Long solicitudId);

    /** Nota total promedio del tribunal: promedio de (suma por jurado) usando dos pasos en Java */
    @Query("SELECT ec.evaluador.id, SUM(ec.notaObtenida) " +
           "FROM EvaluacionCriterio ec " +
           "WHERE ec.solicitud.id = :solicitudId " +
           "GROUP BY ec.evaluador.id")
    List<Object[]> sumaPorEvaluador(@Param("solicitudId") Long solicitudId);
}
