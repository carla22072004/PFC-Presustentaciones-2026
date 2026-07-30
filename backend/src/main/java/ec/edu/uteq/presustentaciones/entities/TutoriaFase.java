package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tutoria_fases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutoriaFase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "tutoriaFases"})
    private Tutor tutor;

    /** Número de fase: 1, 2 o 3 */
    @Column(name = "numero_fase", nullable = false)
    private Integer numeroFase;

    /** Estados: PENDIENTE_ESTUDIANTE | PENDIENTE_TUTOR | APROBADA */
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "PENDIENTE_ESTUDIANTE";

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "archivo_pdf_estudiante")
    private String archivoPdfEstudiante;

    @Column(name = "sha256_pdf", length = 64)
    private String sha256Pdf;

    @Column(name = "tamano_pdf_bytes")
    private Long tamanoPdfBytes;

    @PrePersist
    protected void onCreate() {
        fechaInicio = LocalDateTime.now();
    }
}