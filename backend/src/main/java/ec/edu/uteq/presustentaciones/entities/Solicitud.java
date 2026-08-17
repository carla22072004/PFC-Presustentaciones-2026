package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * sp_generar_reporte_defensas (backend/src/main/resources/db/migration/V2__stored_procedures.sql)
 * arma el reporte consolidado de defensas por carrera cruzando solicitud/estudiante/usuarios/
 * cronograma/sala/evaluaciones -- invocado vía SolicitudRepository (JPA 2.1
 * @NamedStoredProcedureQuery, Fase 3 / Criterio P1).
 */
@NamedStoredProcedureQuery(
        name = "Solicitud.generarReporteDefensas",
        procedureName = "presus.sp_generar_reporte_defensas",
        resultSetMappings = "ReporteDefensaMapping",
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_carrera", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, name = "p_resultado", type = void.class)
        }
)
@SqlResultSetMapping(
        name = "ReporteDefensaMapping",
        classes = @ConstructorResult(
                targetClass = ec.edu.uteq.presustentaciones.dto.ReporteDefensaResult.class,
                columns = {
                        @ColumnResult(name = "solicitud_id", type = Long.class),
                        @ColumnResult(name = "estudiante_nombre", type = String.class),
                        @ColumnResult(name = "expediente", type = String.class),
                        @ColumnResult(name = "titulo_tema", type = String.class),
                        @ColumnResult(name = "estado_solicitud", type = String.class),
                        @ColumnResult(name = "fecha_defensa", type = LocalDateTime.class),
                        @ColumnResult(name = "sala_nombre", type = String.class),
                        @ColumnResult(name = "nota_final", type = Double.class)
                }
        )
)
@Entity
@Table(name = "solicitud", schema = "presus")
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ConvocatoriaTitulacion convocatoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modalidad_titulacion_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ModalidadTitulacion modalidadTitulacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linea_investigacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private LineaInvestigacion lineaInvestigacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_tematica_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AreaTematica areaTematica;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EstadoSolicitud estado;

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
