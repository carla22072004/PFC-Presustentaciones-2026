package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Acta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Acta a JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE s.id = :solicitudId")
    Optional<Acta> findBySolicitudId(@Param("solicitudId") Long solicitudId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT a FROM Acta a JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u",
           countQuery = "SELECT COUNT(a) FROM Acta a")
    org.springframework.data.domain.Page<Acta> findAll(org.springframework.data.domain.Pageable pageable);

    /** Invoca sp_firmar_acta_digital (PROCEDURE). Fase 3 / Criterio P1. */
    @Procedure(procedureName = "presus.sp_firmar_acta_digital")
    void firmarActaDigital(@Param("p_acta_id") Long actaId,
                            @Param("p_rol") String rol,
                            @Param("p_observacion") String observacion);
}
