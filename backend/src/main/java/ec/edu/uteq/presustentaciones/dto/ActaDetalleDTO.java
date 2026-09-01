package ec.edu.uteq.presustentaciones.dto;

import ec.edu.uteq.presustentaciones.entities.Acta;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de un acta para las vistas de docente/coordinador/administrador. Incluye el
 * estado actual, las firmas y el tribunal, sin arrastrar el grafo completo de la
 * solicitud (evita ciclos y sobre-serialización).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActaDetalleDTO {

    private Long id;
    private Long solicitudId;
    private String estudianteNombre;
    private String carrera;
    private String tituloTema;
    private String estado;
    private String estadoNombre;
    private LocalDate fechaGeneracion;
    private String observacionesActa;
    private String archivoPdf;

    private boolean firmada;
    private boolean firmadaPresidente;
    private boolean firmadaVocal1;
    private boolean firmadaVocal2;
    private boolean firmadaTutor;
    private LocalDateTime fechaFirmaPresidente;
    private LocalDateTime fechaFirmaVocal1;
    private LocalDateTime fechaFirmaVocal2;
    private LocalDateTime fechaFirmaTutor;
    private String firmantesPendientes;

    private List<MiembroTribunalDTO> tribunal;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiembroTribunalDTO {
        private String docente;
        private String rol;
        private boolean confirmado;
    }

    public static ActaDetalleDTO de(Acta a, List<Jurado> jurados) {
        var sol = a.getSolicitud();
        var est = sol != null ? sol.getEstudiante() : null;
        var usr = est != null ? est.getUsuario() : null;
        List<MiembroTribunalDTO> tribunal = jurados == null ? List.of() : jurados.stream()
                .map(j -> MiembroTribunalDTO.builder()
                        .docente(j.getDocente() != null && j.getDocente().getUsuario() != null
                                ? j.getDocente().getUsuario().getNombre() + " " + j.getDocente().getUsuario().getApellido()
                                : "—")
                        .rol(j.getRol())
                        .confirmado(j.isConfirmado())
                        .build())
                .toList();
        return ActaDetalleDTO.builder()
                .id(a.getId())
                .solicitudId(sol != null ? sol.getId() : null)
                .estudianteNombre(usr != null ? (usr.getNombre() + " " + usr.getApellido()) : "—")
                .carrera(est != null ? est.getCarrera() : "—")
                .tituloTema(sol != null ? sol.getTituloTema() : "—")
                .estado(a.getEstado() != null ? a.getEstado().getCodigo() : null)
                .estadoNombre(a.getEstado() != null ? a.getEstado().getNombre() : null)
                .fechaGeneracion(a.getFechaGeneracion())
                .observacionesActa(a.getObservacionesActa())
                .archivoPdf(a.getArchivoPdf())
                .firmada(a.isFirmada())
                .firmadaPresidente(a.isFirmadaPresidente())
                .firmadaVocal1(a.isFirmadaVocal1())
                .firmadaVocal2(a.isFirmadaVocal2())
                .firmadaTutor(a.isFirmadaTutor())
                .fechaFirmaPresidente(a.getFechaFirmaPresidente())
                .fechaFirmaVocal1(a.getFechaFirmaVocal1())
                .fechaFirmaVocal2(a.getFechaFirmaVocal2())
                .fechaFirmaTutor(a.getFechaFirmaTutor())
                .firmantesPendientes(a.getFirmantesPendientes())
                .tribunal(tribunal)
                .build();
    }
}
