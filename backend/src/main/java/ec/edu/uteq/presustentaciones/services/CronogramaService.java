package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CronogramaService {

    /**
     * Programa la defensa de una solicitud en una sala y horario específicos, validando
     * primero que el tribunal esté completo y la tutoría completada, y luego que ningún
     * jurado tenga conflicto de horario (vía {@code sp_validar_conflicto_jurado}).
     *
     * @param solicitudId id de la solicitud a programar
     * @param salaId      id de la sala donde se realizará la defensa
     * @param fecha       fecha de la defensa
     * @param hora        hora de inicio de la defensa
     * @return el cronograma creado, en estado "PROGRAMADO"
     * @throws RuntimeException si el tribunal no está completo, la tutoría no está
     *                          completada, o algún jurado tiene conflicto de horario
     */
    Cronograma crearCronograma(Long solicitudId, Long salaId, LocalDate fecha, LocalTime hora);

    /** RF-04: Asignación automática sin conflictos
     * @param solicitudId id de la solicitud a programar
     * @return el cronograma creado con la primera sala/franja libre encontrada
     * @throws RuntimeException si no hay ninguna combinación de sala/franja disponible
     */
    Cronograma asignarAutomatico(Long solicitudId);

    /**
     * @param pageable configuración de paginación
     * @return página de todos los cronogramas del sistema
     */
    Page<Cronograma> listarCronogramas(Pageable pageable);

    /**
     * @param estudianteId id del estudiante
     * @return los cronogramas de las solicitudes de ese estudiante
     */
    List<Cronograma> listarPorEstudiante(Long estudianteId);

    /**
     * @param usuarioId id del usuario autenticado
     * @return los cronogramas visibles para ese usuario (como estudiante o como jurado/tutor)
     */
    List<Cronograma> listarPorUsuario(Long usuarioId);

    /**
     * @param solicitudId id de la solicitud
     * @return el cronograma de esa solicitud, si ya fue programada
     */
    Optional<Cronograma> buscarPorSolicitud(Long solicitudId);

    /** @param id id del cronograma a eliminar */
    void eliminar(Long id);

    /** RF-04: Verificar disponibilidad de sala en franja horaria
     * @param salaId      id de la sala a verificar
     * @param inicio      instante de inicio de la franja propuesta
     * @param duracionMin duración de la defensa en minutos
     * @return {@code true} si la sala está libre en toda esa franja, {@code false} si se
     *         solapa con otro cronograma ya programado
     */
    boolean estaDisponible(Long salaId, java.time.LocalDateTime inicio, int duracionMin);

    /** RF-04: Franjas libres para una fecha
     * @param fecha       fecha sobre la que buscar franjas libres
     * @param duracionMin duración de la defensa en minutos
     * @return lista de instantes de inicio disponibles en cualquier sala esa fecha
     */
    List<java.time.LocalDateTime> franjasDisponibles(LocalDate fecha, int duracionMin);
}
