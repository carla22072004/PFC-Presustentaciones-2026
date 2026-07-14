package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TipoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoMensajeRepository extends JpaRepository<TipoMensaje, Short> {
    Optional<TipoMensaje> findByCodigo(String codigo);
}
