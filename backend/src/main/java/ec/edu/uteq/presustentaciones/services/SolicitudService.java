package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Solicitud;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SolicitudService {
    Solicitud crearSolicitud(Long estudianteId, Solicitud datos);
    Solicitud crearSolicitudPorUsuario(Long usuarioId, Solicitud datos);
    Solicitud enviarSolicitud(Long solicitudId);
    Solicitud aprobarSolicitud(Long solicitudId);
    Solicitud rechazarSolicitud(Long solicitudId);
    Solicitud rechazarConObservacion(Long solicitudId, String observacion);
    List<Solicitud> listarSolicitudes();
    Page<Solicitud> listarSolicitudesPaginado(int pagina, int tamanio, String estado);
    Map<String, Long> contarPorEstado();
    List<Solicitud> listarPorEstudiante(Long estudianteId);
    List<Solicitud> listarPorUsuario(Long usuarioId);
    Optional<Solicitud> obtenerPorId(Long id);
    Solicitud suspenderSolicitud(Long solicitudId, String motivo);
    List<Map<String, Object>> generarReporteDefensasSP(String carrera);
}