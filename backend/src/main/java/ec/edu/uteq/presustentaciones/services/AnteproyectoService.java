package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;

public interface AnteproyectoService {

    /**
     * Sube el PDF del anteproyecto de una solicitud, calcula y persiste su hash SHA-256, y
     * deja el anteproyecto en estado pendiente de revisión.
     *
     * @param solicitudId id de la solicitud a la que pertenece el anteproyecto
     * @param archivo     archivo PDF subido por el estudiante
     * @return el anteproyecto creado o actualizado
     * @throws RuntimeException si la solicitud no existe o el archivo no es un PDF válido
     */
    Anteproyecto enviarAnteproyecto(Long solicitudId, MultipartFile archivo);

    /**
     * @param id            id del anteproyecto a aprobar
     * @param observaciones observaciones opcionales del revisor
     * @return el anteproyecto actualizado en estado "APROBADO"
     * @throws RuntimeException si el anteproyecto no existe
     */
    Anteproyecto aprobarAnteproyecto(Long id, String observaciones);

    /**
     * @param id            id del anteproyecto a rechazar
     * @param observaciones motivo del rechazo
     * @return el anteproyecto actualizado en estado "RECHAZADO"
     * @throws RuntimeException si el anteproyecto no existe
     */
    Anteproyecto rechazarAnteproyecto(Long id, String observaciones);

    /**
     * @param solicitudId id de la solicitud
     * @return el anteproyecto de esa solicitud, si ya fue enviado
     */
    Optional<Anteproyecto> buscarPorSolicitud(Long solicitudId);

    /** RF-02: Verifica que el archivo en disco coincida con el hash SHA-256 almacenado
     * @param solicitudId id de la solicitud cuyo anteproyecto se va a verificar
     * @return {@code true} si el hash SHA-256 del archivo en disco coincide con el
     *         almacenado en base de datos (comparación con {@code MessageDigest.isEqual},
     *         resistente a ataques de timing)
     * @throws RuntimeException si la solicitud no tiene anteproyecto o el archivo no existe
     *                          en disco
     */
    boolean verificarIntegridad(Long solicitudId);
}
