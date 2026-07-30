package ec.edu.uteq.presustentaciones.repositories;

import ec.edu.uteq.presustentaciones.entities.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    
    Optional<Estudiante> findByExpedienteCodigo(String expedienteCodigo);
    
    Optional<Estudiante> findByUsuarioId(Long usuarioId);
    
    List<Estudiante> findByCarrera(String carrera);
    
    @Query("SELECT e FROM Estudiante e JOIN FETCH e.usuario WHERE e.id = :id")
    Optional<Estudiante> findByIdWithUsuario(Long id);
}
