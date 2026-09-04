package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ProgresoTitulacionDTO;

import java.util.Map;

public interface ProgresoTitulacionService {

    /** Estado actual de la ruta de titulación del estudiante (todos los pasos en falso si aún no guardó nada). */
    ProgresoTitulacionDTO obtener(Long estudianteId);

    /** Fusiona los cambios recibidos con el estado guardado y devuelve el progreso actualizado. */
    ProgresoTitulacionDTO actualizar(Long estudianteId, Map<String, Boolean> cambios);
}
