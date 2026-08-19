package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface JuradoService {

    // ── Jurados ──────────────────────────────────────────────────────────────
    Jurado asignarJurado(Long solicitudId, Long docenteId, String rol);
    List<Jurado> listarPorSolicitud(Long solicitudId);
    Page<Jurado> listarTodos(Pageable pageable);
    void eliminarJurado(Long juradoId);

    // ── Tutor ─────────────────────────────────────────────────────────────────
    Tutor asignarTutor(Long solicitudId, Long docenteId);
    Optional<Tutor> obtenerTutorDeSolicitud(Long solicitudId);
    void eliminarTutor(Long tutorId);

    // ── Sugerencia automática ─────────────────────────────────────────────────
    List<Docente> sugerirDocentes(Long solicitudId, int cantidad);
    void asignarJuradosAutomaticamente(Long solicitudId);

    // ── Asignación masiva vía procedimiento almacenado (sp_asignar_jurado_masivo) ─
    /**
     * Asigna en lote pares (solicitudId, docenteId) al rol indicado, invocando
     * sp_asignar_jurado_masivo una vez por par. Toda la operación corre dentro
     * de una única transacción: si un par falla (rol inválido, FK inexistente),
     * se revierten también los pares ya procesados en esa misma llamada.
     */
    void asignarJuradoMasivo(List<Long> solicitudIds, List<Long> docenteIds, String rolCodigo);

    // ── Vista del docente ─────────────────────────────────────────────────────
    List<Jurado> listarPorDocente(Long docenteId);
    List<Tutor> listarTutoriasPorDocente(Long docenteId);
    Optional<Jurado> obtenerInfoJurado(Long solicitudId, Long usuarioId);
}
