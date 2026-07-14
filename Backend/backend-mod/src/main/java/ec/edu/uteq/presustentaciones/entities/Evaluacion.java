package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    // ── Notas desagregadas ───────────────────────────────────────────────────
    /** Nota asignada por el instructor del curso (ponderación default 60%) */
    @Column(name = "nota_instructor")
    private Double notaInstructor;

    /** Nota asignada por el tribunal/jurado (ponderación default 40%) */
    @Column(name = "nota_jurado")
    private Double notaJurado;

    /** Ponderación del instructor en %, default 60 */
    @Column(name = "peso_instructor", nullable = false)
    @Builder.Default
    private Double pesoInstructor = 60.0;

    /** Ponderación del jurado en %, default 40 */
    @Column(name = "peso_jurado", nullable = false)
    @Builder.Default
    private Double pesoJurado = 40.0;

    /** Nota final calculada = (notaInstructor * pesoInstructor/100) + (notaJurado * pesoJurado/100) */
    @Column(name = "nota_final")
    private Double notaFinal;

    /** Valores posibles: APROBADO, REPROBADO */
    @Column(name = "resultado", length = 20)
    private String resultado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "comentario_preestablecido", columnDefinition = "TEXT")
    private String comentarioPreestablecido;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "creadoPor", "actualizadoPor"})
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rubrica_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Rubrica rubrica;

    // ── Método helper para calcular nota final ───────────────────────────────
    public void calcularNotaFinal() {
        if (notaInstructor != null && notaJurado != null) {
            this.notaFinal = (notaInstructor * pesoInstructor / 100.0)
                           + (notaJurado * pesoJurado / 100.0);
            // Escala sobre 10
            this.notaFinal = Math.round(this.notaFinal * 100.0) / 100.0;
            this.resultado = this.notaFinal >= 7.0 ? "APROBADO" : "REPROBADO";
        }
    }
}
