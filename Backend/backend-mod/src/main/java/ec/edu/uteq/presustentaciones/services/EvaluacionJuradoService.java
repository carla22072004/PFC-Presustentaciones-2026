package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EvaluacionJuradoDTO;
import ec.edu.uteq.presustentaciones.entities.EvaluacionJurado;
import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.repositories.EvaluacionJuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluacionJuradoService {

    private final EvaluacionJuradoRepository evaluacionJuradoRepo;
    private final SolicitudRepository solicitudRepo;
    private final JuradoRepository juradoRepo;

    @Transactional
    public EvaluacionJuradoDTO guardarEvaluacion(Long solicitudId, Long juradoId, Double notaJurado, String observaciones) {
        Solicitud solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));

        Jurado jurado = juradoRepo.findById(juradoId)
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado: " + juradoId));

        if (!jurado.getSolicitud().getId().equals(solicitudId)) {
            throw new RuntimeException("El jurado no pertenece a esta solicitud.");
        }

        if (notaJurado < 1 || notaJurado > 10) {
            throw new RuntimeException("La nota debe estar entre 1 y 10.");
        }

        String resultado = notaJurado >= 7 ? "APROBADO" : "REPROBADO";
        String comentarioPreestablecido = generarComentarioPorRango(notaJurado);

        Optional<EvaluacionJurado> existente = evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(solicitudId, juradoId);
        
        EvaluacionJurado evaluacion;
        if (existente.isPresent()) {
            evaluacion = existente.get();
            evaluacion.setNotaJurado(notaJurado);
            evaluacion.setObservaciones(observaciones);
            evaluacion.setResultado(resultado);
            evaluacion.setComentarioPreestablecido(comentarioPreestablecido);
        } else {
            evaluacion = EvaluacionJurado.builder()
                    .solicitud(solicitud)
                    .jurado(jurado)
                    .notaJurado(notaJurado)
                    .observaciones(observaciones)
                    .resultado(resultado)
                    .comentarioPreestablecido(comentarioPreestablecido)
                    .build();
        }

        evaluacion = evaluacionJuradoRepo.save(evaluacion);
        return toDTO(evaluacion);
    }

    @Transactional(readOnly = true)
    public EvaluacionJuradoDTO obtenerEvaluacion(Long solicitudId, Long juradoId) {
        return evaluacionJuradoRepo.findBySolicitudIdAndJuradoId(solicitudId, juradoId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionJuradoDTO> obtenerTribunal(Long solicitudId) {
        return evaluacionJuradoRepo.findBySolicitudId(solicitudId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private EvaluacionJuradoDTO toDTO(EvaluacionJurado eval) {
        String nombreJurado = "";
        if (eval.getJurado() != null && eval.getJurado().getDocente() != null 
            && eval.getJurado().getDocente().getUsuario() != null) {
            nombreJurado = eval.getJurado().getDocente().getUsuario().getNombre() + " "
                    + eval.getJurado().getDocente().getUsuario().getApellido();
        }

        return EvaluacionJuradoDTO.builder()
                .id(eval.getId())
                .solicitudId(eval.getSolicitud().getId())
                .juradoId(eval.getJurado().getId())
                .notaJurado(eval.getNotaJurado())
                .observaciones(eval.getObservaciones())
                .resultado(eval.getResultado())
                .comentarioPreestablecido(eval.getComentarioPreestablecido())
                .nombreJurado(nombreJurado)
                .rolJurado(eval.getJurado().getRol())
                .build();
    }

    private String generarComentarioPorRango(Double nota) {
        if (nota <= 3) {
            return "El trabajo no cumple con los requisitos mínimos esperados. Se evidencian falencias significativas que requieren correcciones sustanciales.";
        } else if (nota <= 6) {
            return "El trabajo presenta un nivel aceptable pero con aspectos que requieren mejoras o correcciones para alcanzar los estándares esperados.";
        } else {
            return "El trabajo cumple satisfactoriamente con los objetivos y requisitos establecidos, demostrando un desempeño adecuado.";
        }
    }
}
