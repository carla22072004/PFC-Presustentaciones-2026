package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.RecursoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecursoTitulacionRepository extends JpaRepository<RecursoTitulacion, Integer> {

    @Query("""
            SELECT r FROM RecursoTitulacion r
            LEFT JOIN FETCH r.carrera c
            WHERE :carreraId IS NULL OR c IS NULL OR c.id = :carreraId
            ORDER BY r.categoria ASC, r.titulo ASC
            """)
    List<RecursoTitulacion> listarVisiblesParaCarrera(@Param("carreraId") Integer carreraId);

    @Query("SELECT r FROM RecursoTitulacion r LEFT JOIN FETCH r.carrera ORDER BY r.categoria ASC, r.titulo ASC")
    List<RecursoTitulacion> listarTodos();
}
