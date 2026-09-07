package ec.edu.uteq.presustentaciones.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import ec.edu.uteq.presustentaciones.dto.BackupInfoDTO;

/**
 * Genera y administra respaldos completos de la base de datos para el apartado
 * "Gestión de Respaldos" del administrador (permiso {@code BACKUPS_GESTIONAR}).
 *
 * <p>Un respaldo es un dump en formato custom comprimido ({@code pg_dump -Fc}), el mismo
 * formato que {@code database/presusDb_full_1M.dump} y que documenta
 * {@code database/README.md} -- se restaura con {@code pg_restore}, nunca con
 * {@code psql < archivo}. Los binarios {@code pg_dump}/{@code pg_restore} vienen en la
 * imagen del backend (ver {@code backend/Dockerfile}: {@code apk add postgresql-client}).
 *
 * <p>Los archivos viven en {@code app.backups.dir} (por defecto {@code uploads/backups}),
 * que en Docker es un volumen persistente montado en {@code /app/uploads}
 * (ver {@code docker-compose.yml}). El nombre de archivo es lo único que los endpoints
 * aceptan del cliente; se valida contra un patrón estricto para descartar path traversal.
 */
@Service
@Slf4j
public class BackupService {

    /** Solo se aceptan nombres de archivo con este formato para descargar/restaurar/eliminar. */
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9._-]+\\.dump$");
    private static final Pattern JDBC_URL =
            Pattern.compile("^jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?;]+).*$");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Un dump/restauración de la base completa no debería tardar más que esto. */
    private static final long TIMEOUT_MINUTOS = 30;

    @Value("${app.backups.dir:uploads/backups}")
    private String backupsDir;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    // ── Consultas ────────────────────────────────────────────────────────────

    /** Respaldos existentes, del más reciente al más antiguo. */
    public List<BackupInfoDTO> listar() {
        Path dir = Paths.get(backupsDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(dir)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".dump"))
                    .map(this::aInfo)
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(BackupInfoDTO::getFechaCreacion).reversed())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo listar el directorio de respaldos: " + e.getMessage(), e);
        }
    }

    // ── Operaciones ──────────────────────────────────────────────────────────

    /** Genera un respaldo nuevo con {@code pg_dump -Fc} y devuelve sus metadatos. */
    public BackupInfoDTO generar() {
        Conexion c = parsearConexion();
        Path dir = crearDirectorio();
        String nombre = "respaldo_" + LocalDateTime.now().format(SELLO) + ".dump";
        Path destino = dir.resolve(nombre);

        List<String> comando = List.of(
                "pg_dump",
                "-h", c.host, "-p", c.port, "-U", c.usuario, "-d", c.baseDatos,
                "--format=custom", "--compress=6", "--no-owner", "--no-privileges",
                "-f", destino.toAbsolutePath().toString());

        ejecutar(comando, "pg_dump", "generar el respaldo");

        if (!Files.isRegularFile(destino) || tamano(destino) == 0L) {
            throw new RuntimeException("pg_dump terminó sin error pero el archivo de respaldo quedó vacío.");
        }
        log.info("Respaldo generado: {} ({} bytes) por {}", nombre, tamano(destino), usuarioActual());
        return aInfo(destino);
    }

    /** Bytes de un respaldo existente, para descargarlo. */
    public byte[] leer(String nombre) {
        Path archivo = resolverExistente(nombre);
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el respaldo: " + e.getMessage(), e);
        }
    }

    /**
     * Restaura la base a partir de un respaldo existente ({@code pg_restore --clean --if-exists}).
     * Operación destructiva: reemplaza el contenido actual de la base por el del dump.
     */
    public void restaurar(String nombre) {
        Path archivo = resolverExistente(nombre);
        Conexion c = parsearConexion();

        List<String> comando = List.of(
                "pg_restore",
                "-h", c.host, "-p", c.port, "-U", c.usuario, "-d", c.baseDatos,
                "--clean", "--if-exists", "--no-owner", "--no-privileges",
                archivo.toAbsolutePath().toString());

        // pg_restore devuelve código != 0 por avisos no fatales (p. ej. "no existe" al
        // hacer DROP ... IF EXISTS sobre una base recién creada). Se toleran esos avisos
        // y solo se falla si el propio archivo o la conexión son inválidos.
        int codigo = ejecutarTolerante(comando, "pg_restore");
        log.warn("Restauración de base ejecutada desde {} por {} (código pg_restore={})",
                nombre, usuarioActual(), codigo);
    }

    /** Elimina permanentemente un archivo de respaldo. */
    public void eliminar(String nombre) {
        Path archivo = resolverExistente(nombre);
        try {
            Files.delete(archivo);
            log.info("Respaldo eliminado: {} por {}", nombre, usuarioActual());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo eliminar el respaldo: " + e.getMessage(), e);
        }
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private record Conexion(String host, String port, String baseDatos, String usuario) {}

    private Conexion parsearConexion() {
        Matcher m = JDBC_URL.matcher(datasourceUrl == null ? "" : datasourceUrl.trim());
        if (!m.matches()) {
            throw new IllegalStateException(
                    "No se pudo interpretar la URL de la base de datos para generar el respaldo.");
        }
        String host = m.group(1);
        String port = m.group(2) != null ? m.group(2) : "5432";
        String baseDatos = m.group(3);
        return new Conexion(host, port, baseDatos, dbUsername);
    }

    private Path crearDirectorio() {
        try {
            Path dir = Paths.get(backupsDir);
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de respaldos: " + e.getMessage(), e);
        }
    }

    /** Valida el nombre, lo resuelve dentro del directorio de respaldos y exige que exista. */
    private Path resolverExistente(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches() || nombre.contains("..")) {
            throw new IllegalArgumentException("Nombre de respaldo inválido.");
        }
        Path dir = Paths.get(backupsDir).toAbsolutePath().normalize();
        Path archivo = dir.resolve(nombre).normalize();
        if (!archivo.getParent().equals(dir)) {
            throw new IllegalArgumentException("Nombre de respaldo inválido.");
        }
        if (!Files.isRegularFile(archivo)) {
            throw new IllegalArgumentException("El respaldo '" + nombre + "' no existe.");
        }
        return archivo;
    }

    private void ejecutar(List<String> comando, String binario, String descripcion) {
        Proceso r = correr(comando, binario);
        if (r.codigo != 0) {
            log.error("{} falló (código {}): {}", binario, r.codigo, r.salida);
            throw new RuntimeException("No se pudo " + descripcion + ": " + resumirError(r.salida));
        }
    }

    private int ejecutarTolerante(List<String> comando, String binario) {
        Proceso r = correr(comando, binario);
        if (!r.salida.isBlank()) {
            log.warn("{} avisos: {}", binario, r.salida);
        }
        return r.codigo;
    }

    private record Proceso(int codigo, String salida) {}

    private Proceso correr(List<String> comando, String binario) {
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("PGPASSWORD", dbPassword == null ? "" : dbPassword);
        pb.redirectErrorStream(true);
        Process proceso;
        try {
            proceso = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "El comando '" + binario + "' no está disponible en el servidor. "
                    + "Revisa que la imagen del backend incluya postgresql-client.");
        }
        String salida;
        try {
            salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!proceso.waitFor(TIMEOUT_MINUTOS, TimeUnit.MINUTES)) {
                proceso.destroyForcibly();
                throw new RuntimeException(
                        "El respaldo excedió el tiempo máximo de " + TIMEOUT_MINUTOS + " minutos.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo la salida de " + binario + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La operación de respaldo fue interrumpida.");
        }
        return new Proceso(proceso.exitValue(), salida.trim());
    }

    private static String resumirError(String salida) {
        if (salida == null || salida.isBlank()) return "sin detalle (revisa los logs del backend).";
        String[] lineas = salida.strip().split("\\r?\\n");
        String ultima = lineas[lineas.length - 1].trim();
        return ultima.length() > 300 ? ultima.substring(0, 300) + "…" : ultima;
    }

    private BackupInfoDTO aInfo(Path p) {
        try {
            long bytes = Files.size(p);
            LocalDateTime creado = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(p).toInstant(), java.time.ZoneId.systemDefault());
            return BackupInfoDTO.builder()
                    .nombre(p.getFileName().toString())
                    .tamanoBytes(bytes)
                    .tamanoLegible(formatoTamano(bytes))
                    .fechaCreacion(creado)
                    .build();
        } catch (IOException e) {
            log.warn("No se pudo leer metadatos de {}: {}", p, e.getMessage());
            return null;
        }
    }

    private long tamano(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String formatoTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    private static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "desconocido";
    }
}
