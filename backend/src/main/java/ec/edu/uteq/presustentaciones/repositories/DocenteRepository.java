package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Docente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {

    List<Docente> findByDisponibleTrue();

    Optional<Docente> findByUsuarioId(Long usuarioId);

    /**
     * ERR-02: la tabla docente tiene 9,807 filas -- /api/docentes (findAll sin Pageable) traía
     * todo de una vez y el <select> con un <option> por fila congelaba el navegador al abrirlo.
     * Búsqueda de texto libre (nombre/apellido/área de especialidad) + paginado, mismo patrón
     * que UsuarioRepository.buscarPaginado, para alimentar un combobox con typeahead en vez del
     * <select> nativo.
     */
    @Query(value = "SELECT d FROM Docente d JOIN FETCH d.usuario u " +
           "WHERE :q IS NULL OR :q = '' " +
           "OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(d.areaEspecialidad) LIKE LOWER(CONCAT('%', :q, '%'))",
           countQuery = "SELECT count(d) FROM Docente d JOIN d.usuario u " +
           "WHERE :q IS NULL OR :q = '' " +
           "OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(d.areaEspecialidad) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Docente> buscarPaginado(@Param("q") String q, Pageable pageable);

    @Query("SELECT d FROM Docente d WHERE d.disponible = true ORDER BY d.cargaHorariaSemanal ASC")
    List<Docente> findDisponiblesOrdenadosPorCarga();

    @Query("SELECT d FROM Docente d ORDER BY d.cargaHorariaSemanal ASC")
    List<Docente> findTodosOrdenadosPorCarga();

    /** Reportes: nombre de un conjunto acotado de docentes (los que participan en el proceso). */
    @Query("SELECT d.id, u.nombre, u.apellido FROM Docente d JOIN d.usuario u WHERE d.id IN :ids")
    List<Object[]> findNombresByIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}
