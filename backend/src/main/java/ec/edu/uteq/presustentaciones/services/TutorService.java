package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.MiEstudianteTutoradoDTO;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TutorService {

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
    Optional<Tutor> buscarPorSolicitud(Long solicitudId);

    /**
     * @param pageable configuración de paginación
     * @return página de todos los registros de tutoría del sistema
     */
    Page<Tutor> listarTodos(Pageable pageable);

    /** @param tutorId id del registro de tutoría a eliminar */
    void eliminarTutor(Long tutorId);

    /**
     * Invoca el procedimiento almacenado de estadísticas de carga de tutores.
     *
     * @return una fila por docente con su carga actual de tutorías
     */
    List<Map<String, Object>> obtenerEstadisticasTutoresSP();

    /**
     * @param usuarioIdDocente id del usuario docente
     * @return los estudiantes tutorados actualmente por ese docente
     */
    List<MiEstudianteTutoradoDTO> misEstudiantes(Long usuarioIdDocente);
}
