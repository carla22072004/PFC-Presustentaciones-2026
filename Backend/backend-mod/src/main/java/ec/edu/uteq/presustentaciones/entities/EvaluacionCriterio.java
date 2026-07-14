package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registra la nota que un JURADO asigna a UN criterio de la rúbrica
 * para una solicitud específica.
 * Escala: 1-100 (% del puntaje máximo del criterio)
 * Rangos de observación: 67-100 (Alto), 34-66 (Medio), 1-33 (Bajo)
 */
@Entity
@Table(name = "evaluaciones_criterio",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"solicitud_id", "jurado_id", "criterio_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluacionCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Escala aplicada: 1-100 (% del puntaje máximo del criterio) */
    @Column(name = "escala", nullable = false)
    private Integer escala;

    /** Nota calculada = criterio.ponderacion * escala / 100  (sobre la base del criterio) */
    @Column(name = "nota_obtenida", nullable = false)
    private Double notaObtenida;

    /** Observación automática según el rango de la escala */
    @Column(name = "observacion_auto", columnDefinition = "TEXT")
    private String observacionAuto;

    /** Observación manual ingresada por el jurado */
    @Column(name = "observacion_manual", columnDefinition = "TEXT")
    private String observacionManual;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "registrado_en", nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "jurados", "tutor",
                           "evaluacion", "acta", "anteproyecto", "cronograma", "notificaciones"})
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jurado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "solicitud"})
    private Jurado jurado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "criterio_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "rubrica"})
    private CriterioRubrica criterio;

    @PrePersist
    protected void onCreate() {
        registradoEn = LocalDateTime.now();
    }

    public static String getObservacionPorRango(int escala) {
        if (escala >= 67) {
            return "Excelente. Cumple satisfactoriamente con los requisitos y objetivos del criterio establecido.";
        } else if (escala >= 34) {
            return "Aceptable. Presenta algunas deficiencias que requieren corrección o mejora.";
        } else {
            return "Deficiente. No cumple con los requisitos mínimos del criterio. Se evidencian falencias significativas.";
        }
    }

    public static String getRangoDescripcion(int escala) {
        if (escala >= 67) {
            return "ALTO";
        } else if (escala >= 34) {
            return "MEDIO";
        } else {
            return "BAJO";
        }
    }
}
