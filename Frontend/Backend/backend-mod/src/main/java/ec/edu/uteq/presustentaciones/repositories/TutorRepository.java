package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Optional<Tutor> findBySolicitudId(Long solicitudId);
    List<Tutor> findByDocenteId(Long docenteId);
    long countByDocenteIdAndEstado(Long docenteId, String estado);

    List<Tutor> findBySolicitudEstudianteUsuarioId(Long usuarioId);
    List<Tutor> findByDocenteUsuarioId(Long usuarioId);
}
