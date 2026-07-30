package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.BloqueHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Short> {
    List<BloqueHorario> findByJornadaId(Short jornadaId);
    List<BloqueHorario> findByJornadaCodigo(String jornadaCodigo);
}
