package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.TemaGuardadoEstudiante;
import ec.edu.uteq.presustentaciones.entities.TemaPropuesto;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
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
