package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Acta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    @Query("SELECT a FROM Acta a JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE s.id = :solicitudId")
    Optional<Acta> findBySolicitudId(@Param("solicitudId") Long solicitudId);

    @Query(value = "SELECT a FROM Acta a JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u",
           countQuery = "SELECT COUNT(a) FROM Acta a")
    Page<Acta> findAll(Pageable pageable);

    /** Detalle de un acta con solicitud + estudiante + usuario + estado en un solo query. */
    @Query("SELECT a FROM Acta a JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u JOIN FETCH a.estado est WHERE a.id = :id")
    Optional<Acta> findDetalleById(@Param("id") Long id);

    /**
     * "Mis actas" del docente: actas de pre-sustentaciones en las que el usuario es
     * jurado (miembros_tribunal) o tutor (tutores). DISTINCT porque un docente puede
     * ser jurado en más de un rol de la misma solicitud.
     */
    @Query(value = "SELECT DISTINCT a FROM Acta a " +
            "JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u JOIN FETCH a.estado est " +
            "WHERE EXISTS (SELECT 1 FROM Jurado j WHERE j.solicitud = s AND j.docente.usuario.email = :email) " +
            "   OR EXISTS (SELECT 1 FROM Tutor t WHERE t.solicitud = s AND t.docente.usuario.email = :email)",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Acta a JOIN a.solicitud s " +
            "WHERE EXISTS (SELECT 1 FROM Jurado j WHERE j.solicitud = s AND j.docente.usuario.email = :email) " +
            "   OR EXISTS (SELECT 1 FROM Tutor t WHERE t.solicitud = s AND t.docente.usuario.email = :email)")
    Page<Acta> findMisActas(@Param("email") String email, Pageable pageable);

    /** ¿Es el usuario tutor o jurado de la solicitud de esta acta? (control de acceso del docente). */
    @Query("SELECT (COUNT(a) > 0) FROM Acta a JOIN a.solicitud s WHERE a.id = :actaId AND (" +
            "EXISTS (SELECT 1 FROM Jurado j WHERE j.solicitud = s AND j.docente.usuario.email = :email) " +
            "OR EXISTS (SELECT 1 FROM Tutor t WHERE t.solicitud = s AND t.docente.usuario.email = :email) " +
            "OR s.estudiante.usuario.email = :email)")
    boolean esParticipante(@Param("actaId") Long actaId, @Param("email") String email);

    /**
     * Búsqueda/filtrado administrativo. Los parámetros String nulos/vacíos no filtran; el
     * {@code OR :param = ''} extra le da a Hibernate la pista de tipo String para el bind
     * (sin él, un parámetro null se enlaza como bytea y Postgres falla en LOWER()).
     *
     * Las fechas NUNCA llegan null: {@code ActaServiceImpl.buscarActas} sustituye por
     * sentinelas (1900-01-01 / 9999-12-31) cuando el usuario no filtra por fecha. Un
     * {@code :desde IS NULL} dejaba a Postgres sin tipo para el bind ("could not determine
     * data type of parameter") — mismo motivo por el que SolicitudRepository.buscarConFiltros
     * usa sentinelas en vez de comprobar null.
     */
    @Query(value = "SELECT a FROM Acta a " +
            "JOIN FETCH a.solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u JOIN FETCH a.estado est " +
            "WHERE (:estado IS NULL OR :estado = '' OR est.codigo = :estado) " +
            "AND (:carrera IS NULL OR :carrera = '' OR LOWER(e.carrera) LIKE LOWER(CONCAT('%', :carrera, '%'))) " +
            "AND a.fechaGeneracion >= :desde AND a.fechaGeneracion <= :hasta " +
            "AND (:q IS NULL OR :q = '' OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(s.tituloTema) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(a) FROM Acta a JOIN a.solicitud s JOIN s.estudiante e JOIN e.usuario u JOIN a.estado est " +
            "WHERE (:estado IS NULL OR :estado = '' OR est.codigo = :estado) " +
            "AND (:carrera IS NULL OR :carrera = '' OR LOWER(e.carrera) LIKE LOWER(CONCAT('%', :carrera, '%'))) " +
            "AND a.fechaGeneracion >= :desde AND a.fechaGeneracion <= :hasta " +
            "AND (:q IS NULL OR :q = '' OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(s.tituloTema) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Acta> buscarConFiltros(@Param("estado") String estado,
                                @Param("carrera") String carrera,
                                @Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta,
                                @Param("q") String q,
                                Pageable pageable);

    // ── Agregados para reportes (COUNT en la base, nunca en memoria) ──────────
    long countByEstadoCodigo(String codigo);

    long countByFirmadaFalse();

    @Query("SELECT a.estado.codigo AS codigo, COUNT(a) AS total FROM Acta a " +
           "WHERE a.fechaGeneracion >= :desde AND a.fechaGeneracion <= :hasta " +
           "GROUP BY a.estado.codigo")
    List<Object[]> contarPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Invoca sp_firmar_acta_digital (PROCEDURE). Fase 3 / Criterio P1. */
    @Procedure(procedureName = "presus.sp_firmar_acta_digital")
    void firmarActaDigital(@Param("p_acta_id") Long actaId,
                            @Param("p_rol") String rol,
                            @Param("p_observacion") String observacion);
}
