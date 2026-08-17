package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // IMPORTANTE: Importar esto
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * sp_generar_codigo_expediente (backend/src/main/resources/db/migration/
 * V3__stored_procedures_validacion_y_codigos.sql) es un PROCEDURE de Postgres (no FUNCTION)
 * con un parámetro INOUT para el valor de retorno escalar -- Hibernate invoca @Procedure vía
 * la sintaxis JDBC "{call proc(?, ?)}", que Postgres solo acepta para PROCEDURE (una FUNCTION
 * con RETURNS<tipo> rechaza CALL con "is not a procedure. Hint: To call a function, use
 * SELECT", sin importar cuántos parámetros se declaren) -- bugs reales encontrados probando
 * el endpoint en vivo contra Docker. Fase 3 / Criterio P1.
 */
@NamedStoredProcedureQuery(
        name = "Estudiante.generarCodigoExpediente",
        procedureName = "presus.sp_generar_codigo_expediente",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_anio", type = Integer.class),
                @StoredProcedureParameter(mode = ParameterMode.INOUT, name = "p_codigo", type = String.class)
        }
)
@Entity
@Table(name = "estudiante", schema = "presus")
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "carrera_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Carrera carreraEntidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "periodo_ingreso_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PeriodoAcademico periodoIngreso;

    @Column(name = "semestre_actual", nullable = false)
    private Short semestreActual;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}