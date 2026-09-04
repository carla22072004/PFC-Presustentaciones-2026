package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarTemaPropuestoRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import ec.edu.uteq.presustentaciones.entities.AreaTematica;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.LineaInvestigacion;
import ec.edu.uteq.presustentaciones.entities.TemaGuardadoEstudiante;
import ec.edu.uteq.presustentaciones.entities.TemaPropuesto;
import ec.edu.uteq.presustentaciones.repositories.AreaTematicaRepository;
import ec.edu.uteq.presustentaciones.repositories.CarreraRepository;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.LineaInvestigacionRepository;
import ec.edu.uteq.presustentaciones.repositories.TemaGuardadoEstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.TemaPropuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemaServiceImpl implements TemaService {

    private final TemaPropuestoRepository temaPropuestoRepository;
    private final TemaGuardadoEstudianteRepository temaGuardadoEstudianteRepository;
    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository carreraRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final AreaTematicaRepository areaTematicaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TemaPropuestoDTO> explorar(Integer carreraId, Integer lineaInvestigacionId,
                                           Integer areaId, String nivelDificultad, Long estudianteId) {
        String nivel = (nivelDificultad != null && !nivelDificultad.isBlank()) ? nivelDificultad.trim() : null;
        List<TemaPropuesto> temas = temaPropuestoRepository.buscarConFiltros(
                carreraId, lineaInvestigacionId, areaId, nivel);

        Set<Integer> guardados = estudianteId == null
                ? Set.of()
                : Set.copyOf(temaGuardadoEstudianteRepository.findTemaIdsByEstudianteId(estudianteId));

        return temas.stream()
                .map(t -> mapToDTO(t, estudianteId != null ? guardados.contains(t.getId()) : null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemaPropuestoDTO> generarIdeas(GenerarTemaRequest request) {
        List<TemaPropuesto> temas;
        if (request.getLineaInvestigacionId() != null) {
            temas = temaPropuestoRepository.findByCarreraIdAndLineaInvestigacionId(
                    request.getCarreraId(), request.getLineaInvestigacionId());
        } else {
            temas = temaPropuestoRepository.findByCarreraId(request.getCarreraId());
        }
        return temas.stream().map(t -> mapToDTO(t, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TemaPropuestoDTO obtenerDetalle(Integer temaPropuestoId) {
        TemaPropuesto tema = temaPropuestoRepository.findByIdConCatalogos(temaPropuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Tema propuesto no encontrado"));
        return mapToDTO(tema, null);
    }

    @Override
    @Transactional
    public void guardarTemaEstudiante(Long estudianteId, Integer temaPropuestoId) {
        if (temaGuardadoEstudianteRepository.existsByEstudianteIdAndTemaPropuestoId(estudianteId, temaPropuestoId)) {
            throw new IllegalStateException("El tema ya está guardado por el estudiante");
        }

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        TemaPropuesto tema = temaPropuestoRepository.findById(temaPropuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Tema propuesto no encontrado"));

        TemaGuardadoEstudiante temaGuardado = TemaGuardadoEstudiante.builder()
                .estudiante(estudiante)
                .temaPropuesto(tema)
                .build();

        temaGuardadoEstudianteRepository.save(temaGuardado);
    }

    @Override
    @Transactional
    public void quitarTemaGuardado(Long estudianteId, Integer temaPropuestoId) {
        int eliminados = temaGuardadoEstudianteRepository
                .deleteByEstudianteIdAndTemaPropuestoId(estudianteId, temaPropuestoId);
        if (eliminados == 0) {
            throw new IllegalArgumentException("El tema no estaba en la lista de guardados del estudiante");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemaPropuestoDTO> obtenerTemasGuardados(Long estudianteId) {
        return temaGuardadoEstudianteRepository.findByEstudianteIdOrderByFechaGuardadoDesc(estudianteId).stream()
                .map(TemaGuardadoEstudiante::getTemaPropuesto)
                .map(t -> mapToDTO(t, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TemaPropuestoDTO crear(GuardarTemaPropuestoRequest request) {
        TemaPropuesto tema = new TemaPropuesto();
        aplicar(tema, request);
        return mapToDTO(temaPropuestoRepository.save(tema), null);
    }

    @Override
    @Transactional
    public TemaPropuestoDTO actualizar(Integer temaPropuestoId, GuardarTemaPropuestoRequest request) {
        TemaPropuesto tema = temaPropuestoRepository.findById(temaPropuestoId)
                .orElseThrow(() -> new IllegalArgumentException("Tema propuesto no encontrado"));
        aplicar(tema, request);
        return mapToDTO(temaPropuestoRepository.save(tema), null);
    }

    @Override
    @Transactional
    public void eliminar(Integer temaPropuestoId) {
        if (!temaPropuestoRepository.existsById(temaPropuestoId)) {
            throw new IllegalArgumentException("Tema propuesto no encontrado");
        }
        // temas_guardados tiene FK ON DELETE CASCADE (V20): al borrar el tema del
        // catálogo también se quita de la lista de los estudiantes que lo guardaron.
        temaPropuestoRepository.deleteById(temaPropuestoId);
    }

    private void aplicar(TemaPropuesto tema, GuardarTemaPropuestoRequest r) {
        tema.setTitulo(r.getTitulo().trim());
        tema.setProblema(trimOrNull(r.getProblema()));
        tema.setObjetivoGeneral(trimOrNull(r.getObjetivoGeneral()));
        tema.setObjetivosEspecificos(trimOrNull(r.getObjetivosEspecificos()));
        tema.setJustificacion(trimOrNull(r.getJustificacion()));
        tema.setBeneficiarios(trimOrNull(r.getBeneficiarios()));
        tema.setNivelDificultad(trimOrNull(r.getNivelDificultad()));

        Carrera carrera = r.getCarreraId() == null ? null : carreraRepository.findById(r.getCarreraId())
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada"));
        LineaInvestigacion linea = r.getLineaInvestigacionId() == null ? null
                : lineaInvestigacionRepository.findById(r.getLineaInvestigacionId())
                .orElseThrow(() -> new IllegalArgumentException("Línea de investigación no encontrada"));
        AreaTematica area = r.getAreaId() == null ? null : areaTematicaRepository.findById(r.getAreaId())
                .orElseThrow(() -> new IllegalArgumentException("Área temática no encontrada"));

        if (area != null && linea != null && area.getLineaInvestigacion() != null
                && !area.getLineaInvestigacion().getId().equals(linea.getId())) {
            throw new IllegalArgumentException("El área temática no pertenece a la línea de investigación indicada");
        }

        tema.setCarrera(carrera);
        tema.setLineaInvestigacion(linea);
        tema.setArea(area);
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private TemaPropuestoDTO mapToDTO(TemaPropuesto entity, Boolean guardado) {
        return TemaPropuestoDTO.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .problema(entity.getProblema())
                .objetivoGeneral(entity.getObjetivoGeneral())
                .objetivosEspecificos(entity.getObjetivosEspecificos())
                .justificacion(entity.getJustificacion())
                .beneficiarios(entity.getBeneficiarios())
                .nivelDificultad(entity.getNivelDificultad())
                .carreraId(entity.getCarrera() != null ? entity.getCarrera().getId() : null)
                .carreraNombre(entity.getCarrera() != null ? entity.getCarrera().getNombre() : null)
                .lineaInvestigacionId(entity.getLineaInvestigacion() != null ? entity.getLineaInvestigacion().getId() : null)
                .lineaInvestigacionNombre(entity.getLineaInvestigacion() != null ? entity.getLineaInvestigacion().getNombre() : null)
                .areaId(entity.getArea() != null ? entity.getArea().getId() : null)
                .areaNombre(entity.getArea() != null ? entity.getArea().getNombre() : null)
                .guardado(guardado)
                .build();
    }
}
