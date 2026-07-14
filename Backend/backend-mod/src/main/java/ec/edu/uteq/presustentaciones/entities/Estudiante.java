package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // IMPORTANTE: Importar esto
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estudiante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Añadimos esto a nivel de clase para que Jackson ignore los proxies de Hibernate
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;

    @Column(name = "carrera", nullable = false, length = 180)
    private String carrera;

    @Column(name = "semestre", length = 30)
    private String semestre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "expediente_codigo", unique = true, length = 60)
    private String expedienteCodigo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}