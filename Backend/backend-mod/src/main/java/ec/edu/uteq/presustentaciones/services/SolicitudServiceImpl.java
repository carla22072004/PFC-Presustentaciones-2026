package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.Carrera;
import ec.edu.uteq.presustentaciones.entities.ConvocatoriaTitulacion;
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.ModalidadTitulacion;
import ec.edu.uteq.presustentaciones.entities.PeriodoAcademico;
import ec.edu.uteq.presustentaciones.entities.Solicitud;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.AnteproyectoRepository;
import ec.edu.uteq.presustentaciones.repositories.CarreraRepository;
import ec.edu.uteq.presustentaciones.repositories.ConvocatoriaTitulacionRepository;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.ModalidadTitulacionRepository;
import ec.edu.uteq.presustentaciones.repositories.PeriodoAcademicoRepository;
import ec.edu.uteq.presustentaciones.repositories.SolicitudRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final AnteproyectoRepository anteproyectoRepository;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final ec.edu.uteq.presustentaciones.repositories.EstadoSolicitudRepository estadoSolicitudRepository;
    private final ModalidadTitulacionRepository modalidadTitulacionRepository;
    private final ConvocatoriaTitulacionRepository convocatoriaTitulacionRepository;
    private final CarreraRepository carreraRepository;
    private final PeriodoAcademicoRepository periodoAcademicoRepository;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void notificarAdmins(String mensaje) {
        List<Usuario> admins = usuarioRepository.findByRol("ADMIN");
        for (Usuario admin : admins) {
            try {
                notificacionService.crearNotificacion(admin.getId(), mensaje);
            } catch (Exception e) {
                log.warn("No se pudo notificar al coordinador ID {}: {}", admin.getId(), e.getMessage());
            }
        }
    }

    private void notificarEstudiante(Solicitud solicitud, String mensaje) {
        try {
            Long usuarioId = solicitud.getEstudiante().getUsuario().getId();
            notificacionService.crearNotificacion(usuarioId, mensaje);
        } catch (Exception e) {
            log.warn("No se pudo notificar al estudiante de solicitud ID {}: {}", solicitud.getId(), e.getMessage());
        }
    }

    // ─── Métodos ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Solicitud crearSolicitud(Long estudianteId, Solicitud datos) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + estudianteId));

        // Resolver estado inicial
        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoCreada = estadoSolicitudRepository.findByCodigo("CREADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("CREADA").nombre("Creada").build()));

        // Resolver modalidad: si el objeto ya viene completo (con id) úsalo; si no, error
        if (datos.getModalidadTitulacion() == null || datos.getModalidadTitulacion().getId() == null) {
            throw new RuntimeException("Debe seleccionar una modalidad de titulación válida");
        }
        ModalidadTitulacion modalidad = modalidadTitulacionRepository
                .findById(datos.getModalidadTitulacion().getId())
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada con ID: " + datos.getModalidadTitulacion().getId()));
        datos.setModalidadTitulacion(modalidad);

        // Resolver convocatoria: si viene en el body úsala, si no buscar/crear la activa
        if (datos.getConvocatoria() == null || datos.getConvocatoria().getId() == null) {
            ConvocatoriaTitulacion convActiva = convocatoriaTitulacionRepository
                    .findFirstByActivaTrue()
                    .orElseGet(this::crearConvocatoriaDefault);
            datos.setConvocatoria(convActiva);
        } else {
            ConvocatoriaTitulacion convocatoria = convocatoriaTitulacionRepository
                    .findById(datos.getConvocatoria().getId())
                    .orElseThrow(() -> new RuntimeException("Convocatoria no encontrada con ID: " + datos.getConvocatoria().getId()));
            datos.setConvocatoria(convocatoria);
        }

        datos.setEstado(estadoCreada);
        datos.setEstudiante(estudiante);
        datos.setCreadoPor(estudiante.getUsuario());
        datos.setActualizadoPor(estudiante.getUsuario());
        datos.setFechaRegistro(LocalDateTime.now());
        datos.setActualizadoEn(LocalDateTime.now());
        return solicitudRepository.save(datos);
    }
 
    @Override
    @Transactional
    public Solicitud crearSolicitudPorUsuario(Long usuarioId, Solicitud datos) {
        // Buscar perfil de estudiante; si no existe, crearlo automáticamente
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPerfilEstudiante(usuarioId));
        return crearSolicitud(estudiante.getId(), datos);
    }

    /**
     * Crea automáticamente un PeriodoAcademico + ConvocatoriaTitulacion activos
     * cuando la base de datos no tiene ninguno configurado (instalación inicial).
     */
    @Transactional
    private ConvocatoriaTitulacion crearConvocatoriaDefault() {
        int anio = java.time.Year.now().getValue();

        // Crear o reusar período académico del año actual
        PeriodoAcademico periodo = periodoAcademicoRepository
                .findByCodigo("PA-" + anio)
                .orElseGet(() -> {
                    log.info("Creando período académico por defecto para año {}", anio);
                    return periodoAcademicoRepository.save(PeriodoAcademico.builder()
                            .codigo("PA-" + anio)
                            .nombre("Período Académico " + anio)
                            .fechaInicio(LocalDate.of(anio, 1, 1))
                            .fechaFin(LocalDate.of(anio, 12, 31))
                            .activo(true)
                            .build());
                });

        // Crear convocatoria activa ligada al período
        log.info("Creando convocatoria activa por defecto para período {}", periodo.getCodigo());
        return convocatoriaTitulacionRepository.save(ConvocatoriaTitulacion.builder()
                .codigo("CONV-" + anio + "-01")
                .nombre("Convocatoria " + anio + " – Período I")
                .periodoAcademico(periodo)
                .fechaInicio(LocalDate.of(anio, 1, 1))
                .fechaFin(LocalDate.of(anio, 12, 31))
                .activa(true)
                .build());
    }

    /**
     * Crea automáticamente el perfil Estudiante para un usuario con rol ESTUDIANTE
     * que aún no tenga registro en la tabla estudiante.
     */
    @Transactional
    private Estudiante crearPerfilEstudiante(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        // Verificar que realmente sea un estudiante
        if (!"ESTUDIANTE".equalsIgnoreCase(usuario.getRol())) {
            throw new RuntimeException("El usuario no tiene rol de estudiante");
        }

        // Obtener la primera carrera disponible como default
        Carrera carreraDefault = carreraRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay carreras configuradas en el sistema. Contacte al administrador."));

        log.info("Creando perfil de estudiante automáticamente para usuario ID: {}", usuarioId);

        Estudiante nuevoEstudiante = Estudiante.builder()
                .usuario(usuario)
                .carrera(carreraDefault.getNombre())
                .carreraEntidad(carreraDefault)
                .semestreActual((short) 1)
                .semestre("1ro")
                .build();

        return estudianteRepository.save(nuevoEstudiante);
    }
 
    @Override
    public List<Solicitud> listarPorUsuario(Long usuarioId) {
        return estudianteRepository.findByUsuarioId(usuarioId)
                .map(e -> solicitudRepository.findByEstudianteId(e.getId()))
                .orElse(java.util.Collections.emptyList());
    }
 
    @Override
    @Transactional
    public Solicitud enviarSolicitud(Long solicitudId) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
 
        boolean tienePdf = anteproyectoRepository.findBySolicitudId(solicitudId)
                .map(a -> a.getArchivoPdf() != null && !a.getArchivoPdf().isBlank())
                .orElse(false);
 
        if (!tienePdf) {
            throw new RuntimeException("Debes cargar el PDF del anteproyecto antes de enviar la solicitud a revisión.");
        }
 
        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoEnviada = estadoSolicitudRepository.findByCodigo("ENVIADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("ENVIADA").nombre("Enviada").build()));

        s.setEstado(estadoEnviada);
        Solicitud guardada = solicitudRepository.save(s);
 
        String nombreEstudiante = s.getEstudiante().getUsuario().getNombre()
                + " " + s.getEstudiante().getUsuario().getApellido();
 
        notificarAdmins(String.format(
                "📋 Nueva solicitud de %s: \"%s\" está pendiente de revisión.",
                nombreEstudiante, s.getTituloTema()));
 
        return guardada;
    }
 
    @Override
    @Transactional
    public Solicitud aprobarSolicitud(Long solicitudId) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoAprobada = estadoSolicitudRepository.findByCodigo("APROBADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("APROBADA").nombre("Aprobada").build()));

        s.setEstado(estadoAprobada);
        Solicitud guardada = solicitudRepository.save(s);
 
        notificarEstudiante(s, String.format(
                "✅ Tu solicitud \"%s\" ha sido APROBADA. Pronto se te asignará fecha y tribunal.",
                s.getTituloTema()));
 
        return guardada;
    }
 
    @Override
    @Transactional
    public Solicitud rechazarSolicitud(Long solicitudId) {
        return rechazarConObservacion(solicitudId, null);
    }
 
    @Override
    @Transactional
    public Solicitud rechazarConObservacion(Long solicitudId, String observacion) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoRechazada = estadoSolicitudRepository.findByCodigo("RECHAZADA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("RECHAZADA").nombre("Rechazada").build()));

        s.setEstado(estadoRechazada);
        if (observacion != null && !observacion.isBlank()) {
            s.setObservaciones(observacion);
        }
        Solicitud guardada = solicitudRepository.save(s);
 
        String obs = (s.getObservaciones() != null && !s.getObservaciones().isBlank())
                ? " Motivo: " + s.getObservaciones() : "";
        notificarEstudiante(s, String.format(
                "❌ Tu solicitud \"%s\" ha sido RECHAZADA.%s Revisa las observaciones.",
                s.getTituloTema(), obs));
 
        return guardada;
    }
 
    @Override
    public List<Solicitud> listarSolicitudes() {
        return solicitudRepository.findAllWithEstudiante();
    }
 
    @Override
    public List<Solicitud> listarPorEstudiante(Long estudianteId) {
        return solicitudRepository.findByEstudianteId(estudianteId);
    }
 
    @Override
    public Optional<Solicitud> obtenerPorId(Long id) {
        return solicitudRepository.findById(id);
    }
 
    @Override
    @Transactional
    public Solicitud suspenderSolicitud(Long solicitudId, String motivo) {
        Solicitud s = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
 
        String codEstado = s.getEstado() != null ? s.getEstado().getCodigo() : "";
        boolean esSuspendible = !"CREADA".equals(codEstado) && !"RECHAZADA".equals(codEstado) && !"SUSPENDIDA".equals(codEstado);
        if (!esSuspendible) {
            throw new RuntimeException("La solicitud no puede ser suspendida en su estado actual: " + codEstado);
        }
 
        if (motivo == null || motivo.isBlank()) {
            throw new RuntimeException("Debe especificar el motivo de la suspensión");
        }
 
        ec.edu.uteq.presustentaciones.entities.EstadoSolicitud estadoSuspendida = estadoSolicitudRepository.findByCodigo("SUSPENDIDA")
                .orElseGet(() -> estadoSolicitudRepository.save(ec.edu.uteq.presustentaciones.entities.EstadoSolicitud.builder()
                        .codigo("SUSPENDIDA").nombre("Suspendida").build()));

        s.setEstado(estadoSuspendida);
        s.setMotivoSuspension(motivo);
        s.setSuspendidoEn(LocalDateTime.now());
 
        Solicitud guardada = solicitudRepository.save(s);
        log.info("Solicitud {} suspendida desde estado {} por motivo: {}", solicitudId, codEstado, motivo);
 
        notificarEstudiante(s, String.format(
                "🚫 Tu trabajo \"%s\" ha sido SUSPENDIDO. Motivo: %s. No podrás continuar.",
                s.getTituloTema(), motivo));

        return guardada;
    }
}