package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionesSolicitudDTO {
    private Long solicitudId;
    private String tituloTema;
    private String nombreEstudiante;
    private ObservacionesTutorDTO tutor;
    private List<ObservacionesJuradoDTO> jurados;
    private ObservacionesCoordinadorDTO coordinador;

    @Data @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservacionesTutorDTO {
        private Long tutorId;
        private String nombreTutor;
        private String observaciones;
        private String fechaRegistro;
    }

    @Data @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservacionesJuradoDTO {
        private Long juradoId;
        private String nombreJurado;
        private String rol;
        private List<CriterioObservacionDTO> criterios;
        private Double notaJurado;
        private String observaciones;
        private String resultado;
        private String comentarioPreestablecido;
    }

    @Data @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriterioObservacionDTO {
        private String nombreCriterio;
        private Double ponderacion;
        private Integer escala;
        private String rangoDescripcion;
        private Double notaObtenida;
        private String observacionAuto;
        private String observacionManual;
    }

    @Data @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservacionesCoordinadorDTO {
        private String observaciones;
        private Double notaInstructor;
        private Double notaFinal;
        private String resultado;
    }
}
