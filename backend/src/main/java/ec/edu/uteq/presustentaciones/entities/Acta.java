package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(name = "archivo_pdf")
    private String archivoPdf;

    // ── Firma multi-actor (RF-08) ─────────────────────────────────────────────

    /** ¿Ha firmado el presidente del jurado? */
    @Column(name = "firmada_presidente", nullable = false)
    @Builder.Default
    private boolean firmadaPresidente = false;

    @Column(name = "fecha_firma_presidente")
    private LocalDateTime fechaFirmaPresidente;

    /** ¿Ha firmado el vocal 1? */
    @Column(name = "firmada_vocal1", nullable = false)
    @Builder.Default
    private boolean firmadaVocal1 = false;

    @Column(name = "fecha_firma_vocal1")
    private LocalDateTime fechaFirmaVocal1;

    /** ¿Ha firmado el vocal 2? */
    @Column(name = "firmada_vocal2", nullable = false)
    @Builder.Default
    private boolean firmadaVocal2 = false;

    @Column(name = "fecha_firma_vocal2")
    private LocalDateTime fechaFirmaVocal2;

    /** ¿Ha firmado el tutor? */
    @Column(name = "firmada_tutor", nullable = false)
    @Builder.Default
    private boolean firmadaTutor = false;

    @Column(name = "fecha_firma_tutor")
    private LocalDateTime fechaFirmaTutor;

    /** true solo cuando TODOS los actores requeridos han firmado */
    @Column(name = "firmada", nullable = false)
    @Builder.Default
    private boolean firmada = false;

    @Column(name = "observaciones_acta", columnDefinition = "TEXT")
    private String observacionesActa;

    @OneToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    // ── Helper ───────────────────────────────────────────────────────────────
    /** El acta queda totalmente firmada cuando presidente + ambos vocales + tutor firmaron */
    public void actualizarEstadoFirma() {
        this.firmada = firmadaPresidente && firmadaVocal1 && firmadaVocal2 && firmadaTutor;
    }

    /** Retorna los firmantes pendientes como texto */
    public String getFirmantesPendientes() {
        StringBuilder sb = new StringBuilder();
        if (!firmadaPresidente) sb.append("Presidente, ");
        if (!firmadaVocal1)     sb.append("Vocal 1, ");
        if (!firmadaVocal2)     sb.append("Vocal 2, ");
        if (!firmadaTutor)      sb.append("Tutor, ");
        String result = sb.toString();
        return result.isEmpty() ? "Todos firmaron" : result.substring(0, result.length() - 2);
    }
}
