package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** Carga solicitudes con estudiante+usuario en un solo query — evita LazyInitializationException */
    @Query("SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u ORDER BY s.fechaRegistro DESC")
    List<Solicitud> findAllWithEstudiante();

    /** Página de solicitudes con estudiante+usuario precargados — evita traer las 44k filas de una vez */
    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u",
           countQuery = "SELECT count(s) FROM Solicitud s")
    Page<Solicitud> findAllWithEstudiante(Pageable pageable);

    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.estudiante e JOIN FETCH e.usuario u WHERE s.estado.codigo = :codigo",
           countQuery = "SELECT count(s) FROM Solicitud s WHERE s.estado.codigo = :codigo")
    Page<Solicitud> findAllWithEstudianteByEstadoCodigo(@Param("codigo") String codigo, Pageable pageable);

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
}
