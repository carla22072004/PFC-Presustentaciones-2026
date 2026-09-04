package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.TemaPropuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemaPropuestoRepository extends JpaRepository<TemaPropuesto, Integer> {

    List<TemaPropuesto> findByCarreraId(Integer carreraId);

    List<TemaPropuesto> findByCarreraIdAndLineaInvestigacionId(Integer carreraId, Integer lineaInvestigacionId);

    /**
     * Búsqueda flexible para el explorador de temas propuestos: cada filtro es opcional
     * (null = no filtra). Se hace un solo query con LEFT JOIN FETCH de los catálogos para
     * poblar los nombres en el DTO sin incurrir en N+1.
     *
     * El CAST(:nivel AS string) es necesario: sin él, cuando nivel es null el driver de
     * Postgres no tiene pista de tipo para el parámetro dentro de LOWER(?) y lo envía como
     * bytea sin tipo, y "function lower(bytea) does not exist" -- hallazgo real al probar
     * el endpoint contra Postgres de verdad (los tests con repositorio mockeado nunca
     * ejecutan el SQL real y no lo detectan).
     */
    @Query("""
            SELECT t FROM TemaPropuesto t
            LEFT JOIN FETCH t.carrera c
            LEFT JOIN FETCH t.lineaInvestigacion l
            LEFT JOIN FETCH t.area a
            WHERE (:carreraId IS NULL OR c.id = :carreraId)
              AND (:lineaId IS NULL OR l.id = :lineaId)
              AND (:areaId IS NULL OR a.id = :areaId)
              AND (:nivel IS NULL OR LOWER(t.nivelDificultad) = LOWER(CAST(:nivel AS string)))
            ORDER BY t.titulo ASC
            """)
    List<TemaPropuesto> buscarConFiltros(@Param("carreraId") Integer carreraId,
                                         @Param("lineaId") Integer lineaId,
                                         @Param("areaId") Integer areaId,
                                         @Param("nivel") String nivelDificultad);

    @Query("""
            SELECT t FROM TemaPropuesto t
            LEFT JOIN FETCH t.carrera
            LEFT JOIN FETCH t.lineaInvestigacion
            LEFT JOIN FETCH t.area
            WHERE t.id = :id
            """)
    java.util.Optional<TemaPropuesto> findByIdConCatalogos(@Param("id") Integer id);
}
