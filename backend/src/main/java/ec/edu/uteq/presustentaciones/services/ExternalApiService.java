package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.UniversityDto;
import java.util.List;

public interface ExternalApiService {
    List<UniversityDto> getUniversitiesOfEcuador();
}
