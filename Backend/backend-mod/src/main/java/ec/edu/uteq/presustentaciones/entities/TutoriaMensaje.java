package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tutoria_mensajes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutoriaMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fase_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "mensajes"})
    private TutoriaFase fase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "remitente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Usuario remitente;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    /** Tipos: OBSERVACION | RESPUESTA | APROBACION */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "leido", nullable = false)
    @Builder.Default
    private Boolean leido = false;

    @PrePersist
    protected void onCreate() {
        fechaEnvio = LocalDateTime.now();
    }
}