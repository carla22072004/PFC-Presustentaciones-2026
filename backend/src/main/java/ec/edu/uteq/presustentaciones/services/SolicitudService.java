package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Solicitud;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SolicitudService {

    /**
     * Crea una nueva solicitud de pre-sustentación para un estudiante ya existente, en estado
     * inicial "CREADA".
     *
     * @param estudianteId id del {@code Estudiante} propietario de la solicitud
     * @param datos        datos de la solicitud a crear (título del tema, modalidad, etc.)
     * @return la solicitud creada y persistida, con su estado y estudiante asociados
     * @throws RuntimeException si el estudiante no existe o falta la modalidad de titulación
     */
    Solicitud crearSolicitud(Long estudianteId, Solicitud datos);

    /**
     * Crea una solicitud a partir del usuario autenticado, creando automáticamente su perfil de
     * {@code Estudiante} (vía {@code sp_generar_codigo_expediente}) si todavía no existe.
     *
     * @param usuarioId id del {@code Usuario} autenticado (rol ESTUDIANTE)
     * @param datos     datos de la solicitud a crear
     * @return la solicitud creada
     * @throws RuntimeException si el usuario no existe, no tiene rol ESTUDIANTE, o no hay
     *                          carreras configuradas para crear el perfil automáticamente
     */
    Solicitud crearSolicitudPorUsuario(Long usuarioId, Solicitud datos);

    /**
     * Transiciona una solicitud de "CREADA" a "ENVIADA", validando que ya tenga un anteproyecto
     * en PDF adjunto.
     *
     * @param solicitudId id de la solicitud a enviar
     * @return la solicitud actualizada en estado "ENVIADA"
     * @throws RuntimeException si la solicitud no existe o no tiene anteproyecto adjunto
     */
    Solicitud enviarSolicitud(Long solicitudId);

    /**
     * Aprueba una solicitud enviada, transicionándola a estado "APROBADA".
     *
     * @param solicitudId id de la solicitud a aprobar
     * @return la solicitud actualizada
     */
    Solicitud aprobarSolicitud(Long solicitudId);

    /**
     * Rechaza una solicitud sin registrar un motivo explícito.
     *
     * @param solicitudId id de la solicitud a rechazar
     * @return la solicitud actualizada en estado "RECHAZADA"
     */
    Solicitud rechazarSolicitud(Long solicitudId);

    /**
     * Rechaza una solicitud registrando el motivo del rechazo en sus observaciones.
     *
     * @param solicitudId  id de la solicitud a rechazar
     * @param observacion  motivo del rechazo, visible luego para el estudiante
     * @return la solicitud actualizada en estado "RECHAZADA" con la observación guardada
     */
    Solicitud rechazarConObservacion(Long solicitudId, String observacion);

    /** @return todas las solicitudes del sistema, sin paginar */
    List<Solicitud> listarSolicitudes();

    /**
     * Lista solicitudes con paginación y filtros opcionales, usada por la vista administrativa
     * de gestión de solicitudes (evita cargar el dataset completo, ver hallazgo real documentado
     * en la memoria del proyecto sobre {@code /solicitudes} sin paginar).
     *
     * @param pagina       número de página, base 0
     * @param tamanio      tamaño de página
     * @param estado       código de estado por el que filtrar, o {@code null} para no filtrar
     * @param texto        texto libre de búsqueda (título/estudiante), o {@code null}
     * @param fechaDesde   fecha mínima de registro, o {@code null} para no acotar
     * @param fechaHasta   fecha máxima de registro, o {@code null} para no acotar
     * @return página de solicitudes que cumplen los filtros
     */
    Page<Solicitud> listarSolicitudesPaginado(int pagina, int tamanio, String estado, String texto,
                                               LocalDate fechaDesde, LocalDate fechaHasta);

    /** @return conteo de solicitudes agrupado por código de estado, para el dashboard */
    Map<String, Long> contarPorEstado();

    /**
     * @param estudianteId id del estudiante
     * @return todas las solicitudes registradas por ese estudiante
     */
    List<Solicitud> listarPorEstudiante(Long estudianteId);

    /**
     * @param usuarioId id del usuario (se resuelve a su perfil de estudiante internamente)
     * @return las solicitudes del estudiante asociado a ese usuario, o lista vacía si no tiene
     *         perfil de estudiante todavía
     */
    List<Solicitud> listarPorUsuario(Long usuarioId);

    /**
     * @param id id de la solicitud
     * @return la solicitud si existe, o {@link Optional#empty()} en caso contrario
     */
    Optional<Solicitud> obtenerPorId(Long id);

    /**
     * Suspende una solicitud que ya está en trámite (no permitido si está en "CREADA",
     * "RECHAZADA" o ya "SUSPENDIDA"), registrando el motivo y la fecha de suspensión.
     *
     * @param solicitudId id de la solicitud a suspender
     * @param motivo      motivo de la suspensión; no puede estar vacío
     * @return la solicitud actualizada en estado "SUSPENDIDA"
     * @throws RuntimeException si la solicitud no existe, su estado actual no permite
     *                          suspensión, o el motivo está vacío
     */
    Solicitud suspenderSolicitud(Long solicitudId, String motivo);

    /**
     * Invoca {@code sp_generar_reporte_defensas} (procedimiento almacenado, Criterio P1) para
     * obtener el reporte consolidado de defensas de una carrera, cruzando solicitud, estudiante,
     * cronograma, sala y evaluación.
     *
     * @param carrera nombre (o coincidencia parcial, {@code ILIKE}) de la carrera a filtrar
     * @return una fila por defensa, con las claves declaradas en
     *         {@code docs/basedatos/CATALOGO-SP.md} (solicitudId, estudianteNombre, expediente,
     *         tituloTema, estadoSolicitud, fechaDefensa, salaNombre, notaFinal)
     */
    List<Map<String, Object>> generarReporteDefensasSP(String carrera);
}