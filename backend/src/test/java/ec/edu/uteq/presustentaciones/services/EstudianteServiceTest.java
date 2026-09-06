package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ActualizarEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.CrearEstudianteRequest;
import ec.edu.uteq.presustentaciones.dto.EstudianteDTO;
import ec.edu.uteq.presustentaciones.entities.*;
import ec.edu.uteq.presustentaciones.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EstudianteService no tenia ninguna prueba (0.98% lineas, 0% ramas antes de este archivo) --
 * es el servicio de alta/edicion explicita de estudiantes (admin/coordinador), separado de la
 * creacion implicita que hace SolicitudServiceImpl en la primera solicitud.
 */
@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolUsuarioRepository rolUsuarioRepository;
    @Mock private CarreraRepository carreraRepository;
    @Mock private PeriodoAcademicoRepository periodoAcademicoRepository;
    @Mock private EstadoAcademicoRepository estadoAcademicoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private EstudianteService estudianteService;

    private Usuario usuario;
    private Estudiante estudiante;
    private Carrera carrera;
    private PeriodoAcademico periodo;
    private EstadoAcademico activo;
    private RolUsuario rolEstudiante;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Ana").apellido("Torres").email("ana@uteq.edu.ec").activo(true).build();
        carrera = Carrera.builder().id(1).nombre("Ingeniería de Software").build();
        periodo = PeriodoAcademico.builder().id(1).nombre("2026-1").build();
        activo = EstadoAcademico.builder().codigo("ACTIVO").nombre("Activo").build();
        rolEstudiante = RolUsuario.builder().codigo("ESTUDIANTE").build();
        estudiante = Estudiante.builder().id(1L).usuario(usuario).carreraEntidad(carrera)
                .periodoIngreso(periodo).semestreActual((short) 3).telefono("0999999999")
                .expedienteCodigo("EXP-001").estadoAcademico(activo).build();
    }

    // ---- listarPaginado ----

    @Test
    void listarPaginadoDevuelveDtosConProyectoCuandoExiste() {
        Page<Estudiante> pagina = new PageImpl<>(List.of(estudiante));
        when(estudianteRepository.buscarPaginado(eq("ana"), any(PageRequest.class))).thenReturn(pagina);
        when(estudianteRepository.findUltimoProyectoPorEstudianteIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Tema X", "EN_EVALUACION"}));

        Page<EstudianteDTO> resultado = estudianteService.listarPaginado(0, 10, "ana");

        assertEquals(1, resultado.getTotalElements());
        EstudianteDTO dto = resultado.getContent().get(0);
        assertEquals("Tema X", dto.getProyectoTitulo());
        assertEquals("EN_EVALUACION", dto.getProyectoEstado());
        assertEquals("Ingeniería de Software", dto.getCarreraNombre());
    }

    @Test
    void listarPaginadoSinResultadosNoConsultaProyectos() {
        when(estudianteRepository.buscarPaginado(isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<EstudianteDTO> resultado = estudianteService.listarPaginado(0, 10, null);

        assertTrue(resultado.getContent().isEmpty());
        verify(estudianteRepository, never()).findUltimoProyectoPorEstudianteIds(any());
    }

    @Test
    void listarPaginadoAcotaPaginaYTamanioFueraDeRango() {
        when(estudianteRepository.buscarPaginado(any(), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        estudianteService.listarPaginado(-5, 500, null);

        verify(estudianteRepository).buscarPaginado(any(), eq(PageRequest.of(0, 100)));
    }

    // ---- obtenerPorId ----

    @Test
    void obtenerPorIdDevuelveDtoConProyecto() {
        when(estudianteRepository.findByIdWithUsuario(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findUltimoProyectoPorEstudianteIds(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Tema Y", "APROBADA"}));

        EstudianteDTO dto = estudianteService.obtenerPorId(1L);

        assertEquals("Tema Y", dto.getProyectoTitulo());
    }

    @Test
    void obtenerPorIdDevuelveDtoSinProyectoSiNoTiene() {
        when(estudianteRepository.findByIdWithUsuario(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findUltimoProyectoPorEstudianteIds(List.of(1L))).thenReturn(List.of());

        EstudianteDTO dto = estudianteService.obtenerPorId(1L);

        assertNull(dto.getProyectoTitulo());
    }

    @Test
    void obtenerPorIdLanzaSiNoExiste() {
        when(estudianteRepository.findByIdWithUsuario(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.obtenerPorId(99L));
        assertEquals("Estudiante no encontrado", ex.getMessage());
    }

    // ---- crear ----

    private CrearEstudianteRequest requestValido() {
        CrearEstudianteRequest req = new CrearEstudianteRequest();
        req.setNombre("Ana");
        req.setApellido("Torres");
        req.setEmail("ana@uteq.edu.ec");
        req.setPassword("secreto123");
        req.setCarreraId(1);
        return req;
    }

    @Test
    void crearLanzaSiFaltanCamposObligatorios() {
        CrearEstudianteRequest req = new CrearEstudianteRequest();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertTrue(ex.getMessage().contains("obligatorios"));
        verify(auditoriaService).marcarActorActual();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void crearLanzaSiEmailYaExiste() {
        CrearEstudianteRequest req = requestValido();
        when(usuarioRepository.existsByEmail("ana@uteq.edu.ec")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertTrue(ex.getMessage().contains("Ya existe un usuario"));
    }

    @Test
    void crearLanzaSiCarreraNoExiste() {
        CrearEstudianteRequest req = requestValido();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertEquals("Carrera no encontrada", ex.getMessage());
    }

    @Test
    void crearLanzaSiPeriodoIngresoIndicadoNoExiste() {
        CrearEstudianteRequest req = requestValido();
        req.setPeriodoIngresoId(5);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(periodoAcademicoRepository.findById(5)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertEquals("Período académico no encontrado", ex.getMessage());
    }

    @Test
    void crearLanzaSiEstadoActivoNoSembrado() {
        CrearEstudianteRequest req = requestValido();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(estadoAcademicoRepository.findByCodigo("ACTIVO")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertEquals("Catálogo de estados académicos no sembrado", ex.getMessage());
    }

    @Test
    void crearLanzaSiRolEstudianteNoExiste() {
        CrearEstudianteRequest req = requestValido();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(estadoAcademicoRepository.findByCodigo("ACTIVO")).thenReturn(Optional.of(activo));
        when(rolUsuarioRepository.findByCodigo("ESTUDIANTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.crear(req));
        assertEquals("Rol ESTUDIANTE no existe en el catálogo", ex.getMessage());
    }

    @Test
    void crearExitosoSinPeriodoUsaSemestrePorDefecto() {
        CrearEstudianteRequest req = requestValido(); // sin periodoIngresoId ni semestreActual
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(estadoAcademicoRepository.findByCodigo("ACTIVO")).thenReturn(Optional.of(activo));
        when(rolUsuarioRepository.findByCodigo("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(passwordEncoder.encode("secreto123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(estudianteRepository.generarCodigoExpediente(null, null)).thenReturn("EXP-010");
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteDTO dto = estudianteService.crear(req);

        assertEquals((short) 1, dto.getSemestreActual());
        assertEquals("Ingeniería de Software", dto.getCarreraNombre());
        assertNull(dto.getProyectoTitulo());
        verify(usuarioRepository).save(argThat(u -> "hash".equals(u.getPassword()) && u.getActivo()));
    }

    @Test
    void crearExitosoConPeriodoYSemestreExplicitos() {
        CrearEstudianteRequest req = requestValido();
        req.setPeriodoIngresoId(1);
        req.setSemestreActual((short) 4);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(periodoAcademicoRepository.findById(1)).thenReturn(Optional.of(periodo));
        when(estadoAcademicoRepository.findByCodigo("ACTIVO")).thenReturn(Optional.of(activo));
        when(rolUsuarioRepository.findByCodigo("ESTUDIANTE")).thenReturn(Optional.of(rolEstudiante));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudianteRepository.generarCodigoExpediente(null, null)).thenReturn("EXP-011");
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteDTO dto = estudianteService.crear(req);

        assertEquals((short) 4, dto.getSemestreActual());
        assertEquals("2026-1", dto.getPeriodoIngresoNombre());
    }

    // ---- actualizar ----

    @Test
    void actualizarLanzaSiEstudianteNoExiste() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> estudianteService.actualizar(99L, new ActualizarEstudianteRequest()));
        assertEquals("Estudiante no encontrado", ex.getMessage());
    }

    @Test
    void actualizarNoTocaCamposEnNull() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteDTO dto = estudianteService.actualizar(1L, new ActualizarEstudianteRequest());

        assertEquals((short) 3, dto.getSemestreActual());
        assertEquals("Ingeniería de Software", dto.getCarreraNombre());
        verifyNoInteractions(carreraRepository, periodoAcademicoRepository);
    }

    @Test
    void actualizarLanzaSiCarreraNuevaNoExiste() {
        ActualizarEstudianteRequest req = new ActualizarEstudianteRequest();
        req.setCarreraId(99);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(carreraRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.actualizar(1L, req));
        assertEquals("Carrera no encontrada", ex.getMessage());
    }

    @Test
    void actualizarLanzaSiPeriodoNuevoNoExiste() {
        ActualizarEstudianteRequest req = new ActualizarEstudianteRequest();
        req.setPeriodoIngresoId(77);
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(periodoAcademicoRepository.findById(77)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.actualizar(1L, req));
        assertEquals("Período académico no encontrado", ex.getMessage());
    }

    @Test
    void actualizarLanzaSiEstadoAcademicoInvalido() {
        ActualizarEstudianteRequest req = new ActualizarEstudianteRequest();
        req.setEstadoAcademicoCodigo("INEXISTENTE");
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(estadoAcademicoRepository.findByCodigo("INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> estudianteService.actualizar(1L, req));
        assertTrue(ex.getMessage().contains("INEXISTENTE"));
    }

    @Test
    void actualizarAplicaTodosLosCamposCuandoVienenTodos() {
        Carrera nuevaCarrera = Carrera.builder().id(2).nombre("Sistemas").build();
        PeriodoAcademico nuevoPeriodo = PeriodoAcademico.builder().id(2).nombre("2026-2").build();
        EstadoAcademico suspendido = EstadoAcademico.builder().codigo("SUSPENDIDO").nombre("Suspendido").build();
        ActualizarEstudianteRequest req = new ActualizarEstudianteRequest();
        req.setCarreraId(2);
        req.setPeriodoIngresoId(2);
        req.setSemestreActual((short) 6);
        req.setTelefono("0888888888");
        req.setEstadoAcademicoCodigo("SUSPENDIDO");

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(carreraRepository.findById(2)).thenReturn(Optional.of(nuevaCarrera));
        when(periodoAcademicoRepository.findById(2)).thenReturn(Optional.of(nuevoPeriodo));
        when(estadoAcademicoRepository.findByCodigo("SUSPENDIDO")).thenReturn(Optional.of(suspendido));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteDTO dto = estudianteService.actualizar(1L, req);

        assertEquals("Sistemas", dto.getCarreraNombre());
        assertEquals("2026-2", dto.getPeriodoIngresoNombre());
        assertEquals((short) 6, dto.getSemestreActual());
        assertEquals("SUSPENDIDO", dto.getEstadoAcademicoCodigo());
    }

    // ---- listarEstadosAcademicos ----

    @Test
    void listarEstadosAcademicosDelegaAlRepositorio() {
        when(estadoAcademicoRepository.findAll()).thenReturn(List.of(activo));
        List<EstadoAcademico> resultado = estudianteService.listarEstadosAcademicos();
        assertEquals(1, resultado.size());
        assertEquals("ACTIVO", resultado.get(0).getCodigo());
    }
}
