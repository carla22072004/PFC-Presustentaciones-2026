package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {

    Usuario crear(Usuario usuario);

    Usuario actualizar(Long id, Usuario usuario);

    void eliminar(Long id);

    Optional<Usuario> obtenerPorId(Long id);

    Optional<Usuario> obtenerPorEmail(String email);

    List<Usuario> listarTodos();

    List<Usuario> listarActivos();

    boolean existePorEmail(String email);

    void activar(Long id);

    void desactivar(Long id);

    Usuario actualizarPerfil(Long id, String emailNotificaciones, String telefono);
}