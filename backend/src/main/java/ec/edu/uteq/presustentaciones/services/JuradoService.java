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

    /**
     * Asigna (upsert) un docente como jurado de una solicitud con el rol indicado.
     *
     * @param solicitudId id de la solicitud
     * @param docenteId   id del docente a asignar
     * @param rol         código de rol de jurado ({@code PRESIDENTE}, {@code VOCAL_1} o
     *                    {@code VOCAL_2})
     * @return el registro de jurado creado o actualizado
     * @throws RuntimeException si el rol no es uno de los tres válidos
     */
    Jurado asignarJurado(Long solicitudId, Long docenteId, String rol);

    /**
     * @param solicitudId id de la solicitud
     * @return los jurados asignados a esa solicitud (0 a 3 registros)
     */
    List<Jurado> listarPorSolicitud(Long solicitudId);

    /**
     * @param pageable configuración de paginación
     * @return página de todos los registros de jurado del sistema
     */
    Page<Jurado> listarTodos(Pageable pageable);

    /** @param juradoId id del registro de jurado a eliminar */
    void eliminarJurado(Long juradoId);

    // ── Tutor ─────────────────────────────────────────────────────────────────

    /**
     * @param solicitudId id de la solicitud
     * @param docenteId   id del docente que actuará como tutor
     * @return el registro de tutoría creado
     */
    Tutor asignarTutor(Long solicitudId, Long docenteId);

    /**
     * @param solicitudId id de la solicitud
     * @return el tutor asignado, si existe
     */
    Optional<Tutor> obtenerTutorDeSolicitud(Long solicitudId);

    /** @param tutorId id del registro de tutoría a eliminar */
    void eliminarTutor(Long tutorId);

    // ── Sugerencia automática ─────────────────────────────────────────────────

    /**
     * Sugiere docentes candidatos a jurado para una solicitud (excluyendo al tutor asignado y a
     * quienes ya tengan conflicto de horario), sin asignarlos todavía.
     *
     * @param solicitudId id de la solicitud
     * @param cantidad    número máximo de docentes a sugerir
     * @return lista de docentes candidatos, tamaño ≤ {@code cantidad}
     */
    List<Docente> sugerirDocentes(Long solicitudId, int cantidad);

    /**
     * Asigna automáticamente los 3 roles de tribunal (PRESIDENTE, VOCAL_1, VOCAL_2) para una
     * solicitud, usando la misma lógica de sugerencia que {@link #sugerirDocentes}.
     *
     * @param solicitudId id de la solicitud
     * @throws RuntimeException si no hay suficientes docentes disponibles para completar el
     *                          tribunal
     */
    void asignarJuradosAutomaticamente(Long solicitudId);

    // ── Asignación masiva vía procedimiento almacenado (sp_asignar_jurado_masivo) ─

    /**
     * Asigna en lote pares (solicitudId, docenteId) al rol indicado, invocando
     * sp_asignar_jurado_masivo una vez por par. Toda la operación corre dentro
     * de una única transacción: si un par falla (rol inválido, FK inexistente),
     * se revierten también los pares ya procesados en esa misma llamada.
     *
     * @param solicitudIds ids de las solicitudes, en el mismo orden que {@code docenteIds}
     * @param docenteIds   ids de los docentes a asignar, uno por cada solicitud del arreglo
     * @param rolCodigo    código de rol aplicado a todos los pares del lote
     * @throws RuntimeException si los dos arreglos no tienen la misma longitud, o si el
     *                          procedimiento almacenado rechaza algún par (rol inválido, FK
     *                          inexistente, o conflicto de horario)
     */
    void asignarJuradoMasivo(List<Long> solicitudIds, List<Long> docenteIds, String rolCodigo);

    // ── Vista del docente ─────────────────────────────────────────────────────

    /**
     * @param docenteId id del docente
     * @return las asignaciones de jurado de ese docente, en cualquier solicitud
     */
    List<Jurado> listarPorDocente(Long docenteId);

    /**
     * @param docenteId id del docente
     * @return las tutorías activas de ese docente
     */
    List<Tutor> listarTutoriasPorDocente(Long docenteId);

    /**
     * @param solicitudId id de la solicitud
     * @param usuarioId   id del usuario autenticado (se resuelve contra el docente vinculado)
     * @return la asignación de jurado de ese usuario en esa solicitud, si existe
     */
    Optional<Jurado> obtenerInfoJurado(Long solicitudId, Long usuarioId);

    /**
     * Variante de {@link #asignarJuradoMasivo} que invoca directamente la sobrecarga de
     * {@code sp_asignar_jurado_masivo} que recibe arreglos SQL ({@code BIGINT[]}) en una sola
     * llamada, en vez de iterar en Java. Ver la nota de fusión de ramas en
     * {@code docs/basedatos/CATALOGO-SP.md} sobre por qué la variante escalar (iterando en
     * Java) es la que queda verificada end-to-end, no esta.
     *
     * @param solicitudIds arreglo de ids de solicitud
     * @param docenteIds   arreglo de ids de docente, en el mismo orden
     * @param rol          código de rol aplicado a todo el lote
     */
    void asignarJuradoMasivoSP(Long[] solicitudIds, Long[] docenteIds, String rol);
}
