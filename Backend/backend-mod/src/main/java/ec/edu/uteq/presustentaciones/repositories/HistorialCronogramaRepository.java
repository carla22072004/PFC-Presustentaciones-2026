package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.HistorialCronograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialCronogramaRepository extends JpaRepository<HistorialCronograma, Long> {
    List<HistorialCronograma> findByCronogramaIdOrderByFechaCambioDesc(Long cronogramaId);
}
