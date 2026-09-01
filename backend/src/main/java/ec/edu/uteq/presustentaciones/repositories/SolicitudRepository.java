package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.dto.ReporteDefensaResult;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /**
     * Invoca sp_generar_reporte_defensas (JPA 2.1 @NamedStoredProcedureQuery declarada en
     * Solicitud.java) -- reporte consolidado multi-tabla por carrera. Fase 3 / Criterio P1.
     */
    @Procedure(name = "Solicitud.generarReporteDefensas")
    List<ReporteDefensaResult> generarReporteDefensas(@Param("p_carrera") String carrera);

    /** Carga solicitudes con estudiante+usuario en un solo query — evita LazyInitializationException */
    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findAllWithEstudiante();

    /**
     * Igual que {@link #findAllWithEstudiante()} pero acotada por {@link Pageable}. El endpoint
     * sin paginar ({@code GET /api/v1/solicitudes}) NUNCA debe devolver las 44k+ filas del volumen
     * real de una sola vez: son ~93 MB de JSON que congelan el navegador y llenan Redis (la lista
     * está cacheada). El listado completo navegable es {@code GET /api/v1/solicitudes/paginado}.
     * Como {@code estudiante} y {@code usuario} son asociaciones @ManyToOne (a-uno), Hibernate
     * pagina en SQL sin el warning de "collection fetch + firstResult/maxResults en memoria".
     */
    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findAllWithEstudiante(Pageable pageable);

    /**
     * Página de solicitudes con estudiante+usuario precargados — evita traer las 44k filas de
     * una vez. Combina el filtro de estado (pestañas), búsqueda de texto libre por título
     * del tema o nombre/apellido del estudiante (ERR-01: el listado del coordinador solo
     * filtraba por estado, sin buscador), y un rango de fechaRegistro (para poder acotar a
     * "hoy" o a un día concreto desde el frontend). Todos los parámetros son opcionales e
     * independientes entre sí -- mismo patrón que UsuarioRepository.buscarPaginado.
     */
    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u " +
           "WHERE (:estado IS NULL OR :estado = '' OR s.estado.codigo = :estado) " +
           "AND (:texto IS NULL OR :texto = '' " +
           "     OR LOWER(s.tituloTema) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "     OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "     OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
           "AND s.fechaRegistro >= :fechaDesde AND s.fechaRegistro <= :fechaHasta",
           countQuery = "SELECT count(s) FROM Solicitud s JOIN s.estudiante e JOIN e.usuario u " +
           "WHERE (:estado IS NULL OR :estado = '' OR s.estado.codigo = :estado) " +
           "AND (:texto IS NULL OR :texto = '' " +
           "     OR LOWER(s.tituloTema) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "     OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "     OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
           "AND s.fechaRegistro >= :fechaDesde AND s.fechaRegistro <= :fechaHasta")
    Page<Solicitud> buscarConFiltros(@Param("estado") String estado, @Param("texto") String texto,
                                      @Param("fechaDesde") LocalDateTime fechaDesde,
                                      @Param("fechaHasta") LocalDateTime fechaHasta,
                                      Pageable pageable);

    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE e.id = :estudianteId ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE u.id = :usuarioId ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE s.id = :id")
    Optional<Solicitud> findByIdWithEstudiante(@Param("id") Long id);

    long countByEstadoCodigo(String codigo);

    @Query("SELECT s.estado.codigo AS codigo, COUNT(s) AS total FROM Solicitud s GROUP BY s.estado.codigo")
    List<EstadoConteo> contarAgrupadoPorEstado();

    interface EstadoConteo {
        String getCodigo();
        Long getTotal();
    }

    // ── Agregados para el módulo de reportes (COORDINADOR / ADMINISTRADOR) ────
    // Todos resuelven con GROUP BY en Postgres: nunca cargan la tabla en memoria.

    /**
     * Cantidad de solicitudes por estado, filtrable por rango de fechaRegistro y carrera.
     * El rango de fechas NO es opcional a nivel de query (mismo criterio que
     * buscarConFiltros): el service pasa sentinelas MIN/MAX cuando el usuario no filtra --
     * un {@code :fecha IS NULL} deja a Postgres sin tipo para el bind ("could not determine
     * data type of parameter").
     */
    @Query("SELECT s.estado.codigo, COUNT(s) FROM Solicitud s " +
           "WHERE s.fechaRegistro >= :desde AND s.fechaRegistro <= :hasta " +
           "AND (:carrera IS NULL OR :carrera = '' OR LOWER(s.estudiante.carrera) LIKE LOWER(CONCAT('%', :carrera, '%'))) " +
           "GROUP BY s.estado.codigo ORDER BY s.estado.codigo")
    List<Object[]> contarPorEstado(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   @Param("carrera") String carrera);

    /** Cantidad de pre-sustentaciones (solicitudes) por período académico de su convocatoria. */
    @Query("SELECT p.codigo, COUNT(s) FROM Solicitud s JOIN s.convocatoria c JOIN c.periodoAcademico p " +
           "WHERE s.fechaRegistro >= :desde AND s.fechaRegistro <= :hasta " +
           "GROUP BY p.codigo ORDER BY p.codigo")
    List<Object[]> contarPorPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Estadísticas por carrera: total, completadas y rechazadas. */
    @Query("SELECT s.estudiante.carrera, COUNT(s), " +
           "SUM(CASE WHEN s.estado.codigo = 'COMPLETADA' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN s.estado.codigo = 'RECHAZADA' THEN 1 ELSE 0 END) " +
           "FROM Solicitud s GROUP BY s.estudiante.carrera ORDER BY COUNT(s) DESC")
    List<Object[]> estadisticasPorCarrera();

    long countByFechaRegistroBetween(LocalDateTime desde, LocalDateTime hasta);

    @Query(value = "SELECT * FROM presus.sp_generar_reporte_defensas(:carrera)", nativeQuery = true)
    List<Object[]> generarReporteDefensasSp(@Param("carrera") String carrera);
}
