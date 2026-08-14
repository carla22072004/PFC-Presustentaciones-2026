package ec.edu.uteq.presustentaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
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
@EnableJpaRepositories
@EnableAsync
@org.springframework.cache.annotation.EnableCaching
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

            // Buscar rol admin de la base de datos
            ec.edu.uteq.presustentaciones.entities.RolUsuario adminRol = rolUsuarioRepository.findByCodigo("ADMIN").orElse(null);

            // Único usuario sembrado: administrador del sistema
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

        } catch (Exception e) {
            log.error("Error al inicializar datos base: {}", e.getMessage());
        }
    }
}