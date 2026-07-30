package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Cronograma;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CronogramaService {
    Cronograma crearCronograma(Long solicitudId, Long salaId, LocalDate fecha, LocalTime hora);
    /** RF-04: Asignación automática sin conflictos */
    Cronograma asignarAutomatico(Long solicitudId);
    List<Cronograma> listarCronogramas();
    List<Cronograma> listarPorEstudiante(Long estudianteId);
    List<Cronograma> listarPorUsuario(Long usuarioId);
    Optional<Cronograma> buscarPorSolicitud(Long solicitudId);
    void eliminar(Long id);
    /** RF-04: Verificar disponibilidad de sala en franja horaria */
    boolean estaDisponible(Long salaId, java.time.LocalDateTime inicio, int duracionMin);
    /** RF-04: Franjas libres para una fecha */
    List<java.time.LocalDateTime> franjasDisponibles(LocalDate fecha, int duracionMin);
}
