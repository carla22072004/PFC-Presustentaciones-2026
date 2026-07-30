package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    List<Docente> findByDisponibleTrue();

    Optional<Docente> findByUsuarioId(Long usuarioId);

    @Query("SELECT d FROM Docente d WHERE d.disponible = true ORDER BY d.cargaHorariaSemanal ASC")
    List<Docente> findDisponiblesOrdenadosPorCarga();

    @Query("SELECT d FROM Docente d ORDER BY d.cargaHorariaSemanal ASC")
    List<Docente> findTodosOrdenadosPorCarga();
}
