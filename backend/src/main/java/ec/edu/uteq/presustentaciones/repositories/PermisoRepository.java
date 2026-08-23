package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Short> {

    List<Permiso> findAllByOrderByCategoriaAscNombreAsc();

    List<Permiso> findByCodigoIn(List<String> codigos);

    @Query(value = "SELECT rp.rol_id FROM presus.rol_permisos rp " +
            "JOIN presus.permisos p ON p.id = rp.permiso_id " +
            "WHERE p.codigo = :codigo", nativeQuery = true)
    List<Short> findRolIdsConPermiso(@Param("codigo") String codigo);

    /**
     * Punto único de verificación de acceso -- reemplaza los @PreAuthorize("hasRole(...)")
     * fijos en código. Consulta directa contra la tabla de sesión (usuarios.rol_id) unida
     * a rol_permisos, así que un cambio en "Gestionar Permisos" aplica de inmediato, sin
     * esperar a que el usuario vuelva a iniciar sesión (el JWT no lleva permisos, solo
     * identidad -- por diseño, para que esto sea realmente dinámico).
     */
    @Query(value = "SELECT EXISTS (" +
            "  SELECT 1 FROM presus.rol_permisos rp " +
            "  JOIN presus.permisos p ON p.id = rp.permiso_id " +
            "  JOIN presus.usuarios u ON u.rol_id = rp.rol_id " +
            "  WHERE u.email = :email AND p.codigo = :codigo" +
            ")", nativeQuery = true)
    boolean usuarioTienePermiso(@Param("email") String email, @Param("codigo") String codigo);

    @Query(value = "SELECT p.codigo FROM presus.permisos p " +
            "JOIN presus.rol_permisos rp ON rp.permiso_id = p.id " +
            "WHERE rp.rol_id = :rolId", nativeQuery = true)
    List<String> findCodigosPorRol(@Param("rolId") Short rolId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM presus.rol_permisos WHERE rol_id = :rolId", nativeQuery = true)
    void eliminarPermisosDeRol(@Param("rolId") Short rolId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (:rolId, :permisoId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void asignarPermiso(@Param("rolId") Short rolId, @Param("permisoId") Short permisoId);
}
