package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    /**
     * Invoca sp_generar_codigo_expediente (FUNCTION escalar, categoría "generación de
     * códigos secuenciales" del Bloque A.2): usa nextval() sobre una secuencia dedicada,
     * atómico a nivel de motor -- sin condiciones de carrera entre altas concurrentes.
     */
    @Procedure(name = "Estudiante.generarCodigoExpediente")
    String generarCodigoExpediente(@Param("p_anio") Integer anio, @Param("p_codigo") String codigoInicial);

    Optional<Estudiante> findByExpedienteCodigo(String expedienteCodigo);
    
    Optional<Estudiante> findByUsuarioId(Long usuarioId);
    
    List<Estudiante> findByCarrera(String carrera);
    
    @Query("SELECT e FROM Estudiante e JOIN FETCH e.usuario WHERE e.id = :id")
    Optional<Estudiante> findByIdWithUsuario(Long id);
}
