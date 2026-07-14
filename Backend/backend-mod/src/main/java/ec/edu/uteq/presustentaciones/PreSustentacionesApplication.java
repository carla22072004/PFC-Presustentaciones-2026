package ec.edu.uteq.presustentaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

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
public class PreSustentacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PreSustentacionesApplication.class, args);
    }
}