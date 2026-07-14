package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.EscalaCriterioDTO;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaRequest;
import ec.edu.uteq.presustentaciones.dto.EvaluacionRubricaResponse;
import ec.edu.uteq.presustentaciones.dto.ObservacionesSolicitudDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricaEvaluacionServiceImpl implements RubricaEvaluacionService {

    private final EvaluacionCriterioRepository evalCriterioRepo;
    private final CriterioRubricaRepository criterioRepo;
    private final JuradoRepository juradoRepo;
    private final SolicitudRepository solicitudRepo;
    private final RubricaRepository rubricaRepo;
    private final TutorRepository tutorRepo;
    private final EvaluacionRepository evaluacionRepo;
    private final EvaluacionJuradoRepository evaluacionJuradoRepo;

    @Override
    @Transactional
    public EvaluacionRubricaResponse registrarEvaluacion(EvaluacionRubricaRequest req) {
        Solicitud solicitud = solicitudRepo.findById(req.getSolicitudId())
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + req.getSolicitudId()));

        Jurado jurado = juradoRepo.findById(req.getJuradoId())
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado: " + req.getJuradoId()));

        if (!jurado.getSolicitud().getId().equals(req.getSolicitudId())) {
            throw new RuntimeException("El jurado no pertenece a esta solicitud.");
        }

        rubricaRepo.findById(req.getRubricaId())
                .orElseThrow(() -> new RuntimeException("Rúbrica no encontrada: " + req.getRubricaId()));

        List<CriterioRubrica> criterios = criterioRepo.findByRubricaIdOrderByOrdenAsc(req.getRubricaId());
        if (criterios.isEmpty()) {
            throw new RuntimeException("La rúbrica no tiene criterios definidos.");
        }
        if (req.getCriterios() == null || req.getCriterios().size() != criterios.size()) {
            throw new RuntimeException("Debe evaluar todos los " + criterios.size() + " criterios de la rúbrica.");
        }
        for (EscalaCriterioDTO c : req.getCriterios()) {
            if (c.getEscala() < 1 || c.getEscala() > 100) {
                throw new RuntimeException("Escala inválida: " + c.getEscala() + ". Use valores entre 1 y 100.");
            }
        }

        // Permite re-evaluación: eliminar la anterior
        evalCriterioRepo.deleteBySolicitudIdAndJuradoId(req.getSolicitudId(), req.getJuradoId());

        List<EvaluacionCriterio> guardadas = new ArrayList<>();
        for (EscalaCriterioDTO cDto : req.getCriterios()) {
            CriterioRubrica criterio = criterios.stream()
                    .filter(c -> c.getId().equals(cDto.getCriterioId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Criterio no encontrado: " + cDto.getCriterioId()));

            double notaObtenida = Math.round(criterio.getPonderacion() * cDto.getEscala() / 100.0 * 100.0) / 100.0;
            String observacionAuto = EvaluacionCriterio.getObservacionPorRango(cDto.getEscala());

            EvaluacionCriterio ec = EvaluacionCriterio.builder()
                    .solicitud(solicitud)
                    .jurado(jurado)
                    .criterio(criterio)
                    .escala(cDto.getEscala())
                    .notaObtenida(notaObtenida)
                    .observacionAuto(observacionAuto)
                    .observacionManual(cDto.getObservacionManual())
                    .observaciones(cDto.getObservaciones())
                    .build();
            guardadas.add(evalCriterioRepo.save(ec));
        }

        return buildResponse(jurado, guardadas, req.getSolicitudId());
    }

    @Override
    public EvaluacionRubricaResponse obtenerEvaluacionJurado(Long solicitudId, Long juradoId) {
        Jurado jurado = juradoRepo.findById(juradoId)
                .orElseThrow(() -> new RuntimeException("Jurado no encontrado: " + juradoId));
        List<EvaluacionCriterio> evals = evalCriterioRepo.findBySolicitudIdAndJuradoId(solicitudId, juradoId);
        return buildResponse(jurado, evals, solicitudId);
    }

    @Override
    public List<EvaluacionRubricaResponse> obtenerEvaluacionesSolicitud(Long solicitudId) {
        List<Jurado> jurados = juradoRepo.findBySolicitudId(solicitudId);
        return jurados.stream()
                .map(j -> {
                    List<EvaluacionCriterio> evals = evalCriterioRepo.findBySolicitudIdAndJuradoId(solicitudId, j.getId());
                    return buildResponse(j, evals, solicitudId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Double calcularNotaTribunal(Long solicitudId) {
        return promedioTribunal(solicitudId);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /** Calcula el promedio de (suma de notas por jurado) en Java para evitar subqueries en JPQL */
    private Double promedioTribunal(Long solicitudId) {
        List<Object[]> filas = evalCriterioRepo.sumaPorJurado(solicitudId);
        if (filas == null || filas.isEmpty()) return null;
        double suma = filas.stream()
                .mapToDouble(f -> ((Number) f[1]).doubleValue())
                .sum();
        double promedio = suma / filas.size();
        return Math.round(promedio * 100.0) / 100.0;
    }

    private EvaluacionRubricaResponse buildResponse(Jurado jurado,
                                                     List<EvaluacionCriterio> evals,
                                                     Long solicitudId) {
        String nombre = jurado.getDocente() != null && jurado.getDocente().getUsuario() != null
                ? jurado.getDocente().getUsuario().getNombre() + " " + jurado.getDocente().getUsuario().getApellido()
                : "Docente #" + jurado.getId();

        List<EvaluacionRubricaResponse.CriterioResultado> detalles = evals.stream()
                .map(ec -> EvaluacionRubricaResponse.CriterioResultado.builder()
                        .criterioId(ec.getCriterio().getId())
                        .nombreCriterio(ec.getCriterio().getNombre())
                        .ponderacion(ec.getCriterio().getPonderacion())
                        .escala(ec.getEscala())
                        .rangoDescripcion(EvaluacionCriterio.getRangoDescripcion(ec.getEscala()))
                        .notaObtenida(ec.getNotaObtenida())
                        .observacionAuto(ec.getObservacionAuto())
                        .observacionManual(ec.getObservacionManual())
                        .observaciones(ec.getObservaciones())
                        .build())
                .collect(Collectors.toList());

        double notaTotal = evals.stream()
                .mapToDouble(EvaluacionCriterio::getNotaObtenida)
                .sum();
        notaTotal = Math.round(notaTotal * 100.0) / 100.0;

        Double notaPromedio = promedioTribunal(solicitudId);

        List<Jurado> todosJurados = juradoRepo.findBySolicitudId(solicitudId);
        boolean completo = !todosJurados.isEmpty() && todosJurados.stream()
                .allMatch(j -> evalCriterioRepo.existsBySolicitudIdAndJuradoId(solicitudId, j.getId()));

        return EvaluacionRubricaResponse.builder()
                .solicitudId(solicitudId)
                .juradoId(jurado.getId())
                .nombreJurado(nombre)
                .rolJurado(jurado.getRol())
                .detalles(detalles)
                .notaTotalJurado(evals.isEmpty() ? null : notaTotal)
                .notaPromedioTribunal(notaPromedio)
                .tribunalCompleto(completo)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ObservacionesSolicitudDTO obtenerObservacionesSolicitud(Long solicitudId) {
        Solicitud solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + solicitudId));

        String nombreEstudiante = "";
        if (solicitud.getEstudiante() != null && solicitud.getEstudiante().getUsuario() != null) {
            nombreEstudiante = solicitud.getEstudiante().getUsuario().getNombre() + " " 
                    + solicitud.getEstudiante().getUsuario().getApellido();
        }

        ObservacionesSolicitudDTO.ObservacionesTutorDTO tutorDTO = null;
        var tutorOpt = tutorRepo.findBySolicitudId(solicitudId);
        if (tutorOpt.isPresent()) {
            Tutor tutor = tutorOpt.get();
            String nombreTutor = tutor.getDocente() != null && tutor.getDocente().getUsuario() != null
                    ? tutor.getDocente().getUsuario().getNombre() + " " + tutor.getDocente().getUsuario().getApellido()
                    : "Tutor";
            String fechaRegistro = tutor.getFechaAsignacion() != null
                    ? tutor.getFechaAsignacion().toString() : null;
            tutorDTO = ObservacionesSolicitudDTO.ObservacionesTutorDTO.builder()
                    .tutorId(tutor.getId())
                    .nombreTutor(nombreTutor)
                    .observaciones(tutor.getObservaciones())
                    .fechaRegistro(fechaRegistro)
                    .build();
        }

        List<ObservacionesSolicitudDTO.ObservacionesJuradoDTO> juradosDTO = new ArrayList<>();
        List<Jurado> jurados = juradoRepo.findBySolicitudId(solicitudId);
        
        List<EvaluacionJurado> evaluacionesJurado = evaluacionJuradoRepo.findBySolicitudId(solicitudId);
        
        for (Jurado jurado : jurados) {
            String nombreJurado = jurado.getDocente() != null && jurado.getDocente().getUsuario() != null
                    ? jurado.getDocente().getUsuario().getNombre() + " " + jurado.getDocente().getUsuario().getApellido()
                    : "Docente";
            
            EvaluacionJurado evalJurado = evaluacionesJurado.stream()
                    .filter(e -> e.getJurado().getId().equals(jurado.getId()))
                    .findFirst()
                    .orElse(null);
            
            List<EvaluacionCriterio> criterios = evalCriterioRepo.findBySolicitudIdAndJuradoId(solicitudId, jurado.getId());
            List<ObservacionesSolicitudDTO.CriterioObservacionDTO> criteriosDTO = criterios.stream()
                    .map(ec -> ObservacionesSolicitudDTO.CriterioObservacionDTO.builder()
                            .nombreCriterio(ec.getCriterio().getNombre())
                            .ponderacion(ec.getCriterio().getPonderacion())
                            .escala(ec.getEscala())
                            .rangoDescripcion(EvaluacionCriterio.getRangoDescripcion(ec.getEscala()))
                            .notaObtenida(ec.getNotaObtenida())
                            .observacionAuto(ec.getObservacionAuto())
                            .observacionManual(ec.getObservacionManual())
                            .build())
                    .collect(Collectors.toList());
            
            juradosDTO.add(ObservacionesSolicitudDTO.ObservacionesJuradoDTO.builder()
                    .juradoId(jurado.getId())
                    .nombreJurado(nombreJurado)
                    .rol(jurado.getRol())
                    .criterios(criteriosDTO)
                    .notaJurado(evalJurado != null ? evalJurado.getNotaJurado() : null)
                    .observaciones(evalJurado != null ? evalJurado.getObservaciones() : null)
                    .resultado(evalJurado != null ? evalJurado.getResultado() : null)
                    .comentarioPreestablecido(evalJurado != null ? evalJurado.getComentarioPreestablecido() : null)
                    .build());
        }

        ObservacionesSolicitudDTO.ObservacionesCoordinadorDTO coordinadorDTO = null;
        var evaluacionOpt = evaluacionRepo.findBySolicitudId(solicitudId);
        if (evaluacionOpt.isPresent()) {
            Evaluacion ev = evaluacionOpt.get();
            coordinadorDTO = ObservacionesSolicitudDTO.ObservacionesCoordinadorDTO.builder()
                    .observaciones(ev.getObservaciones())
                    .notaInstructor(ev.getNotaInstructor())
                    .notaFinal(ev.getNotaFinal())
                    .resultado(ev.getResultado())
                    .build();
        }

        return ObservacionesSolicitudDTO.builder()
                .solicitudId(solicitudId)
                .tituloTema(solicitud.getTituloTema())
                .nombreEstudiante(nombreEstudiante)
                .tutor(tutorDTO)
                .jurados(juradosDTO)
                .coordinador(coordinadorDTO)
                .build();
    }
}
