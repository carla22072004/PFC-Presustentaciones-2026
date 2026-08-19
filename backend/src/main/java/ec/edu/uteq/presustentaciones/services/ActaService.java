package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Acta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ActaService {

    /** RF-11: Genera el acta y crea el PDF real en disco */
    Acta generarActa(Long solicitudId);

    /** RF-08: Firma el acta por un actor específico (PRESIDENTE, VOCAL_1, VOCAL_2, TUTOR) */
    Acta firmarActa(Long actaId, String rol, String observacion);

    /** Retorna el path del PDF generado para descarga */
    byte[] obtenerPdfBytes(Long actaId);

    Page<Acta> listarActas(Pageable pageable);
    Optional<Acta> buscarPorSolicitud(Long solicitudId);
}
