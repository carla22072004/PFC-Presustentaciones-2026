package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "anteproyectos", schema = "presus")
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

    /**
     * Columna "estado_id" (FK NOT NULL a estados_proceso) heredada del esquema real
     * de la base de datos, en paralelo al campo "estado" (texto) de arriba, que es
     * el que usa toda la lógica de la aplicación. Nada la poblaba, causando un error
     * al subir un anteproyecto ("null value in column estado_id"). Se sincroniza
     * automáticamente a partir de "estado" en @PrePersist/@PreUpdate, mapeando al
     * catálogo estados_proceso (PENDIENTE=1, EN_PROCESO=2, APROBADO=3, OBSERVADO=4,
     * RECHAZADO=5), sin necesidad de tocar los puntos del servicio que la usan.
     */
    @Column(name = "estado_id", nullable = false)
    private Short estadoProcesoId;

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

    @PrePersist
    @PreUpdate
    private void sincronizarEstadoProceso() {
        if (estado == null) {
            estadoProcesoId = 1; // PENDIENTE
            return;
        }
        estadoProcesoId = switch (estado) {
            case "ENVIADO" -> (short) 2;   // EN_PROCESO
            case "APROBADO" -> (short) 3;
            case "OBSERVADO" -> (short) 4;
            case "RECHAZADO" -> (short) 5;
            default -> (short) 1;          // PENDIENTE
        };
    }
}
