package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Ruta de titulación / checklist de cada estudiante. {@code pasosJson} guarda el
 * estado de los pasos como un objeto JSON {"clave_paso": true/false} — el catálogo
 * de pasos lo define el servicio, aquí solo se persiste qué marcó el estudiante.
 * Se mapea como String (mismo criterio que Auditoria) y el servicio lo convierte.
 */
@Entity
@Table(name = "progreso_estudiante", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgresoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Estudiante estudiante;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pasos_json", columnDefinition = "jsonb", nullable = false)
    private String pasosJson;
}
