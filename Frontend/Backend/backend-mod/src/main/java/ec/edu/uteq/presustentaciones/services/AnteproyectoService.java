package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;

public interface AnteproyectoService {
    Anteproyecto enviarAnteproyecto(Long solicitudId, MultipartFile archivo);
    Anteproyecto aprobarAnteproyecto(Long id, String observaciones);
    Anteproyecto rechazarAnteproyecto(Long id, String observaciones);
    Optional<Anteproyecto> buscarPorSolicitud(Long solicitudId);
    /** RF-02: Verifica que el archivo en disco coincida con el hash SHA-256 almacenado */
    boolean verificarIntegridad(Long solicitudId);
}
