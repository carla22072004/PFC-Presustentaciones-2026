package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Usuario;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface IUsuarioService {

    /**
     * @param page número de página, base 0
     * @param size tamaño de página (se acota a un máximo de 100)
     * @param q    texto libre de búsqueda por nombre/apellido/email, o {@code null}
     * @return página de usuarios que cumplen el filtro
     */
    Page<Usuario> listarPaginado(int page, int size, String q);

    /**
     * Crea un usuario, encriptando su contraseña con BCrypt y resolviendo su
     * {@code RolUsuario} (FK) a partir del código de rol textual.
     *
     * @param usuario datos del usuario a crear (contraseña en texto plano)
     * @return el usuario creado, con la contraseña ya encriptada
     * @throws RuntimeException si ya existe un usuario con ese email, o el rol no es válido
     */
    Usuario crear(Usuario usuario);

    /**
     * Actualiza los datos de un usuario existente. Si el rol cambia, sincroniza también la FK
     * {@code RolUsuario} para que ambas columnas paralelas queden consistentes.
     *
     * @param id      id del usuario a actualizar
     * @param usuario datos nuevos (nombre, apellido, email, rol, teléfono)
     * @return el usuario actualizado
     * @throws RuntimeException si el usuario no existe o el nuevo rol no es válido
     */
    Usuario actualizar(Long id, Usuario usuario);

    /** @param id id del usuario a eliminar
     * @throws RuntimeException si el usuario no existe */
    void eliminar(Long id);

    /**
     * @param id id del usuario
     * @return el usuario si existe
     */
    Optional<Usuario> obtenerPorId(Long id);

    /**
     * @param email email del usuario
     * @return el usuario con ese email, si existe
     */
    Optional<Usuario> obtenerPorEmail(String email);

    /** @return todos los usuarios del sistema, sin paginar */
    List<Usuario> listarTodos();

    /** @return los usuarios con {@code activo = true} */
    List<Usuario> listarActivos();

    /**
     * @param email email a verificar
     * @return {@code true} si ya existe un usuario registrado con ese email
     */
    boolean existePorEmail(String email);

    /** @param id id del usuario a activar
     * @throws RuntimeException si el usuario no existe */
    void activar(Long id);

    /** @param id id del usuario a desactivar
     * @throws RuntimeException si el usuario no existe */
    void desactivar(Long id);

    /**
     * Actualiza solo los campos de perfil autoeditables por el propio usuario (no requiere
     * rol ADMIN), sin tocar rol ni email institucional.
     *
     * @param id                  id del usuario
     * @param emailNotificaciones correo alterno para recibir notificaciones, o {@code null}
     * @param telefono            teléfono de contacto, o {@code null}
     * @return el usuario actualizado
     * @throws RuntimeException si el usuario no existe
     */
    Usuario actualizarPerfil(Long id, String emailNotificaciones, String telefono);
}
