package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.DisponibilidadSala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DisponibilidadSalaRepository extends JpaRepository<DisponibilidadSala, Long> {
    List<DisponibilidadSala> findBySalaIdAndFecha(Long salaId, LocalDate fecha);
    List<DisponibilidadSala> findByFecha(LocalDate fecha);
}
