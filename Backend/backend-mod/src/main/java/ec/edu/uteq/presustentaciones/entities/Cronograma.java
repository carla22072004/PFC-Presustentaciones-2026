package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cronograma")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cronograma {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "creadoPor", "actualizadoPor"})
    private Solicitud solicitud;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Sala sala;
    
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;
    
    @Column(name = "duracion_min", nullable = false)
    private Integer duracionMin = 45;
    
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ACTIVO";
    
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    
    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
    
    public LocalDateTime getFechaFin() {
        return fechaInicio.plusMinutes(duracionMin);
    }
}
