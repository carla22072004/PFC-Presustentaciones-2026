package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.ModalidadTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModalidadTitulacionRepository extends JpaRepository<ModalidadTitulacion, Short> {
    Optional<ModalidadTitulacion> findByCodigo(String codigo);
}
