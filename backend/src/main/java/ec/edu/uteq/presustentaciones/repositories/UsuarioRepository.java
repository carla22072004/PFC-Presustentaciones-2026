package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByActivoTrue();

    /** La tabla puede tener decenas de miles de filas (datos de carga k6) — el listado del panel de admin siempre pagina. */
    @Query("SELECT u FROM Usuario u WHERE :q IS NULL OR :q = '' " +
           "OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.rol) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Usuario> buscarPaginado(@Param("q") String q, Pageable pageable);

    @Modifying
    @Query("UPDATE Usuario u SET u.emailNotificaciones = :emailNoti, u.telefono = :telefono WHERE u.id = :id")
    int actualizarPerfil(@Param("id") Long id,
                         @Param("emailNoti") String emailNotificaciones,
                         @Param("telefono") String telefono);

    List<Usuario> findByRol(String rol);
}