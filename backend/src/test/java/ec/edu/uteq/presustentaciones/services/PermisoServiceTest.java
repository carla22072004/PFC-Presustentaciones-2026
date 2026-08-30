package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.repositories.PermisoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PermisoService es el punto unico de autorizacion referenciado desde
 * @PreAuthorize("@permisoService.tienePermiso(...)") en todos los controllers protegidos
 * (ver UsuarioController y AuthControllerIntegrationTest). Sin tests dedicados pese a ser
 * el bean de seguridad mas invocado del backend.
 */
@ExtendWith(MockitoExtension.class)
class PermisoServiceTest {

    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private PermisoService permisoService;

    @Test
    void tienePermisoRetornaFalseSiAuthenticationEsNull() {
        assertFalse(permisoService.tienePermiso(null, "USUARIOS_GESTIONAR"));
        verify(permisoRepository, never()).usuarioTienePermiso(anyString(), anyString());
    }

    @Test
    void tienePermisoRetornaFalseSiNoEstaAutenticado() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user@uteq.edu.ec", "pass");
        auth.setAuthenticated(false);

        assertFalse(permisoService.tienePermiso(auth, "USUARIOS_GESTIONAR"));
        verify(permisoRepository, never()).usuarioTienePermiso(anyString(), anyString());
    }

    @Test
    void tienePermisoRetornaFalseParaUsuarioAnonimo() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertFalse(permisoService.tienePermiso(auth, "USUARIOS_GESTIONAR"));
        verify(permisoRepository, never()).usuarioTienePermiso(anyString(), anyString());
    }

    @Test
    void tienePermisoDelegaAlRepositorioConElEmailDelUsuarioAutenticado() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "coordinador@uteq.edu.ec", null, AuthorityUtils.createAuthorityList("ROLE_COORDINADOR"));
        when(permisoRepository.usuarioTienePermiso("coordinador@uteq.edu.ec", "USUARIOS_GESTIONAR"))
                .thenReturn(true);

        assertTrue(permisoService.tienePermiso(auth, "USUARIOS_GESTIONAR"));
        verify(permisoRepository).usuarioTienePermiso("coordinador@uteq.edu.ec", "USUARIOS_GESTIONAR");
    }

    @Test
    void tienePermisoRetornaFalseSiElRepositorioNoEncuentraElPermiso() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "estudiante@uteq.edu.ec", null, AuthorityUtils.createAuthorityList("ROLE_ESTUDIANTE"));
        when(permisoRepository.usuarioTienePermiso("estudiante@uteq.edu.ec", "USUARIOS_GESTIONAR"))
                .thenReturn(false);

        assertFalse(permisoService.tienePermiso(auth, "USUARIOS_GESTIONAR"));
    }
}
