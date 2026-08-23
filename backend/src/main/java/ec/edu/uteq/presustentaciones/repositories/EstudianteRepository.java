package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Estudiante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    /** Listado del panel de admin -- 41,000+ filas, siempre pagina. Busca por nombre,
     * apellido, email, expediente o nombre de carrera. */
    @Query("SELECT e FROM Estudiante e JOIN FETCH e.usuario u JOIN FETCH e.carreraEntidad c " +
           "JOIN FETCH e.estadoAcademico WHERE :q IS NULL OR :q = '' " +
           "OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(e.expedienteCodigo) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Estudiante> buscarPaginado(@Param("q") String q, Pageable pageable);

    /** Última solicitud (el "proyecto" vigente) de cada estudiante de la página actual,
     * en un solo query -- evita N+1 al pedir el proyecto por separado por cada fila. */
    @Query(value = "SELECT DISTINCT ON (s.estudiante_id) s.estudiante_id, s.titulo_tema, s.estado " +
           "FROM presus.solicitud s WHERE s.estudiante_id IN :ids " +
           "ORDER BY s.estudiante_id, s.fecha_registro DESC", nativeQuery = true)
    List<Object[]> findUltimoProyectoPorEstudianteIds(@Param("ids") List<Long> ids);

    /**
     * Invoca sp_generar_codigo_expediente (FUNCTION escalar, categoría "generación de
     * códigos secuenciales" del Bloque A.2): usa nextval() sobre una secuencia dedicada,
     * atómico a nivel de motor -- sin condiciones de carrera entre altas concurrentes.
     */
    @Procedure(name = "Estudiante.generarCodigoExpediente")
    String generarCodigoExpediente(@Param("p_anio") Integer anio, @Param("p_codigo") String codigoInicial);

    Optional<Estudiante> findByExpedienteCodigo(String expedienteCodigo);
    
    Optional<Estudiante> findByUsuarioId(Long usuarioId);
    
    List<Estudiante> findByCarrera(String carrera);
    
    @Query("SELECT e FROM Estudiante e JOIN FETCH e.usuario WHERE e.id = :id")
    Optional<Estudiante> findByIdWithUsuario(Long id);
}
