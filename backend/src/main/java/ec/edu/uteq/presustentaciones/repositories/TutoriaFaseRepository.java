package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TutoriaFase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutoriaFaseRepository extends JpaRepository<TutoriaFase, Long> {

    List<TutoriaFase> findByTutorIdOrderByNumeroFaseAsc(Long tutorId);

    Optional<TutoriaFase> findByTutorIdAndNumeroFase(Long tutorId, Integer numeroFase);

    long countByTutorIdAndEstado(Long tutorId, String estado);

    long countByTutorId(Long tutorId);
}
