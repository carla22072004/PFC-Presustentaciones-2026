package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Tutor;

import java.util.List;
import java.util.Optional;

public interface JuradoService {

    // ── Jurados ──────────────────────────────────────────────────────────────
    Jurado asignarJurado(Long solicitudId, Long docenteId, String rol);
    List<Jurado> listarPorSolicitud(Long solicitudId);
    List<Jurado> listarTodos();
    void eliminarJurado(Long juradoId);

    // ── Tutor ─────────────────────────────────────────────────────────────────
    Tutor asignarTutor(Long solicitudId, Long docenteId);
    Optional<Tutor> obtenerTutorDeSolicitud(Long solicitudId);
    void eliminarTutor(Long tutorId);

    // ── Sugerencia automática ─────────────────────────────────────────────────
    List<Docente> sugerirDocentes(Long solicitudId, int cantidad);
    void asignarJuradosAutomaticamente(Long solicitudId);

    // ── Vista del docente ─────────────────────────────────────────────────────
    List<Jurado> listarPorDocente(Long docenteId);
    List<Tutor> listarTutoriasPorDocente(Long docenteId);
    Optional<Jurado> obtenerInfoJurado(Long solicitudId, Long usuarioId);
}
