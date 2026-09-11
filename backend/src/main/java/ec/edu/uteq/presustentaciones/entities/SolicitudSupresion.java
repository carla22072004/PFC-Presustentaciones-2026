package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RNF-19: solicitud de supresión de datos personales a instancia del titular. Deliberadamente
 * NO guarda el dato suprimido (solo referencia el id del usuario, que sigue existiendo,
 * seudonimizado -- ver {@code UsuarioServiceImpl#seudonimizar}).
 */
@Entity
@Table(name = "solicitud_supresion", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudSupresion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    /** PENDIENTE | RESUELTA | RECHAZADA */
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    /** SEUDONIMIZACION | RECHAZADA -- null mientras esté PENDIENTE. */
    @Column(name = "tipo_resolucion", length = 20)
    private String tipoResolucion;

    @Column(name = "resuelto_por")
    private Long resueltoPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;
}
