package ec.edu.uteq.presustentaciones.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * Guía o recurso del Centro de Titulación (formatos, plantillas, reglamentos).
 * {@code carrera} nulo = recurso general, visible para todas las carreras.
 */
@Entity
@Table(name = "recursos_titulacion", schema = "presus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecursoTitulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Carrera carrera;
}
