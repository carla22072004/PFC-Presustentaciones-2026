package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.GuardarRecursoRequest;
import ec.edu.uteq.presustentaciones.dto.RecursoTitulacionDTO;

import java.util.List;

public interface RecursoTitulacionService {

    /** Recursos visibles para una carrera (los generales + los de esa carrera). null = todos. */
    List<RecursoTitulacionDTO> listar(Integer carreraId);

    RecursoTitulacionDTO crear(GuardarRecursoRequest request);

    RecursoTitulacionDTO actualizar(Integer id, GuardarRecursoRequest request);

    void eliminar(Integer id);
}
