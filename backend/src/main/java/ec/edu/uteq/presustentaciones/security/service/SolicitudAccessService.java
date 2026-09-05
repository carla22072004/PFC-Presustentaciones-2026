package ec.edu.uteq.presustentaciones.security.service;

import ec.edu.uteq.presustentaciones.entities.Jurado;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Tutor;
import ec.edu.uteq.presustentaciones.repositories.JuradoRepository;
import ec.edu.uteq.presustentaciones.repositories.TutorRepository;
import ec.edu.uteq.presustentaciones.services.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Verifica que el usuario autenticado tenga relación real con una {@link Solicitud} antes de
 * exponer datos académicos asociados a ella (evaluaciones, rúbricas, anteproyecto). Mismo
 * patrón que {@code ActaServiceImpl.validarAcceso} / {@code NotificacionServiceImpl.validarAcceso}
 * (evita IDOR: nunca basta con "estar autenticado", hay que ser el estudiante dueño, un jurado
 * asignado, el tutor, o tener el permiso administrativo indicado), consolidado aquí porque
 * Evaluación, EvaluaciónJurado, RúbricaEvaluación y Anteproyecto comparten exactamente la misma
 * relación con Solicitud.
 */
@Service
@RequiredArgsConstructor
public class SolicitudAccessService {

    private final JuradoRepository juradoRepository;
    private final TutorRepository tutorRepository;
    private final PermisoService permisoService;

    /**
     * @param solicitud             la solicitud cuyo dato asociado se quiere leer/escribir
     * @param codigosPermisoBypass  códigos de permiso (p.ej. "EVALUACION_CALIFICAR",
     *                              "ANTEPROYECTO_REVISAR") cuya sola tenencia ya autoriza el
     *                              acceso, sin necesidad de participar directamente en la
     *                              solicitud -- refleja los mismos permisos que ya protegen las
     *                              acciones administrativas equivalentes en cada controlador.
     * @throws AccessDeniedException si el usuario no es ADMIN, no tiene ninguno de esos
     *                               permisos, y no participa en la solicitud como estudiante
     *                               dueño, jurado asignado o tutor.
     */
    public void validarAcceso(Solicitud solicitud, String... codigosPermisoBypass) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        for (String codigo : codigosPermisoBypass) {
            if (permisoService.tienePermiso(auth, codigo)) return;
        }

        String email = auth.getName();

        if (solicitud.getEstudiante() != null && solicitud.getEstudiante().getUsuario() != null
                && solicitud.getEstudiante().getUsuario().getEmail().equals(email)) {
            return;
        }

        List<Jurado> jurados = juradoRepository.findBySolicitudId(solicitud.getId());
        boolean esJurado = jurados.stream().anyMatch(j ->
                j.getDocente() != null && j.getDocente().getUsuario() != null
                        && j.getDocente().getUsuario().getEmail().equals(email));
        if (esJurado) return;

        Optional<Tutor> tutorOpt = tutorRepository.findBySolicitudId(solicitud.getId());
        if (tutorOpt.isPresent() && tutorOpt.get().getDocente() != null
                && tutorOpt.get().getDocente().getUsuario() != null
                && tutorOpt.get().getDocente().getUsuario().getEmail().equals(email)) {
            return;
        }

        throw new AccessDeniedException("No tienes permiso para acceder a la información de esta solicitud");
    }
}
