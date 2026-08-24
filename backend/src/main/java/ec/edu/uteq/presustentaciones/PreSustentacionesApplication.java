package ec.edu.uteq.presustentaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Sistema de Gestión de Pre-Sustentaciones de Trabajos de Titulación
 * Universidad Técnica Estatal de Quevedo
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@SpringBootApplication
@Slf4j
public class PreSustentacionesApplication implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolUsuarioRepository rolUsuarioRepository;

    public static void main(String[] args) {
        SpringApplication.run(PreSustentacionesApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        initDemoData();
    }

    private void initDemoData() {
        try {
            // Insertar facultad inicial si no existe
            try {
                jdbcTemplate.update(
                    "INSERT INTO presus.facultades (id, codigo, nombre) OVERRIDING SYSTEM VALUE VALUES (1, 'FCI', 'Facultad de Ciencias de la Ingeniería') ON CONFLICT (id) DO NOTHING"
                );
            } catch (Exception e) {
                log.warn("Verificación de facultad inicial: {}", e.getMessage());
            }

            // Insertar carrera inicial si no existe
            try {
                jdbcTemplate.update(
                    "INSERT INTO presus.carreras (id, facultad_id, codigo, nombre) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'ISW', 'Ingeniería en Software') ON CONFLICT (id) DO NOTHING"
                );
            } catch (Exception e) {
                log.warn("Verificación de carrera inicial: {}", e.getMessage());
            }

            // Sembrar catalogo de roles si no existe (ninguna migracion los inserta:
            // roles_usuario.id no es autogenerado, requiere valores explicitos)
            try {
                jdbcTemplate.update(
                    "INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES " +
                    "(1, 'ADMIN', 'Administrador'), (2, 'DOCENTE', 'Docente'), " +
                    "(3, 'COORDINADOR', 'Coordinador'), (4, 'ESTUDIANTE', 'Estudiante') " +
                    "ON CONFLICT (id) DO NOTHING"
                );
            } catch (Exception e) {
                log.warn("Verificación de catálogo de roles: {}", e.getMessage());
            }

            // Buscar rol admin de la base de datos
            ec.edu.uteq.presustentaciones.entities.RolUsuario adminRol = rolUsuarioRepository.findByCodigo("ADMIN").orElse(null);
            ec.edu.uteq.presustentaciones.entities.RolUsuario coordinadorRol = rolUsuarioRepository.findByCodigo("COORDINADOR").orElse(null);

            // Usuario administrador del sistema
            if (!usuarioRepository.existsByEmail("admin@uteq.edu.ec")) {
                Usuario admin = Usuario.builder()
                    .nombre("Admin")
                    .apellido("Sistema")
                    .email("admin@uteq.edu.ec")
                    .password(passwordEncoder.encode("admin123"))
                    .rol("ADMIN")
                    .rolUsuario(adminRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(admin);
                log.info("Usuario administrador inicial verificado.");
            }

            // Usuario de demostración (Fase 8, criterio P5): credenciales publicadas en
            // README.md para que el tribunal pueda entrar sin registrarse. Rol COORDINADOR
            // porque expone el flujo académico completo (asignar jurados, programar
            // cronograma, ver reportes) sin ser una cuenta de administración del sistema.
            if (!usuarioRepository.existsByEmail("demo@uteq.edu.ec")) {
                Usuario demo = Usuario.builder()
                    .nombre("Usuario")
                    .apellido("Demostración")
                    .email("demo@uteq.edu.ec")
                    .password(passwordEncoder.encode("Demo2026!"))
                    .rol("COORDINADOR")
                    .rolUsuario(coordinadorRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(demo);
                log.info("Usuario de demostración inicial verificado.");
            }

            ec.edu.uteq.presustentaciones.entities.RolUsuario docenteRol = rolUsuarioRepository.findByCodigo("DOCENTE").orElse(null);
            ec.edu.uteq.presustentaciones.entities.RolUsuario estudianteRol = rolUsuarioRepository.findByCodigo("ESTUDIANTE").orElse(null);

            // Usuario Docente / Tutor / Jurado
            if (!usuarioRepository.existsByEmail("docente@uteq.edu.ec")) {
                Usuario docenteUser = Usuario.builder()
                    .nombre("Docente")
                    .apellido("Tutor")
                    .email("docente@uteq.edu.ec")
                    .password(passwordEncoder.encode("docente123"))
                    .rol("DOCENTE")
                    .rolUsuario(docenteRol)
                    .activo(true)
                    .build();
                Usuario savedDocente = usuarioRepository.save(docenteUser);
                jdbcTemplate.update(
                    "INSERT INTO presus.docente (usuario_id, facultad_id, area_especialidad, carga_horaria_semanal, disponible, creado_en) " +
                    "VALUES (?, 1, 'Ingeniería de Software', 20, true, now()) ON CONFLICT (usuario_id) DO NOTHING",
                    savedDocente.getId()
                );
                log.info("Usuario docente inicial verificado.");
            }

            // Usuario Estudiante
            if (!usuarioRepository.existsByEmail("estudiante@uteq.edu.ec")) {
                Usuario estUser = Usuario.builder()
                    .nombre("Estudiante")
                    .apellido("Pregrado")
                    .email("estudiante@uteq.edu.ec")
                    .password(passwordEncoder.encode("estudiante123"))
                    .rol("ESTUDIANTE")
                    .rolUsuario(estudianteRol)
                    .activo(true)
                    .build();
                Usuario savedEst = usuarioRepository.save(estUser);
                jdbcTemplate.update(
                    "INSERT INTO presus.estudiante (usuario_id, carrera_id, carrera, semestre, semestre_actual, expediente_codigo, telefono, creado_en) " +
                    "VALUES (?, 1, 'Ingeniería en Software', '8vo', 8, 'EXP-2026-001', '0999999999', now()) ON CONFLICT (usuario_id) DO NOTHING",
                    savedEst.getId()
                );
                log.info("Usuario estudiante inicial verificado.");
            }

        } catch (Exception e) {
            log.error("Error al inicializar datos base: {}", e.getMessage());
        }
    }
}
