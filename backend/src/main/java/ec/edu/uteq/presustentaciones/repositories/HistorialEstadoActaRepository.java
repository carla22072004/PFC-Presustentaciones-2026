package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.HistorialEstadoActa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoActaRepository extends JpaRepository<HistorialEstadoActa, Long> {

    @Query("SELECT h FROM HistorialEstadoActa h " +
           "LEFT JOIN FETCH h.usuario u " +
           "LEFT JOIN FETCH h.estadoAnterior ea " +
           "JOIN FETCH h.estadoNuevo en " +
           "WHERE h.acta.id = :actaId ORDER BY h.fechaCambio DESC, h.id DESC")
    List<HistorialEstadoActa> findByActaIdOrderByFechaCambioDesc(@Param("actaId") Long actaId);

    long countByActaId(Long actaId);
}
