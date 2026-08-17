package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Jurado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JuradoRepository extends JpaRepository<Jurado, Long> {

    List<Jurado> findBySolicitudId(Long solicitudId);

    /** Invoca sp_asignar_jurado_masivo (PROCEDURE). Fase 3 / Criterio P1. */
    @Procedure(procedureName = "presus.sp_asignar_jurado_masivo")
    void asignarJuradoMasivo(@Param("p_solicitud_ids") Long[] solicitudIds,
                              @Param("p_docente_ids") Long[] docenteIds,
                              @Param("p_rol") String rol);

    /**
     * Invoca sp_validar_conflicto_jurado (FUNCTION escalar, categoría "validaciones
     * cruzadas" del Bloque A.2): true si el docente NO tiene otra defensa asignada que se
     * solape con el horario dado.
     */
    @Procedure(name = "Jurado.validarConflictoJurado")
    Boolean validarConflictoJurado(@Param("p_solicitud_id") Long solicitudId,
                                    @Param("p_docente_id") Long docenteId,
                                    @Param("p_fecha_inicio") LocalDateTime fechaInicio,
                                    @Param("p_duracion_min") Integer duracionMin,
                                    @Param("p_disponible") Boolean disponibleInicial);

    @Query("SELECT j FROM Jurado j WHERE j.docente.id = :docenteId")
    List<Jurado> findByDocenteId(Long docenteId);

    @Query("SELECT COUNT(j) FROM Jurado j WHERE j.docente.id = :docenteId AND j.solicitud.estado.codigo != 'RECHAZADA'")
    long contarAsignacionesActivasByDocente(Long docenteId);

    @Query("SELECT j FROM Jurado j JOIN j.docente d JOIN d.usuario u " +
           "WHERE j.solicitud.id = :solicitudId AND u.id = :usuarioId")
    Optional<Jurado> findBySolicitudIdAndUsuarioId(@Param("solicitudId") Long solicitudId, 
                                                    @Param("usuarioId") Long usuarioId);
}
