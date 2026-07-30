package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Tutor;
import java.util.List;
import java.util.Optional;

public interface TutorService {
    Tutor asignarTutor(Long solicitudId, Long docenteId);
    Optional<Tutor> buscarPorSolicitud(Long solicitudId);
    List<Tutor> listarTodos();
    void eliminarTutor(Long tutorId);
}
