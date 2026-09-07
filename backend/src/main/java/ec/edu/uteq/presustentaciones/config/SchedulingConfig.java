package ec.edu.uteq.presustentaciones.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas de Spring (usado por BackupScheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
