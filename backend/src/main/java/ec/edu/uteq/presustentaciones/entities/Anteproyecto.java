package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "anteproyectos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Anteproyecto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "archivo_pdf")
    private String archivoPdf;

    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio;

    /** Estados: ENVIADO, APROBADO, RECHAZADO */
    @Column(name = "estado", length = 30)
    private String estado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /** RF-02: Hash SHA-256 del archivo para verificación de integridad */
    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @OneToOne
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;
}
