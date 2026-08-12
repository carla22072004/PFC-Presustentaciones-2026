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
import java.util.List;
import java.util.Map;

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
public class PreSustentacionesApplication implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository rolUsuarioRepository;

    public static void main(String[] args) {
        SpringApplication.run(PreSustentacionesApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== INICIANDO DIAGNÓSTICO DE BASE DE DATOS ======");
        try {
            // 1. Mostrar columnas de la tabla estudiante
            try {
                List<Map<String, Object>> columnasEst = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_schema = 'presus' AND table_name = 'estudiante'"
                );
                System.out.println("Columnas de presus.estudiante:");
                for (Map<String, Object> col : columnasEst) {
                    System.out.println(" - " + col.get("column_name") + " (" + col.get("data_type") + 
                                       "), Nullable: " + col.get("is_nullable"));
                }
            } catch (Exception e) {
                System.out.println("Error al leer columnas de estudiante: " + e.getMessage());
            }

            // 2. Mostrar columnas de la tabla docente
            try {
                List<Map<String, Object>> columnasDoc = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_schema = 'presus' AND table_name = 'docente'"
                );
                System.out.println("Columnas de presus.docente:");
                for (Map<String, Object> col : columnasDoc) {
                    System.out.println(" - " + col.get("column_name") + " (" + col.get("data_type") + 
                                       "), Nullable: " + col.get("is_nullable"));
                }
            } catch (Exception e) {
                System.out.println("Error al leer columnas de docente: " + e.getMessage());
            }

            // 3. Mostrar columnas de la tabla carreras
            try {
                List<Map<String, Object>> columnasCar = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_schema = 'presus' AND table_name = 'carreras'"
                );
                System.out.println("Columnas de presus.carreras:");
                for (Map<String, Object> col : columnasCar) {
                    System.out.println(" - " + col.get("column_name") + " (" + col.get("data_type") + 
                                       "), Nullable: " + col.get("is_nullable"));
                }
            } catch (Exception e) {
                System.out.println("Error al leer columnas de carreras: " + e.getMessage());
            }

            // 4. Mostrar todas las tablas en el esquema presus
            try {
                List<String> tablas = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'presus' ORDER BY table_name",
                    String.class
                );
                System.out.println("Tablas en presus (" + tablas.size() + "):");
                for (String t : tablas) {
                    System.out.println(" - " + t);
                }
            } catch (Exception e) {
                System.out.println("Error al listar tablas: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Error en el diagnóstico: " + e.getMessage());
        }
        System.out.println("====================================================");

        initDemoData();
    }

    private void initDemoData() {
        System.out.println("====== SEMBRANDO DATOS DE PRUEBA INDIVIDUALES ======");
        try {
            // Mostrar columnas de la tabla facultades
            try {
                List<Map<String, Object>> columnasFac = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_schema = 'presus' AND table_name = 'facultades'"
                );
                System.out.println("Columnas de presus.facultades:");
                for (Map<String, Object> col : columnasFac) {
                    System.out.println(" - " + col.get("column_name") + " (" + col.get("data_type") + 
                                       "), Nullable: " + col.get("is_nullable"));
                }
            } catch (Exception e) {
                System.out.println("Error al leer columnas de facultades: " + e.getMessage());
            }

            // Insertar facultad de prueba si no existe
            try {
                jdbcTemplate.update(
                    "INSERT INTO presus.facultades (id, codigo, nombre) OVERRIDING SYSTEM VALUE VALUES (1, 'FCI', 'Facultad de Ciencias de la Ingeniería') ON CONFLICT (id) DO NOTHING"
                );
                System.out.println("Semilla de facultad verificada.");
            } catch (Exception e) {
                System.out.println("Error insertando facultad: " + e.getMessage() + " | Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "Ninguna"));
            }

            // Insertar carrera de prueba si no existe
            try {
                jdbcTemplate.update(
                    "INSERT INTO presus.carreras (id, facultad_id, codigo, nombre) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'ISW', 'Ingeniería en Software') ON CONFLICT (id) DO NOTHING"
                );
                System.out.println("Semilla de carrera verificada.");
            } catch (Exception e) {
                System.out.println("Error insertando carrera: " + e.getMessage() + " | Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "Ninguna"));
            }

            // Buscar rol admin de la base de datos
            ec.edu.uteq.presustentaciones.entities.RolUsuario adminRol = rolUsuarioRepository.findByCodigo("ADMIN").orElse(null);

            // Único usuario sembrado: administrador del sistema. El resto de cuentas
            // (coordinador, docentes, estudiantes) se crean desde el panel de gestión de usuarios.
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
                System.out.println("Usuario Admin sembrado con éxito.");
            }

        } catch (Exception e) {
            System.err.println("Error al sembrar datos de prueba: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("====================================================");
    }
}