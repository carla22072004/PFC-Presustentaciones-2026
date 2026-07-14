package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Jurado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JuradoRepository extends JpaRepository<Jurado, Long> {

    List<Jurado> findBySolicitudId(Long solicitudId);

    @Query("SELECT j FROM Jurado j WHERE j.docente.id = :docenteId")
    List<Jurado> findByDocenteId(Long docenteId);

    @Query("SELECT COUNT(j) FROM Jurado j WHERE j.docente.id = :docenteId AND j.solicitud.estado NOT IN ('RECHAZADA')")
    long contarAsignacionesActivasByDocente(Long docenteId);

    @Query("SELECT j FROM Jurado j JOIN j.docente d JOIN d.usuario u " +
           "WHERE j.solicitud.id = :solicitudId AND u.id = :usuarioId")
    Optional<Jurado> findBySolicitudIdAndUsuarioId(@Param("solicitudId") Long solicitudId, 
                                                    @Param("usuarioId") Long usuarioId);
}
