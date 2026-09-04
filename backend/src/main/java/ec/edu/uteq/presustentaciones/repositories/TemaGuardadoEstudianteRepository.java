package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TemaGuardadoEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemaGuardadoEstudianteRepository extends JpaRepository<TemaGuardadoEstudiante, Integer> {

    @Query("""
            SELECT g FROM TemaGuardadoEstudiante g
            LEFT JOIN FETCH g.temaPropuesto t
            LEFT JOIN FETCH t.carrera
            LEFT JOIN FETCH t.lineaInvestigacion
            LEFT JOIN FETCH t.area
            WHERE g.estudiante.id = :estudianteId
            ORDER BY g.fechaGuardado DESC
            """)
    List<TemaGuardadoEstudiante> findByEstudianteIdOrderByFechaGuardadoDesc(@Param("estudianteId") Long estudianteId);

    List<TemaGuardadoEstudiante> findByEstudianteId(Long estudianteId);

    boolean existsByEstudianteIdAndTemaPropuestoId(Long estudianteId, Integer temaPropuestoId);

    @Modifying
    int deleteByEstudianteIdAndTemaPropuestoId(Long estudianteId, Integer temaPropuestoId);

    @Query("SELECT g.temaPropuesto.id FROM TemaGuardadoEstudiante g WHERE g.estudiante.id = :estudianteId")
    List<Integer> findTemaIdsByEstudianteId(@Param("estudianteId") Long estudianteId);
}
