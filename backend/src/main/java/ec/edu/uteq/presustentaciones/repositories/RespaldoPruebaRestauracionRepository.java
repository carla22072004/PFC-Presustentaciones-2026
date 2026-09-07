package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.RespaldoPruebaRestauracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespaldoPruebaRestauracionRepository extends JpaRepository<RespaldoPruebaRestauracion, Long> {

    List<RespaldoPruebaRestauracion> findTop50ByOrderByFechaDesc();

    RespaldoPruebaRestauracion findFirstByOrderByFechaDesc();
}
