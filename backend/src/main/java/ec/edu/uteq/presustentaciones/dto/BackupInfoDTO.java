package ec.edu.uteq.presustentaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Metadatos de un archivo de respaldo (dump) de la base de datos, para la tabla del
 * apartado "Gestión de Respaldos" del administrador. No expone la ruta absoluta en el
 * servidor -- solo el nombre del archivo, que es lo único que los endpoints aceptan de
 * vuelta para descargar / restaurar / eliminar.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfoDTO {

    /** Nombre del archivo, p. ej. {@code respaldo_20260907_013045.dump}. */
    private String nombre;

    /** Tamaño en bytes. */
    private long tamanoBytes;

    /** Tamaño ya formateado para mostrar ("12.8 MB"). */
    private String tamanoLegible;

    /** Fecha de creación del archivo (hora del servidor). */
    private LocalDateTime fechaCreacion;
}
