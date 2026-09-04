package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolUsuarioRepository rolUsuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Juan");
        usuario.setApellido("Pérez");
        usuario.setEmail("jperez@uteq.edu.ec");
        usuario.setRol("ESTUDIANTE");
        usuario.setActivo(true);

        lenient().when(rolUsuarioRepository.findByCodigo(anyString()))
                .thenAnswer(inv -> Optional.of(RolUsuario.builder().codigo(inv.getArgument(0)).build()));
    }

    @Test
    void testListarTodos() {
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuario));
        List<Usuario> resultado = usuarioService.listarTodos();
        assertEquals(1, resultado.size());
        assertEquals("Juan", resultado.get(0).getNombre());
    }

    @Test
    void testObtenerPorIdExitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        Optional<Usuario> resultado = usuarioService.obtenerPorId(1L);
        assertTrue(resultado.isPresent());
        assertEquals("jperez@uteq.edu.ec", resultado.get().getEmail());
    }

    @Test
    void testCrearUsuario() {
        when(passwordEncoder.encode(any())).thenReturn("hash-encriptado");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        Usuario guardado = usuarioService.crear(usuario);
        assertNotNull(guardado);
        assertEquals("jperez@uteq.edu.ec", guardado.getEmail());
    }

    @Test
    void testCrearUsuarioIgnoraIdDelClienteParaEvitarSobrescribirUsuarioExistente() {
        // Hallazgo real: guardar el "usuario" recibido tal cual, con save(usuario), es solo
        // seguro si id=null. Si el id llega no-nulo (por ejemplo, 1L = el usuario ADMIN real),
        // Spring Data JPA hace merge() en vez de persist() y SOBRESCRIBE esa fila existente en
        // lugar de crear una nueva. crear() debe forzar id=null sin importar lo que traiga el
        // objeto de entrada.
        usuario.setId(1L); // simula un id de un usuario ya existente llegando en el body
        when(passwordEncoder.encode(any())).thenReturn("hash");

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.crear(usuario);

        assertNull(captor.getValue().getId(), "crear() debe forzar id=null antes de guardar, sin importar el id recibido");
    }

    @Test
    void testCrearUsuarioEncriptaLaContrasena() {
        usuario.setPassword("claveEnTextoPlano");
        when(passwordEncoder.encode("claveEnTextoPlano")).thenReturn("hash-bcrypt-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = usuarioService.crear(usuario);

        assertEquals("hash-bcrypt-simulado", guardado.getPassword());
        verify(passwordEncoder).encode("claveEnTextoPlano");
    }

    @Test
    void testCrearUsuarioAsignaRolUsuario() {
        // Regresión: crear() guardaba la columna 'rol' (string) pero dejaba 'rol_id' (FK) nulo,
        // lo que violaba la restricción NOT NULL de la base de datos real.
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = usuarioService.crear(usuario);

        assertNotNull(guardado.getRolUsuario());
        assertEquals("ESTUDIANTE", guardado.getRolUsuario().getCodigo());
    }

    @Test
    void testActualizarSincronizaRolUsuarioAlCambiarRol() {
        Usuario existente = new Usuario();
        existente.setId(1L);
        existente.setRol("ESTUDIANTE");
        existente.setRolUsuario(RolUsuario.builder().codigo("ESTUDIANTE").build());

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario cambios = new Usuario();
        cambios.setNombre("Juan");
        cambios.setApellido("Pérez");
        cambios.setEmail("jperez@uteq.edu.ec");
        cambios.setRol("COORDINADOR");

        Usuario actualizado = usuarioService.actualizar(1L, cambios);

        assertEquals("COORDINADOR", actualizado.getRol());
        assertEquals("COORDINADOR", actualizado.getRolUsuario().getCodigo());
    }

    @Test
    void testCrearUsuarioRechazaEmailDuplicado() {
        when(usuarioRepository.existsByEmail(usuario.getEmail())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> usuarioService.crear(usuario));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void testCambiarEstadoActivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        
        usuarioService.desactivar(1L);
        assertFalse(usuario.getActivo());

        usuarioService.activar(1L);
        assertTrue(usuario.getActivo());
    }
}
