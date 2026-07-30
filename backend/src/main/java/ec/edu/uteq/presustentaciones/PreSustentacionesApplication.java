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
import ec.edu.uteq.presustentaciones.entities.Estudiante;
import ec.edu.uteq.presustentaciones.entities.Docente;
import ec.edu.uteq.presustentaciones.repositories.UsuarioRepository;
import ec.edu.uteq.presustentaciones.repositories.EstudianteRepository;
import ec.edu.uteq.presustentaciones.repositories.DocenteRepository;
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
    private EstudianteRepository estudianteRepository;

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ec.edu.uteq.presustentaciones.repositories.RolUsuarioRepository rolUsuarioRepository;

    @Autowired
    private ec.edu.uteq.presustentaciones.repositories.CarreraRepository carreraRepository;

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

            // Buscar roles de la base de datos
            ec.edu.uteq.presustentaciones.entities.RolUsuario adminRol = rolUsuarioRepository.findByCodigo("ADMIN").orElse(null);
            ec.edu.uteq.presustentaciones.entities.RolUsuario coordRol = rolUsuarioRepository.findByCodigo("COORDINADOR").orElse(null);
            ec.edu.uteq.presustentaciones.entities.RolUsuario estRol = rolUsuarioRepository.findByCodigo("ESTUDIANTE").orElse(null);
            ec.edu.uteq.presustentaciones.entities.RolUsuario docenteRol = rolUsuarioRepository.findByCodigo("DOCENTE").orElse(null);
            
            // Buscar carrera de la base de datos
            ec.edu.uteq.presustentaciones.entities.Carrera carrera = carreraRepository.findById(1).orElse(null);

            // 1. ADMIN
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

            // 2. COORDINACION
            if (!usuarioRepository.existsByEmail("lsanchez@uteq.edu.ec")) {
                Usuario coord = Usuario.builder()
                    .nombre("Laura Patricia")
                    .apellido("Sánchez Mora")
                    .email("lsanchez@uteq.edu.ec")
                    .password(passwordEncoder.encode("coordinacion123"))
                    .rol("COORDINADOR")
                    .rolUsuario(coordRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(coord);
                System.out.println("Usuario Coordinador sembrado con éxito.");
            }

            // 3. ESTUDIANTE 1
            if (!usuarioRepository.existsByEmail("jperez@uteq.edu.ec")) {
                Usuario est1User = Usuario.builder()
                    .nombre("Juan Carlos")
                    .apellido("Pérez López")
                    .email("jperez@uteq.edu.ec")
                    .password(passwordEncoder.encode("estudiante123"))
                    .rol("ESTUDIANTE")
                    .rolUsuario(estRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(est1User);

                Estudiante est1 = Estudiante.builder()
                    .usuario(est1User)
                    .carrera("Ingeniería en Software")
                    .semestre("8vo")
                    .telefono("0991234567")
                    .expedienteCodigo("SW-2024-001")
                    .carreraEntidad(carrera)
                    .semestreActual((short) 8)
                    .build();
                estudianteRepository.save(est1);
                System.out.println("Usuario Estudiante y perfil sembrados con éxito.");
            }

            // 4. TUTOR
            if (!usuarioRepository.existsByEmail("rmartinez@uteq.edu.ec")) {
                Usuario tutorUser = Usuario.builder()
                    .nombre("Roberto")
                    .apellido("Martínez Silva")
                    .email("rmartinez@uteq.edu.ec")
                    .password(passwordEncoder.encode("tutor123"))
                    .rol("DOCENTE")
                    .rolUsuario(docenteRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(tutorUser);

                Docente tutor = Docente.builder()
                    .usuario(tutorUser)
                    .areaEspecialidad("Ingeniería de Software")
                    .cargaHorariaSemanal(20)
                    .disponible(true)
                    .build();
                docenteRepository.save(tutor);
                System.out.println("Usuario Tutor y perfil sembrados con éxito.");
            }

            // 5. PRESIDENTE
            if (!usuarioRepository.existsByEmail("arodriguez@uteq.edu.ec")) {
                Usuario presUser = Usuario.builder()
                    .nombre("Ana María")
                    .apellido("Rodríguez Vega")
                    .email("arodriguez@uteq.edu.ec")
                    .password(passwordEncoder.encode("presidente123"))
                    .rol("DOCENTE")
                    .rolUsuario(docenteRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(presUser);

                Docente pres = Docente.builder()
                    .usuario(presUser)
                    .areaEspecialidad("Gestión de Proyectos")
                    .cargaHorariaSemanal(25)
                    .disponible(true)
                    .build();
                docenteRepository.save(pres);
                System.out.println("Usuario Presidente y perfil sembrados con éxito.");
            }

            // 6. JURADO
            if (!usuarioRepository.existsByEmail("clopez@uteq.edu.ec")) {
                Usuario jurUser = Usuario.builder()
                    .nombre("Carlos Alberto")
                    .apellido("López Castro")
                    .email("clopez@uteq.edu.ec")
                    .password(passwordEncoder.encode("jurado123"))
                    .rol("DOCENTE")
                    .rolUsuario(docenteRol)
                    .activo(true)
                    .build();
                usuarioRepository.save(jurUser);

                Docente jur = Docente.builder()
                    .usuario(jurUser)
                    .areaEspecialidad("Base de Datos")
                    .cargaHorariaSemanal(18)
                    .disponible(true)
                    .build();
                docenteRepository.save(jur);
                System.out.println("Usuario Jurado y perfil sembrados con éxito.");
            }

        } catch (Exception e) {
            System.err.println("Error al sembrar datos de prueba: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("====================================================");
    }
}