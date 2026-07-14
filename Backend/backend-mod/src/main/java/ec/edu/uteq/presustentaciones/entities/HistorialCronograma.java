package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_cronograma", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialCronograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cronograma_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Cronograma cronograma;

    @Column(name = "fecha_anterior", nullable = false)
    private LocalDateTime fechaAnterior;

    @Column(name = "fecha_nueva", nullable = false)
    private LocalDateTime fechaNueva;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_anterior_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sala salaAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_nueva_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sala salaNueva;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private Usuario usuario;

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
