package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Anteproyecto;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.AnteproyectoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AnteproyectoServiceImpl implements AnteproyectoService {

    private final AnteproyectoRepository anteproyectoRepository;
    private final SolicitudRepository solicitudRepository;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.upload.dir:uploads/anteproyectos}")
    private String uploadDir;

    public AnteproyectoServiceImpl(AnteproyectoRepository ar, SolicitudRepository sr,
                                   NotificacionService ns, UsuarioRepository ur) {
        this.anteproyectoRepository = ar;
        this.solicitudRepository = sr;
        this.notificacionService = ns;
        this.usuarioRepository = ur;
    }

    @Override
    public Anteproyecto enviarAnteproyecto(Long solicitudId, MultipartFile archivo) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if ("SUSPENDIDA".equals(solicitud.getEstado())) {
            throw new RuntimeException("Tu trabajo ha sido suspendido y no puedes subir archivos. Motivo: " + solicitud.getMotivoSuspension());
        }

        Path dirPath = Paths.get(uploadDir);
        try { Files.createDirectories(dirPath); } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads", e);
        }

        String nombreArchivo = "solicitud_" + solicitudId + "_" + UUID.randomUUID() + ".pdf";
        Path rutaArchivo = dirPath.resolve(nombreArchivo);
        String sha256 = calcularSha256YGuardar(archivo, rutaArchivo);

        Anteproyecto anteproyecto = anteproyectoRepository
                .findBySolicitudId(solicitudId).orElse(null);

        if (anteproyecto != null
                && anteproyecto.getArchivoPdf() != null
                && !"RECHAZADO".equals(anteproyecto.getEstado())) {
            throw new RuntimeException(
                    "No puedes reemplazar el PDF. El coordinador debe rechazar el anteproyecto para permitir una nueva carga.");
        }

        if (anteproyecto == null) anteproyecto = new Anteproyecto();

        anteproyecto.setArchivoPdf(nombreArchivo);
        anteproyecto.setFechaEnvio(LocalDate.now());
        anteproyecto.setEstado("ENVIADO");
        anteproyecto.setSolicitud(solicitud);
        anteproyecto.setSha256Hash(sha256);
        anteproyecto.setTamanoBytes(archivo.getSize());

        Anteproyecto guardado = anteproyectoRepository.save(anteproyecto);

        // Notificar a los admins que hay un anteproyecto nuevo para revisar
        notificarAdminsNuevoAnteproyecto(solicitud);

        return guardado;
    }

    private String calcularSha256YGuardar(MultipartFile archivo, Path destino) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = archivo.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                Files.copy(dis, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Error al calcular SHA-256 del archivo", e);
        }
    }

    @Override
    public boolean verificarIntegridad(Long solicitudId) {
        Anteproyecto ap = anteproyectoRepository.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Anteproyecto no encontrado"));

        if (ap.getSha256Hash() == null) return false;

        Path rutaArchivo = Paths.get(uploadDir).resolve(ap.getArchivoPdf()).normalize();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(rutaArchivo);
            String hashActual = HexFormat.of().formatHex(digest.digest(bytes));
            return hashActual.equals(ap.getSha256Hash());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Anteproyecto aprobarAnteproyecto(Long id, String obs) {
        Anteproyecto ap = anteproyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anteproyecto no encontrado"));
        ap.setEstado("APROBADO");
        ap.setObservaciones(obs);
        Anteproyecto guardado = anteproyectoRepository.save(ap);

        // Notificar al estudiante
        notificarEstudianteAnteproyecto(ap, true, obs);

        return guardado;
    }

    @Override
    public Anteproyecto rechazarAnteproyecto(Long id, String obs) {
        Anteproyecto ap = anteproyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anteproyecto no encontrado"));
        ap.setEstado("RECHAZADO");
        ap.setObservaciones(obs);
        Anteproyecto guardado = anteproyectoRepository.save(ap);

        // Notificar al estudiante
        notificarEstudianteAnteproyecto(ap, false, obs);

        return guardado;
    }

    @Override
    public Optional<Anteproyecto> buscarPorSolicitud(Long solicitudId) {
        return anteproyectoRepository.findBySolicitudId(solicitudId);
    }

    // ── Helpers de notificación ───────────────────────────────────────────────

    private void notificarAdminsNuevoAnteproyecto(Solicitud solicitud) {
        try {
            List<Usuario> admins = usuarioRepository.findByRol("ADMIN");
            String nombreEst = solicitud.getEstudiante().getUsuario().getNombre()
                    + " " + solicitud.getEstudiante().getUsuario().getApellido();
            for (Usuario admin : admins) {
                notificacionService.crearNotificacion(admin.getId(),
                        String.format("📄 El estudiante %s ha subido el PDF del anteproyecto \"%s\". " +
                                        "Está pendiente de revisión y aprobación.",
                                nombreEst, solicitud.getTituloTema()));
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar a admins sobre nuevo anteproyecto: {}", e.getMessage());
        }
    }

    private void notificarEstudianteAnteproyecto(Anteproyecto ap, boolean aprobado, String obs) {
        try {
            Long usuarioId = ap.getSolicitud().getEstudiante().getUsuario().getId();
            String titulo  = ap.getSolicitud().getTituloTema();
            String msg;
            if (aprobado) {
                msg = String.format("✅ Tu anteproyecto \"%s\" ha sido APROBADO. " +
                        "Ya puedes proceder con el siguiente paso del proceso.", titulo);
            } else {
                msg = String.format("❌ Tu anteproyecto \"%s\" ha sido RECHAZADO. " +
                        "Debes corregirlo y volver a cargarlo.", titulo);
            }
            if (obs != null && !obs.isBlank()) {
                msg += " Observaciones del coordinador: " + obs;
            }
            notificacionService.crearNotificacion(usuarioId, msg);
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante sobre anteproyecto: {}", e.getMessage());
        }
    }
}