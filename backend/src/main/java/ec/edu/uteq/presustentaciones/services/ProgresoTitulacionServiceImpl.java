package ec.edu.uteq.presustentaciones.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.presustentaciones.dto.ProgresoTitulacionDTO;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.ProgresoEstudiante;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.ProgresoEstudianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProgresoTitulacionServiceImpl implements ProgresoTitulacionService {

    private final ProgresoEstudianteRepository progresoRepository;
    private final EstudianteRepository estudianteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Catálogo fijo de pasos de la ruta de titulación, en orden. */
    private static final List<ProgresoTitulacionDTO.PasoDTO> CATALOGO = List.of(
            paso("tema_definido", 1, "Definir el tema de titulación",
                    "Tienes claro el problema, el objetivo general y la línea de investigación."),
            paso("tutor_asignado", 2, "Tener un tutor asignado",
                    "La coordinación te asignó un docente tutor para tu anteproyecto."),
            paso("anteproyecto_elaborado", 3, "Elaborar el anteproyecto",
                    "Redactaste el documento del anteproyecto siguiendo la plantilla oficial."),
            paso("anteproyecto_aprobado", 4, "Anteproyecto aprobado por el tutor",
                    "El tutor revisó y aprobó tu anteproyecto."),
            paso("solicitud_registrada", 5, "Registrar la solicitud de pre-sustentación",
                    "Enviaste la solicitud desde 'Nueva Solicitud' con toda la información requerida."),
            paso("correcciones_aplicadas", 6, "Aplicar las correcciones de tutoría",
                    "Subiste las correcciones de cada fase de tutoría y el tutor las aprobó."),
            paso("documento_final", 7, "Subir el documento final del anteproyecto",
                    "Cargaste la versión final en PDF lista para el tribunal."),
            paso("pre_sustentacion_programada", 8, "Pre-sustentación programada",
                    "Ya tienes fecha, hora y sala asignadas para tu pre-sustentación.")
    );

    private static ProgresoTitulacionDTO.PasoDTO paso(String clave, int orden, String titulo, String desc) {
        return ProgresoTitulacionDTO.PasoDTO.builder()
                .clave(clave).orden(orden).titulo(titulo).descripcion(desc).completado(false).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgresoTitulacionDTO obtener(Long estudianteId) {
        String json = progresoRepository.findByEstudianteId(estudianteId)
                .map(ProgresoEstudiante::getPasosJson)
                .orElse("{}");
        return construirDTO(leerEstado(json));
    }

    @Override
    @Transactional
    public ProgresoTitulacionDTO actualizar(Long estudianteId, Map<String, Boolean> cambios) {
        ProgresoEstudiante progreso = progresoRepository.findByEstudianteId(estudianteId)
                .orElseGet(() -> crearVacio(estudianteId));

        Map<String, Boolean> estado = leerEstado(progreso.getPasosJson());
        for (ProgresoTitulacionDTO.PasoDTO p : CATALOGO) {
            if (cambios.containsKey(p.getClave()) && cambios.get(p.getClave()) != null) {
                estado.put(p.getClave(), cambios.get(p.getClave()));
            }
        }
        progreso.setPasosJson(escribirEstado(estado));
        progresoRepository.save(progreso);
        return construirDTO(estado);
    }

    private ProgresoEstudiante crearVacio(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        return ProgresoEstudiante.builder()
                .estudiante(estudiante)
                .pasosJson("{}")
                .build();
    }

    private Map<String, Boolean> leerEstado(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Boolean> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            return parsed != null ? parsed : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String escribirEstado(Map<String, Boolean> estado) {
        try {
            return objectMapper.writeValueAsString(estado);
        } catch (Exception e) {
            return "{}";
        }
    }

    private ProgresoTitulacionDTO construirDTO(Map<String, Boolean> estado) {
        List<ProgresoTitulacionDTO.PasoDTO> pasos = CATALOGO.stream()
                .map(p -> ProgresoTitulacionDTO.PasoDTO.builder()
                        .clave(p.getClave())
                        .orden(p.getOrden())
                        .titulo(p.getTitulo())
                        .descripcion(p.getDescripcion())
                        .completado(Boolean.TRUE.equals(estado.get(p.getClave())))
                        .build())
                .toList();

        int completados = (int) pasos.stream().filter(ProgresoTitulacionDTO.PasoDTO::isCompletado).count();
        int total = pasos.size();
        int porcentaje = total == 0 ? 0 : Math.round((completados * 100f) / total);

        return ProgresoTitulacionDTO.builder()
                .pasos(pasos)
                .completados(completados)
                .total(total)
                .porcentaje(porcentaje)
                .build();
    }
}
