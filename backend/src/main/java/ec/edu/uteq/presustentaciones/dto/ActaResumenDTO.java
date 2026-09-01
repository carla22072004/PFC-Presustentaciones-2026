package ec.edu.uteq.presustentaciones.dto;

import ec.edu.uteq.presustentaciones.entities.Acta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Vista de lista de un acta (para "Mis actas" del docente y la gestión del
 * administrador). No expone la solicitud completa ni el árbol de firmantes:
 * solo lo necesario para la tabla + el enlace al detalle.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActaResumenDTO {

    private Long id;
    private Long solicitudId;
    private String estudianteNombre;
    private String carrera;
    private String tituloTema;
    private String estado;
    private String estadoNombre;
    private LocalDate fechaGeneracion;
    private boolean firmada;
    private String firmantesPendientes;

    public static ActaResumenDTO de(Acta a) {
        var sol = a.getSolicitud();
        var est = sol != null ? sol.getEstudiante() : null;
        var usr = est != null ? est.getUsuario() : null;
        return ActaResumenDTO.builder()
                .id(a.getId())
                .solicitudId(sol != null ? sol.getId() : null)
                .estudianteNombre(usr != null ? (usr.getNombre() + " " + usr.getApellido()) : "—")
                .carrera(est != null ? est.getCarrera() : "—")
                .tituloTema(sol != null ? sol.getTituloTema() : "—")
                .estado(a.getEstado() != null ? a.getEstado().getCodigo() : null)
                .estadoNombre(a.getEstado() != null ? a.getEstado().getNombre() : null)
                .fechaGeneracion(a.getFechaGeneracion())
                .firmada(a.isFirmada())
                .firmantesPendientes(a.getFirmantesPendientes())
                .build();
    }
}
