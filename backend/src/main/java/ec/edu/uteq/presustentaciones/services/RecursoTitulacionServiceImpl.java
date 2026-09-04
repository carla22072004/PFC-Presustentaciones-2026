package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GuardarRecursoRequest;
import ec.edu.uteq.presustentaciones.dto.RecursoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.RecursoTitulacion;
import ec.edu.uteq.presustentaciones.repositories.CarreraRepository;
import ec.edu.uteq.presustentaciones.repositories.RecursoTitulacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecursoTitulacionServiceImpl implements RecursoTitulacionService {

    private final RecursoTitulacionRepository recursoRepository;
    private final CarreraRepository carreraRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecursoTitulacionDTO> listar(Integer carreraId) {
        List<RecursoTitulacion> recursos = carreraId == null
                ? recursoRepository.listarTodos()
                : recursoRepository.listarVisiblesParaCarrera(carreraId);
        return recursos.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RecursoTitulacionDTO crear(GuardarRecursoRequest request) {
        RecursoTitulacion recurso = RecursoTitulacion.builder()
                .titulo(request.getTitulo().trim())
                .categoria(request.getCategoria().trim())
                .urlArchivo(request.getUrlArchivo().trim())
                .carrera(resolverCarrera(request.getCarreraId()))
                .build();
        return mapToDTO(recursoRepository.save(recurso));
    }

    @Override
    @Transactional
    public RecursoTitulacionDTO actualizar(Integer id, GuardarRecursoRequest request) {
        RecursoTitulacion recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado"));
        recurso.setTitulo(request.getTitulo().trim());
        recurso.setCategoria(request.getCategoria().trim());
        recurso.setUrlArchivo(request.getUrlArchivo().trim());
        recurso.setCarrera(resolverCarrera(request.getCarreraId()));
        return mapToDTO(recursoRepository.save(recurso));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!recursoRepository.existsById(id)) {
            throw new IllegalArgumentException("Recurso no encontrado");
        }
        recursoRepository.deleteById(id);
    }

    private Carrera resolverCarrera(Integer carreraId) {
        if (carreraId == null) {
            return null;
        }
        return carreraRepository.findById(carreraId)
                .orElseThrow(() -> new IllegalArgumentException("Carrera no encontrada"));
    }

    private RecursoTitulacionDTO mapToDTO(RecursoTitulacion r) {
        return RecursoTitulacionDTO.builder()
                .id(r.getId())
                .titulo(r.getTitulo())
                .categoria(r.getCategoria())
                .urlArchivo(r.getUrlArchivo())
                .carreraId(r.getCarrera() != null ? r.getCarrera().getId() : null)
                .carreraNombre(r.getCarrera() != null ? r.getCarrera().getNombre() : null)
                .build();
    }
}
