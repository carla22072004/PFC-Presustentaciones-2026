package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Acta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    Optional<Acta> findBySolicitudId(Long solicitudId);

    @org.springframework.data.jpa.repository.query.Procedure(procedureName = "presus.sp_firmar_acta_digital")
    void spFirmarActaDigital(
            @Param("p_acta_id") Long actaId,
            @Param("p_rol") String rol,
            @Param("p_observacion") String observacion
    );
}
