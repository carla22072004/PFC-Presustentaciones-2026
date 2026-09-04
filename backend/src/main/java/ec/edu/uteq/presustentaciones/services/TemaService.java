package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GenerarTemaRequest;
import ec.edu.uteq.presustentaciones.dto.GuardarTemaPropuestoRequest;
import ec.edu.uteq.presustentaciones.dto.TemaPropuestoDTO;
import java.util.List;

public interface TemaService {

    /**
     * Explora el catálogo de temas propuestos con filtros opcionales.
     * @param estudianteId si no es null, cada tema se marca con {@code guardado} según
     *                     los temas que ya guardó ese estudiante.
     */
    List<TemaPropuestoDTO> explorar(Integer carreraId, Integer lineaInvestigacionId,
                                    Integer areaId, String nivelDificultad, Long estudianteId);

    /** Sugiere ideas de tema a partir de la carrera / línea del estudiante. */
    List<TemaPropuestoDTO> generarIdeas(GenerarTemaRequest request);

    /** Detalle de un tema propuesto. */
    TemaPropuestoDTO obtenerDetalle(Integer temaPropuestoId);

    void guardarTemaEstudiante(Long estudianteId, Integer temaPropuestoId);

    void quitarTemaGuardado(Long estudianteId, Integer temaPropuestoId);

    List<TemaPropuestoDTO> obtenerTemasGuardados(Long estudianteId);

    // ── Gestión del catálogo (permiso ORIENTACION_CATALOGO_GESTIONAR) ─────────

    TemaPropuestoDTO crear(GuardarTemaPropuestoRequest request);

    TemaPropuestoDTO actualizar(Integer temaPropuestoId, GuardarTemaPropuestoRequest request);

    void eliminar(Integer temaPropuestoId);
}
