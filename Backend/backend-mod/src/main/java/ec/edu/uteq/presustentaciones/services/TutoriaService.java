package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.TutoriaFaseDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaMensajeDTO;
import ec.edu.uteq.presustentaciones.dto.TutoriaResumenDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TutoriaService {

    TutoriaResumenDTO obtenerResumen(Long tutorId, Long usuarioId);

    List<TutoriaFaseDTO> obtenerFases(Long tutorId);

    TutoriaFaseDTO crearFaseConObservacion(Long tutorId, Long tutorUsuarioId, String observacion);

    TutoriaFaseDTO subirPdfCorregido(Long faseId, MultipartFile archivo, Long estudianteUsuarioId);

    TutoriaFaseDTO aprobarFase(Long faseId, Long tutorUsuarioId, String comentario);

    TutoriaMensajeDTO enviarMensaje(Long faseId, Long remitenteId, String contenido, String tipo);

    void marcarMensajesLeidos(Long faseId, Long usuarioId);

    Resource obtenerPdfFase(Long faseId);

    List<TutoriaResumenDTO> obtenerTutoriasEstudiante(Long estudianteUsuarioId);

    List<TutoriaResumenDTO> obtenerTutoriasDocente(Long docenteUsuarioId);
}
