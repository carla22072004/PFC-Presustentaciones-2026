package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TutoriaService {

    /**
     * @param tutorId   id del registro de tutoría
     * @param usuarioId id del usuario que consulta (para resolver permisos de vista)
     * @return resumen de la tutoría: fase actual, progreso y estado
     * @throws RuntimeException si la tutoría no existe
     */
    TutoriaResumenDTO obtenerResumen(Long tutorId, Long usuarioId);

    /**
     * @param tutorId id del registro de tutoría
     * @return las fases registradas de esa tutoría, en orden
     */
    List<TutoriaFaseDTO> obtenerFases(Long tutorId);

    /**
     * @param tutorId        id del registro de tutoría
     * @param tutorUsuarioId id del usuario docente que crea la fase
     * @param observacion    observación inicial del docente para esta fase
     * @return la fase creada
     * @throws RuntimeException si la tutoría no existe
     */
    TutoriaFaseDTO crearFaseConObservacion(Long tutorId, Long tutorUsuarioId, String observacion);

    /**
     * @param faseId              id de la fase de tutoría
     * @param archivo             PDF corregido subido por el estudiante
     * @param estudianteUsuarioId id del usuario estudiante que sube el archivo
     * @return la fase actualizada con el nuevo PDF
     * @throws RuntimeException si la fase no existe o el archivo no es un PDF válido
     */
    TutoriaFaseDTO subirPdfCorregido(Long faseId, MultipartFile archivo, Long estudianteUsuarioId);

    /**
     * @param faseId         id de la fase a aprobar
     * @param tutorUsuarioId id del usuario docente que aprueba
     * @param comentario     comentario opcional de aprobación
     * @return la fase actualizada en estado aprobado
     * @throws RuntimeException si la fase no existe
     */
    TutoriaFaseDTO aprobarFase(Long faseId, Long tutorUsuarioId, String comentario);

    /**
     * @param faseId      id de la fase de tutoría
     * @param remitenteId id del usuario que envía el mensaje
     * @param contenido   texto del mensaje
     * @param tipo        tipo de mensaje (p. ej. comentario, corrección)
     * @return el mensaje creado
     * @throws RuntimeException si la fase no existe
     */
    TutoriaMensajeDTO enviarMensaje(Long faseId, Long remitenteId, String contenido, String tipo);

    /**
     * @param faseId    id de la fase de tutoría
     * @param usuarioId id del usuario que marca los mensajes como leídos
     */
    void marcarMensajesLeidos(Long faseId, Long usuarioId);

    /**
     * @param faseId id de la fase de tutoría
     * @return el recurso PDF de esa fase, para descarga
     * @throws RuntimeException si la fase no existe o no tiene PDF
     */
    Resource obtenerPdfFase(Long faseId);

    /**
     * @param estudianteUsuarioId id del usuario estudiante
     * @return resúmenes de todas las tutorías de ese estudiante
     */
    List<TutoriaResumenDTO> obtenerTutoriasEstudiante(Long estudianteUsuarioId);

    /**
     * @param docenteUsuarioId id del usuario docente
     * @return resúmenes de todas las tutorías a cargo de ese docente
     */
    List<TutoriaResumenDTO> obtenerTutoriasDocente(Long docenteUsuarioId);

    /**
     * Registra el avance de una fase de tutoría vía procedimiento almacenado.
     *
     * @param tutorId     id del registro de tutoría
     * @param numeroFase  número de fase que avanza
     * @param archivoPdf  nombre del archivo PDF asociado al avance
     * @param tamanoBytes tamaño en bytes del archivo
     * @param sha256      hash SHA-256 del archivo, para verificación de integridad posterior
     */
    void registrarAvanceSP(Long tutorId, Integer numeroFase, String archivoPdf, Long tamanoBytes, String sha256);
}
