package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Acta;

import java.util.List;
import java.util.Optional;

public interface ActaService {

    /** RF-11: Genera el acta y crea el PDF real en disco */
    Acta generarActa(Long solicitudId);

    /** RF-08: Firma el acta por un actor específico (PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR) */
    Acta firmarActa(Long actaId, String rol);

    /** Retorna el path del PDF generado para descarga */
    byte[] obtenerPdfBytes(Long actaId);

    List<Acta> listarActas();
    Optional<Acta> buscarPorSolicitud(Long solicitudId);
}
