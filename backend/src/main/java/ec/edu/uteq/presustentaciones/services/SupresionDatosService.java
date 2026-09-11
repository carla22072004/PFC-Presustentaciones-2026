package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.SolicitudSupresion;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.SolicitudSupresionRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RNF-19, tercer criterio: procedimiento de supresión de datos personales a solicitud del
 * titular. La resolución nunca borra el registro del usuario -- lo <b>seudonimiza</b>: el
 * expediente académico (solicitudes, actas, calificaciones) sigue enlazado al mismo id, porque
 * la normativa de titulación obliga a conservarlo (ver {@code docs/etica/RETENCION-DATOS.md}).
 * El registro de la propia solicitud tampoco guarda el dato suprimido -- solo referencia el id,
 * que sigue siendo válido después de la seudonimización.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupresionDatosService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudSupresionRepository solicitudRepository;

    public SolicitudSupresion solicitar(Long usuarioId) {
        if (solicitudRepository.existsByUsuarioIdAndEstado(usuarioId, "PENDIENTE")) {
            throw new IllegalStateException("Ya existe una solicitud de supresión pendiente para esta cuenta.");
        }
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        SolicitudSupresion solicitud = SolicitudSupresion.builder()
                .usuarioId(usuarioId)
                .fechaSolicitud(LocalDateTime.now())
                .estado("PENDIENTE")
                .build();
        return solicitudRepository.save(solicitud);
    }

    public List<SolicitudSupresion> listar() {
        return solicitudRepository.findAllByOrderByFechaSolicitudDesc();
    }

    /**
     * @param aceptar true para seudonimizar la cuenta, false para rechazar la solicitud (p. ej.
     *                porque el titular tiene un proceso de titulación en curso que la normativa
     *                obliga a poder identificar)
     */
    @Transactional
    public SolicitudSupresion resolver(Long solicitudId, boolean aceptar, Long resueltoPorId, String notas) {
        SolicitudSupresion solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud de supresión no encontrada."));
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalStateException("Esta solicitud ya fue resuelta.");
        }

        if (aceptar) {
            seudonimizar(solicitud.getUsuarioId());
            solicitud.setTipoResolucion("SEUDONIMIZACION");
            solicitud.setEstado("RESUELTA");
        } else {
            solicitud.setTipoResolucion("RECHAZADA");
            solicitud.setEstado("RECHAZADA");
        }
        solicitud.setResueltoPor(resueltoPorId);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setNotas(notas);
        return solicitudRepository.save(solicitud);
    }

    /**
     * Reemplaza los campos identificables por marcadores no identificables y desactiva la
     * cuenta. No borra la fila: el expediente académico enlazado al mismo id (solicitudes,
     * actas, evaluaciones) debe seguir existiendo para cumplir la obligación legal de
     * conservación -- ver RETENCION-DATOS.md.
     */
    private void seudonimizar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        usuario.setNombre("Usuario suprimido");
        usuario.setApellido("#" + usuario.getId());
        usuario.setEmail("suprimido-" + usuario.getId() + "@presustentaciones.invalid");
        usuario.setTelefono(null);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.warn("RNF-19: usuario {} seudonimizado por resolución de solicitud de supresión.", usuarioId);
    }
}
