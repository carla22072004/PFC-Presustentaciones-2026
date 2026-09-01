package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Optional<Tutor> findBySolicitudId(Long solicitudId);
    List<Tutor> findByDocenteId(Long docenteId);
    long countByDocenteIdAndEstado(Long docenteId, String estado);

    List<Tutor> findBySolicitudEstudianteUsuarioId(Long usuarioId);
    List<Tutor> findByDocenteUsuarioId(Long usuarioId);

    @Query(value = "SELECT * FROM presus.sp_obtener_estadisticas_tutores()", nativeQuery = true)
    List<Object[]> obtenerEstadisticasTutoresSp();

    /** Reportes: cuántas tutorías tiene asignadas cada docente (GROUP BY en la base). */
    @Query("SELECT t.docente.id, COUNT(t) FROM Tutor t GROUP BY t.docente.id")
    List<Object[]> contarTutoriasPorDocente();

    boolean existsBySolicitudIdAndDocenteUsuarioEmail(Long solicitudId, String email);
}
