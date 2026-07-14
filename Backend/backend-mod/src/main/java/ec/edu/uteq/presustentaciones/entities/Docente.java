package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "docente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Docente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;
    
    @Column(name = "area_especialidad", length = 180)
    private String areaEspecialidad;
    
    @Column(name = "carga_horaria_semanal", nullable = false)
    private Integer cargaHorariaSemanal = 0;
    
    @Column(name = "disponible", nullable = false)
    private Boolean disponible = true;
    
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    
    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}
