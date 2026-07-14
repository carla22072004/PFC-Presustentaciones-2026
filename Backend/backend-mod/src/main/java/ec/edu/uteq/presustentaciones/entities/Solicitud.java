package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ec.edu.uteq.presustentaciones.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudiante_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Estudiante estudiante;
    
    @Column(name = "titulo_tema", nullable = false, length = 300)
    private String tituloTema;
    
    @Column(name = "modalidad", length = 120)
    private String modalidad;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
    
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
    
    // Trazabilidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    @JsonIgnore
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    @JsonIgnore
    private Usuario actualizadoPor;
    
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.CREADA;

    @Column(name = "motivo_suspension", columnDefinition = "TEXT")
    private String motivoSuspension;

    @Column(name = "suspendido_en")
    private LocalDateTime suspendidoEn;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
