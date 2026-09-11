package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /**
     * RNF-19: depuración automática de la bitácora (nunca vía API -- ver RNF-18). Bulk delete
     * de JPA: no dispara ningún trigger ni evento de aplicación, es un {@code DELETE} directo,
     * y devuelve cuántas filas borró para que {@code DepuracionBitacoraScheduler} pueda dejar
     * traza exacta.
     */
    @Modifying
    @Query("DELETE FROM Auditoria a WHERE a.fecha < :fechaCorte")
    int borrarAnterioresA(@Param("fechaCorte") LocalDateTime fechaCorte);

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
