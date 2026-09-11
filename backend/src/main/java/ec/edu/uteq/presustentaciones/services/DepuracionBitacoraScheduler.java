package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.entities.DepuracionBitacoraLog;
import ec.edu.uteq.presustentaciones.repositories.AuditoriaRepository;
import ec.edu.uteq.presustentaciones.repositories.DepuracionBitacoraLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RNF-19: depura automáticamente las entradas de {@code presus.auditoria} anteriores al
 * período de retención declarado en {@code docs/etica/RETENCION-DATOS.md} (2 años por
 * omisión). Sigue el mismo patrón que {@link BackupScheduler} (tarea programada, cron
 * configurable por propiedad, nunca un endpoint) -- deliberado, porque RNF-18 exige que
 * <b>ninguna operación de la API</b> pueda borrar la bitácora; una tarea de sistema sin
 * entrada de usuario no es eso: es la política de retención ya declarada, aplicándose sola.
 *
 * <p>Cada corrida deja traza verificable en {@code presus.depuracion_bitacora_log}: cuántas
 * entradas borró y hasta qué fecha, para que "automático" no signifique "invisible".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DepuracionBitacoraScheduler {

    private final AuditoriaRepository auditoriaRepository;
    private final DepuracionBitacoraLogRepository logRepository;

    @Value("${app.auditoria.retencion-dias:730}")
    private int retencionDias;

    /** Primer día de cada mes, 03:30 -- fuera de horario académico, después del respaldo diario. */
    @Scheduled(cron = "0 30 3 1 * *")
    @Transactional
    public void depurar() {
        LocalDateTime fechaCorte = LocalDateTime.now().minusDays(retencionDias);
        int eliminadas = auditoriaRepository.borrarAnterioresA(fechaCorte);
        logRepository.save(DepuracionBitacoraLog.builder()
                .fechaEjecucion(LocalDateTime.now())
                .entradasEliminadas(eliminadas)
                .fechaCorte(fechaCorte)
                .build());
        if (eliminadas > 0) {
            log.info("Depuración de bitácora (RNF-19): {} entradas anteriores a {} eliminadas.",
                    eliminadas, fechaCorte);
        }
    }
}
