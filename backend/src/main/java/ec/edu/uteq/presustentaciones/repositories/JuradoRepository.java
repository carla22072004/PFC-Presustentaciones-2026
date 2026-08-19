package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Jurado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JuradoRepository extends JpaRepository<Jurado, Long> {

    /**
     * Invoca el procedimiento almacenado PostgreSQL sp_asignar_jurado_masivo
     * (backend/src/main/resources/db/migration/V2__stored_procedures.sql),
     * que hace el upsert en miembros_tribunal resolviendo rol_jurado_id
     * a partir del código de rol. Se llama una vez por par solicitud/docente
     * dentro de una transacción Spring (@Transactional en el servicio) para
     * que el lote completo se confirme o revierta como una unidad.
     */
    @Procedure(procedureName = "sp_asignar_jurado_masivo")
    void spAsignarJuradoMasivo(@Param("p_solicitud_id") Long solicitudId,
                                @Param("p_docente_id") Long docenteId,
                                @Param("p_rol_codigo") String rolCodigo);

    List<Jurado> findBySolicitudId(Long solicitudId);

    @Query("SELECT j FROM Jurado j WHERE j.docente.id = :docenteId")
    List<Jurado> findByDocenteId(Long docenteId);

    @Query("SELECT COUNT(j) FROM Jurado j WHERE j.docente.id = :docenteId AND j.solicitud.estado.codigo != 'RECHAZADA'")
    long contarAsignacionesActivasByDocente(Long docenteId);

    @Query("SELECT j FROM Jurado j JOIN j.docente d JOIN d.usuario u " +
           "WHERE j.solicitud.id = :solicitudId AND u.id = :usuarioId")
    Optional<Jurado> findBySolicitudIdAndUsuarioId(@Param("solicitudId") Long solicitudId, 
                                                    @Param("usuarioId") Long usuarioId);
}
