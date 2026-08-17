package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Proyección del resultado de sp_generar_reporte_defensas (función SQL, RETURNS TABLE),
 * invocada vía @NamedStoredProcedureQuery + @SqlResultSetMapping desde SolicitudRepository.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDefensaResult {
    private Long solicitudId;
    private String estudianteNombre;
    private String expediente;
    private String tituloTema;
    private String estadoSolicitud;
    private LocalDateTime fechaDefensa;
    private String salaNombre;
    private Double notaFinal;
}
