package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trazabilidad persistente de las transiciones de estado de un acta
 * (V19__historial_actas_y_reportes.sql). Análoga a {@link HistorialEstadosSolicitud},
 * ampliada con {@code accion} y {@code rolUsuario} para el timeline del requerimiento.
 * La escribe {@code ActaServiceImpl} en cada cambio; la auditoría genérica de V15
 * (trigger sobre presus.actas) queda como respaldo a nivel de base.
 */
@Entity
@Table(name = "historial_estados_acta", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstadoActa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acta_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Acta acta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_anterior_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EstadoActa estadoAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EstadoActa estadoNuevo;

    /** Autor del cambio. {@code null} solo para los registros históricos sembrados por V19. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Usuario usuario;

    /** Rol con el que actuó el usuario (ADMIN / COORDINADOR / DOCENTE), copiado al momento del cambio. */
    @Column(name = "rol_usuario", length = 30)
    private String rolUsuario;

    /** CREAR, CAMBIO_ESTADO, FIRMA_COMPLETA, ... */
    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCambio = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }
}
