package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TutoriaFase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutoriaFaseRepository extends JpaRepository<TutoriaFase, Long> {

    List<TutoriaFase> findByTutorIdOrderByNumeroFaseAsc(Long tutorId);

    Optional<TutoriaFase> findByTutorIdAndNumeroFase(Long tutorId, Integer numeroFase);

    long countByTutorIdAndEstado(Long tutorId, String estado);

    long countByTutorId(Long tutorId);

    @org.springframework.data.jpa.repository.query.Procedure(procedureName = "presus.sp_registrar_tutoria_avance")
    void spRegistrarTutoriaAvance(
            @Param("p_tutor_id") Long tutorId,
            @Param("p_numero_fase") Integer numeroFase,
            @Param("p_archivo_pdf") String archivoPdf,
            @Param("p_tamano_bytes") Long tamanoBytes,
            @Param("p_sha256") String sha256
    );
}
