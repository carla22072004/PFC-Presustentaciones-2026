package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tutores", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "docente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jurados", "tutores"})
    private Docente docente;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jurados", "tutor", "evaluacion", "acta", "anteproyecto", "cronograma", "notificaciones"})
    private Solicitud solicitud;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    /** Estado de la tutoría: ACTIVO, COMPLETADA, FINALIZADO, REEMPLAZADO */
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    /**
     * Columna "estado_id" (FK NOT NULL a estados_proceso) heredada del esquema real,
     * en paralelo a "estado" (texto), que es el que usa la lógica de la aplicación.
     * Se sincroniza automáticamente a partir de "estado" (mismo patrón aplicado en
     * Solicitud.java y Anteproyecto.java para el mismo problema).
     */
    @Column(name = "estado_id", nullable = false)
    private Short estadoProcesoId;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
        sincronizarEstadoProceso();
    }

    @PreUpdate
    protected void onUpdate() {
        sincronizarEstadoProceso();
    }

    private void sincronizarEstadoProceso() {
        estadoProcesoId = switch (estado) {
            case "ACTIVO" -> (short) 2;                         // EN_PROCESO
            case "COMPLETADA", "FINALIZADO" -> (short) 3;       // APROBADO
            case "REEMPLAZADO" -> (short) 5;                    // RECHAZADO
            default -> (short) 1;                               // PENDIENTE
        };
    }
}
