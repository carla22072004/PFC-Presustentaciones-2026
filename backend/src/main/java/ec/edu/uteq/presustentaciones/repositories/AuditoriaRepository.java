package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a WHERE " +
           "(:tabla IS NULL OR :tabla = '' OR a.tabla = :tabla) " +
           "AND (:accion IS NULL OR :accion = '' OR a.accion = :accion) " +
           "AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId) " +
           "AND (:texto IS NULL OR :texto = '' " +
           "     OR LOWER(a.usuarioNombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "     OR LOWER(a.tabla) LIKE LOWER(CONCAT('%', :texto, '%')))")
    Page<Auditoria> buscarConFiltros(@Param("tabla") String tabla,
                                      @Param("accion") String accion,
                                      @Param("usuarioId") Long usuarioId,
                                      @Param("texto") String texto,
                                      Pageable pageable);
}
