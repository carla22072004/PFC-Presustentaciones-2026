package ec.edu.uteq.presustentaciones.bootstrap;

import ec.edu.uteq.presustentaciones.entities.RolUsuario;
import ec.edu.uteq.presustentaciones.entities.Usuario;
import ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * RNF-15: siembra las cuentas de demostración (admin@/demo@/docente@/estudiante@uteq.edu.ec)
 * con contraseñas literales en el código. Antes de esta fase corría en TODO arranque, sin
 * importar el entorno -- este componente ahora solo existe bajo el perfil {@code dev}
 * (activar con {@code SPRING_PROFILES_ACTIVE=dev}, como ya hace {@code docker-compose.override.yml}
 * para desarrollo local). Un despliegue sin ese perfil activo nunca instancia esta clase: cero
 * cuentas con contraseña conocida en el código. El bootstrap de un despliegue real vive en
 * {@link AdminBootstrap}, que toma la contraseña del entorno y valida contra
 * {@code PasswordPolicyValidator} en vez de sembrar una constante.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolUsuarioRepository rolUsuarioRepository;

    @Override
    public void run(String... args) {
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

            RolUsuario adminRol = rolUsuarioRepository.findByCodigo("ADMIN").orElse(null);
            RolUsuario coordinadorRol = rolUsuarioRepository.findByCodigo("COORDINADOR").orElse(null);

            // Usuario administrador del sistema
            if (!usuarioRepository.existsByEmail("admin@uteq.edu.ec")) {
                Usuario admin = Usuario.builder()
                    .nombre("Admin")
                    .apellido("Sistema")
                    .email("admin@uteq.edu.ec")
                    .password(passwordEncoder.encode("Admin2026!"))
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

            RolUsuario docenteRol = rolUsuarioRepository.findByCodigo("DOCENTE").orElse(null);
            RolUsuario estudianteRol = rolUsuarioRepository.findByCodigo("ESTUDIANTE").orElse(null);

            // Usuario Docente / Tutor / Jurado
            if (!usuarioRepository.existsByEmail("docente@uteq.edu.ec")) {
                Usuario docenteUser = Usuario.builder()
                    .nombre("Docente")
                    .apellido("Tutor")
                    .email("docente@uteq.edu.ec")
                    .password(passwordEncoder.encode("Docente2026!"))
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
                    .password(passwordEncoder.encode("Estudiante2026!"))
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
            log.error("Error al inicializar datos de demostración: {}", e.getMessage());
        }
    }
}
