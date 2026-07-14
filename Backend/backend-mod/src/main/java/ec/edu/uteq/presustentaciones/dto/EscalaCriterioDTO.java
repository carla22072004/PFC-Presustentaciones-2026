package ec.edu.uteq.presustentaciones.dto;

import lombok.Data;

@Data
public class EscalaCriterioDTO {
    private Long criterioId;
    /** Escala: 1-100 (% del puntaje máximo del criterio) */
    private Integer escala;
    private String observaciones;
    /** Observación automática según el rango (generada en backend) */
    private String observacionAuto;
    /** Observación manual ingresada por el jurado */
    private String observacionManual;
}
