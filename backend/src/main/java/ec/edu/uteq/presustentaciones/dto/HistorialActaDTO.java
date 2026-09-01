package ec.edu.uteq.presustentaciones.dto;

import ec.edu.uteq.presustentaciones.entities.HistorialEstadoActa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Una entrada del timeline de trazabilidad de un acta. Aplana
 * {@link HistorialEstadoActa} a lo que el timeline del frontend necesita:
 * quién (email + nombre + rol), qué acción, transición de estado y motivo.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialActaDTO {

    private Long id;
    private Long actaId;
    private String accion;
    private String estadoAnterior;
    private String estadoNuevo;
    private String usuarioEmail;
    private String usuarioNombre;
    private String rolUsuario;
    private String comentario;
    private LocalDateTime fecha;

    public static HistorialActaDTO de(HistorialEstadoActa h) {
        var u = h.getUsuario();
        return HistorialActaDTO.builder()
                .id(h.getId())
                .actaId(h.getActa() != null ? h.getActa().getId() : null)
                .accion(h.getAccion())
                .estadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().getCodigo() : null)
                .estadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().getCodigo() : null)
                .usuarioEmail(u != null ? u.getEmail() : null)
                .usuarioNombre(u != null ? (u.getNombre() + " " + u.getApellido()) : "Sistema")
                .rolUsuario(h.getRolUsuario())
                .comentario(h.getComentario())
                .fecha(h.getFechaCambio())
                .build();
    }
}
