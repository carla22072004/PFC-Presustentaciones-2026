package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface TutorService {
    Tutor asignarTutor(Long solicitudId, Long docenteId);
    Optional<Tutor> buscarPorSolicitud(Long solicitudId);
    Page<Tutor> listarTodos(Pageable pageable);
    void eliminarTutor(Long tutorId);
}
