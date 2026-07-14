package ec.edu.uteq.presustentaciones.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol; // ESTUDIANTE, DOCENTE, ADMIN

    @Column(nullable = false)
    private Boolean activo = true;

    @Column
    private String telefono;

    /** Correo donde llegan las notificaciones del sistema (puede ser distinto al de login) */
    @Column(name = "email_notificaciones")
    private String emailNotificaciones;
}