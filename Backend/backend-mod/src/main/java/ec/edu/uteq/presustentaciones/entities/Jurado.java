package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jurados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jurado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "docente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jurados", "tutores"})
    private Docente docente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jurados", "tutor", "evaluacion", "acta", "anteproyecto", "cronograma", "notificaciones"})
    private Solicitud solicitud;

    /** Roles posibles: PRESIDENTE, VOCAL_1, VOCAL_2 */
    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "confirmado", nullable = false)
    @Builder.Default
    private boolean confirmado = false;

    @Column(name = "asignado_en", nullable = false, updatable = false)
    private LocalDateTime asignadoEn;

    @PrePersist
    protected void onCreate() {
        asignadoEn = LocalDateTime.now();
    }
}
