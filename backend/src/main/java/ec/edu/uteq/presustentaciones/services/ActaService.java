package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Acta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ActaService {

    /** RF-11: Genera el acta y crea el PDF real en disco
     * @param solicitudId id de la solicitud a la que pertenece el acta
     * @return el acta existente si ya se había generado, o la recién creada (con su PDF)
     * @throws RuntimeException si la solicitud no existe o no se pudo crear el directorio
     *                          de actas en disco
     */
    Acta generarActa(Long solicitudId);

    /** RF-08: Firma el acta por un actor específico (PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR)
     * @param actaId      id del acta a firmar
     * @param rol         rol que firma ({@code PRESIDENTE}, {@code VOCAL_1}, {@code VOCAL_2}
     *                    o {@code TUTOR}); no distingue mayúsculas/minúsculas
     * @param observacion observación opcional del firmante, o {@code null}
     * @return el acta actualizada; si con esta firma quedan las 4 completas, la solicitud pasa
     *         a "COMPLETADA" y el PDF se regenera con el estado final de las firmas
     * @throws RuntimeException si el acta no existe o {@code rol} no es uno de los 4 válidos
     */
    Acta firmarActa(Long actaId, String rol, String observacion);

    /** Retorna el path del PDF generado para descarga
     * @param actaId id del acta
     * @return los bytes del PDF generado para esa acta
     * @throws RuntimeException si el acta no existe, o si todavía no tiene PDF generado
     */
    byte[] obtenerPdfBytes(Long actaId);

    /**
     * @param pageable configuración de paginación
     * @return página de todas las actas del sistema
     */
    Page<Acta> listarActas(Pageable pageable);

    /**
     * @param solicitudId id de la solicitud
     * @return el acta de esa solicitud, si ya fue generada
     */
    Optional<Acta> buscarPorSolicitud(Long solicitudId);
}
