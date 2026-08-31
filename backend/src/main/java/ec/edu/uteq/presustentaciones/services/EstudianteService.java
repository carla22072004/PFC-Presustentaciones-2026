package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ActualizarEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.CrearEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.EstudianteDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Gestión de estudiantes" -- antes de esto, un Estudiante solo se creaba como
 * efecto secundario de la primera solicitud (SolicitudServiceImpl.crearPerfilEstudiante),
 * con la primera carrera del catálogo y semestre fijo en 1, sin forma de corregirlo
 * después. Este servicio agrega el alta explícita (admin/coordinador) y la edición de
 * carrera, semestre, período de ingreso y estado académico.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final CarreraRepository carreraRepository;
    private final PeriodoAcademicoRepository periodoAcademicoRepository;
    private final EstadoAcademicoRepository estadoAcademicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    /**
     * @param page número de página, base 0
     * @param size tamaño de página (se acota a un máximo de 100)
     * @param q    texto libre de búsqueda por nombre/apellido/email, o {@code null}
     * @return página de estudiantes con su último proyecto de titulación (si tiene)
     */
    @Transactional(readOnly = true)
    public Page<EstudianteDTO> listarPaginado(int page, int size, String q) {
        int paginaSegura = Math.max(page, 0);
        int tamanioSeguro = Math.min(Math.max(size, 1), 100);
        Page<Estudiante> pagina = estudianteRepository.buscarPaginado(q, PageRequest.of(paginaSegura, tamanioSeguro));

        List<Long> ids = pagina.getContent().stream().map(Estudiante::getId).toList();
        Map<Long, Object[]> proyectos = ids.isEmpty() ? Map.of() : estudianteRepository
                .findUltimoProyectoPorEstudianteIds(ids).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> r));

        return pagina.map(e -> toDto(e, proyectos.get(e.getId())));
    }

    /**
     * @param id id del estudiante
     * @return el estudiante con su último proyecto de titulación (si tiene)
     * @throws RuntimeException si el estudiante no existe
     */
    @Transactional(readOnly = true)
    public EstudianteDTO obtenerPorId(Long id) {
        Estudiante e = estudianteRepository.findByIdWithUsuario(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        List<Object[]> proyectos = estudianteRepository.findUltimoProyectoPorEstudianteIds(List.of(id));
        return toDto(e, proyectos.isEmpty() ? null : proyectos.get(0));
    }

    /**
     * Registra el usuario (rol ESTUDIANTE) y su perfil académico en un solo paso.
     *
     * @param req datos del estudiante a crear (nombre, apellido, email, contraseña, carrera
     *            obligatorios; período de ingreso, teléfono y semestre opcionales)
     * @return el estudiante creado
     * @throws RuntimeException si faltan campos obligatorios, el email ya está en uso, o la
     *                          carrera/período no existen
     */
    public EstudianteDTO crear(CrearEstudianteRequest req) {
        auditoriaService.marcarActorActual();

        if (req.getNombre() == null || req.getNombre().isBlank()
                || req.getApellido() == null || req.getApellido().isBlank()
                || req.getEmail() == null || req.getEmail().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()
                || req.getCarreraId() == null) {
            throw new RuntimeException("Nombre, apellido, email, contraseña y carrera son obligatorios.");
        }
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con el email: " + req.getEmail());
        }

        Carrera carrera = carreraRepository.findById(req.getCarreraId())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
        PeriodoAcademico periodo = req.getPeriodoIngresoId() != null
                ? periodoAcademicoRepository.findById(req.getPeriodoIngresoId())
                        .orElseThrow(() -> new RuntimeException("Período académico no encontrado"))
                : null;
        EstadoAcademico activo = estadoAcademicoRepository.findByCodigo("ACTIVO")
                .orElseThrow(() -> new RuntimeException("Catálogo de estados académicos no sembrado"));
        RolUsuario rolEstudiante = rolUsuarioRepository.findByCodigo("ESTUDIANTE")
                .orElseThrow(() -> new RuntimeException("Rol ESTUDIANTE no existe en el catálogo"));

        Usuario usuario = Usuario.builder()
                .nombre(req.getNombre())
                .apellido(req.getApellido())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .telefono(req.getTelefono())
                .rol("ESTUDIANTE")
                .rolUsuario(rolEstudiante)
                .activo(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        short semestreActual = req.getSemestreActual() != null ? req.getSemestreActual() : (short) 1;
        String expedienteCodigo = estudianteRepository.generarCodigoExpediente(null, null);

        Estudiante estudiante = Estudiante.builder()
                .usuario(usuario)
                .carrera(carrera.getNombre())
                .carreraEntidad(carrera)
                .periodoIngreso(periodo)
                .semestreActual(semestreActual)
                .semestre(semestreActual + "")
                .telefono(req.getTelefono())
                .expedienteCodigo(expedienteCodigo)
                .estadoAcademico(activo)
                .build();
        estudiante = estudianteRepository.save(estudiante);

        return toDto(estudiante, null);
    }

    /**
     * Actualiza solo los campos de perfil académico enviados (carrera, período de ingreso,
     * semestre, teléfono, estado académico); los campos {@code null} en {@code req} se dejan
     * sin tocar.
     *
     * @param id  id del estudiante a actualizar
     * @param req campos a actualizar; cualquier campo en {@code null} no se modifica
     * @return el estudiante actualizado
     * @throws RuntimeException si el estudiante no existe, o la carrera/período/estado
     *                          académico indicados no existen
     */
    public EstudianteDTO actualizar(Long id, ActualizarEstudianteRequest req) {
        auditoriaService.marcarActorActual();
        Estudiante e = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        if (req.getCarreraId() != null) {
            Carrera carrera = carreraRepository.findById(req.getCarreraId())
                    .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
            e.setCarreraEntidad(carrera);
            e.setCarrera(carrera.getNombre());
        }
        if (req.getPeriodoIngresoId() != null) {
            PeriodoAcademico periodo = periodoAcademicoRepository.findById(req.getPeriodoIngresoId())
                    .orElseThrow(() -> new RuntimeException("Período académico no encontrado"));
            e.setPeriodoIngreso(periodo);
        }
        if (req.getSemestreActual() != null) {
            e.setSemestreActual(req.getSemestreActual());
            e.setSemestre(req.getSemestreActual() + "");
        }
        if (req.getTelefono() != null) {
            e.setTelefono(req.getTelefono());
        }
        if (req.getEstadoAcademicoCodigo() != null && !req.getEstadoAcademicoCodigo().isBlank()) {
            EstadoAcademico estado = estadoAcademicoRepository.findByCodigo(req.getEstadoAcademicoCodigo())
                    .orElseThrow(() -> new RuntimeException("Estado académico inválido: " + req.getEstadoAcademicoCodigo()));
            e.setEstadoAcademico(estado);
        }

        Estudiante guardado = estudianteRepository.save(e);
        return toDto(guardado, null);
    }

    /** @return el catálogo completo de estados académicos disponibles */
    @Transactional(readOnly = true)
    public List<EstadoAcademico> listarEstadosAcademicos() {
        return estadoAcademicoRepository.findAll();
    }

    private EstudianteDTO toDto(Estudiante e, Object[] proyecto) {
        Usuario u = e.getUsuario();
        return EstudianteDTO.builder()
                .id(e.getId())
                .usuarioId(u.getId())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .email(u.getEmail())
                .activo(u.getActivo())
                .telefono(e.getTelefono())
                .expedienteCodigo(e.getExpedienteCodigo())
                .carreraId(e.getCarreraEntidad() != null ? e.getCarreraEntidad().getId() : null)
                .carreraNombre(e.getCarreraEntidad() != null ? e.getCarreraEntidad().getNombre() : e.getCarrera())
                .periodoIngresoId(e.getPeriodoIngreso() != null ? e.getPeriodoIngreso().getId() : null)
                .periodoIngresoNombre(e.getPeriodoIngreso() != null ? e.getPeriodoIngreso().getNombre() : null)
                .semestreActual(e.getSemestreActual())
                .estadoAcademicoCodigo(e.getEstadoAcademico() != null ? e.getEstadoAcademico().getCodigo() : null)
                .estadoAcademicoNombre(e.getEstadoAcademico() != null ? e.getEstadoAcademico().getNombre() : null)
                .proyectoTitulo(proyecto != null ? (String) proyecto[1] : null)
                .proyectoEstado(proyecto != null ? (String) proyecto[2] : null)
                .build();
    }
}
