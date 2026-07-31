package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.enums.RolUsuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

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
        usuario.setRol(RolUsuario.ESTUDIANTE);
        usuario.setActivo(true);
    }

    @Test
    void testObtenerTodos() {
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuario));
        List<Usuario> resultado = usuarioService.obtenerTodos();
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
    void testGuardarUsuario() {
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        Usuario guardado = usuarioService.guardar(usuario);
        assertNotNull(guardado);
        assertEquals(1L, guardado.getId());
    }

    @Test
    void testCambiarEstadoActivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        
        usuarioService.desactivar(1L);
        assertFalse(usuario.isActivo());

        usuarioService.activar(1L);
        assertTrue(usuario.isActivo());
    }
}
