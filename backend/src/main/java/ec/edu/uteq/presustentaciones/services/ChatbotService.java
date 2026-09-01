package ec.edu.uteq.presustentaciones.services;

import ec.edu.uteq.presustentaciones.dto.ChatRequest;
import ec.edu.uteq.presustentaciones.dto.ChatResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ChatbotService {

    public ChatResponse processMessage(ChatRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = extractPrimaryRole(auth);
        String msg = request.getMessage() != null ? request.getMessage().toLowerCase() : "";

        return switch (role) {
            case "ESTUDIANTE" -> handleEstudiante(msg);
            case "DOCENTE", "TUTOR" -> handleDocente(msg);
            case "COORDINADOR" -> handleCoordinador(msg);
            case "ADMINISTRADOR" -> handleAdministrador(msg);
            case "JURADO" -> handleJurado(msg);
            default -> unknownRoleResponse();
        };
    }

    private String extractPrimaryRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return "GUEST";
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority().replace("ROLE_", "").toUpperCase();
            if (List.of("ESTUDIANTE", "DOCENTE", "TUTOR", "COORDINADOR", "ADMINISTRADOR", "JURADO").contains(role)) {
                return role;
            }
        }
        return "GUEST";
    }

    private ChatResponse handleEstudiante(String msg) {
        if (msg.contains("solicitud") && (msg.contains("crear") || msg.contains("hacer"))) {
            return ChatResponse.builder()
                    .response("Para crear una solicitud, ve a la sección de 'Registrar Solicitud'. Necesitarás la información de tu anteproyecto.")
                    .options(Arrays.asList("Registrar solicitud"))
                    .route("/solicitudes/registrar")
                    .build();
        } else if (msg.contains("mis solicitudes") || msg.contains("ver mi solicitud")) {
            return ChatResponse.builder()
                    .response("Puedes revisar tus solicitudes desde el módulo de Solicitudes. Allí podrás consultar el estado y la información registrada.")
                    .options(Arrays.asList("Ver mis solicitudes"))
                    .route("/solicitudes/listar")
                    .build();
        } else if (msg.contains("pendiente")) {
            return ChatResponse.builder()
                    .response("Si tu solicitud está pendiente, significa que está esperando la revisión del Coordinador o la asignación de tribunal.")
                    .options(Arrays.asList("Ver mis solicitudes"))
                    .route("/solicitudes/listar")
                    .build();
        } else if (msg.contains("observaciones")) {
            return ChatResponse.builder()
                    .response("Puedes ver las observaciones de tus solicitudes en la sección 'Ver Observaciones'.")
                    .options(Arrays.asList("Ver observaciones"))
                    .route("/solicitudes/observaciones")
                    .build();
        } else if (msg.contains("sustentacion") || msg.contains("cuando")) {
            return ChatResponse.builder()
                    .response("Las fechas de sustentación se definen en el Cronograma. Revisa tu Horario para más detalles.")
                    .options(Arrays.asList("Ver mi horario"))
                    .route("/horario")
                    .build();
        } else if (msg.isEmpty() || msg.contains("hola") || msg.contains("ayuda")) {
            return ChatResponse.builder()
                    .response("Hola 👋 Soy tu asistente del Sistema de Pre-Sustentaciones UTEQ. Puedo ayudarte con solicitudes, anteproyectos, observaciones y sustentación.")
                    .options(Arrays.asList("¿Dónde veo mis solicitudes?", "¿Cómo creo una solicitud?"))
                    .build();
        }
        
        return unknownQueryResponse("ESTUDIANTE");
    }

    private ChatResponse handleDocente(String msg) {
        if (msg.contains("estudiante") && msg.contains("mis")) {
            return ChatResponse.builder()
                    .response("Puedes ver el listado de tus estudiantes asignados en la sección 'Mis Estudiantes'.")
                    .options(Arrays.asList("Ver mis estudiantes"))
                    .route("/docente/mis-estudiantes")
                    .build();
        } else if (msg.contains("revisar") || msg.contains("solicitud")) {
            return ChatResponse.builder()
                    .response("Puedes revisar los anteproyectos de tus estudiantes desde la sección correspondiente.")
                    .options(Arrays.asList("Ver anteproyectos"))
                    .route("/docente/ver-anteproyecto")
                    .build();
        } else if (msg.contains("acta")) {
            return ChatResponse.builder()
                    .response("Puedes firmar las actas pendientes en la sección 'Firmar Acta'.")
                    .options(Arrays.asList("Firmar acta"))
                    .route("/docente/firmar-acta")
                    .build();
        } else if (msg.contains("tutoria") || msg.contains("tutoría")) {
            return ChatResponse.builder()
                    .response("Gestiona las tutorías con tus estudiantes en la sección 'Mis Tutorías'.")
                    .options(Arrays.asList("Mis tutorías"))
                    .route("/tutorias")
                    .build();
        } else if (msg.isEmpty() || msg.contains("hola") || msg.contains("ayuda")) {
            return ChatResponse.builder()
                    .response("Hola 👋 Soy tu asistente. Puedo ayudarte con estudiantes, tutorías, anteproyectos, actas y evaluaciones.")
                    .options(Arrays.asList("¿Dónde veo mis estudiantes?", "¿Cómo gestiono una tutoría?"))
                    .build();
        }
        return unknownQueryResponse("DOCENTE");
    }

    private ChatResponse handleCoordinador(String msg) {
        if (msg.contains("revisar") && msg.contains("solicitud")) {
            return ChatResponse.builder()
                    .response("Como coordinador puedes revisar las solicitudes desde el módulo de Revisar Solicitudes.")
                    .options(Arrays.asList("Revisar solicitudes"))
                    .route("/admin/revisar-solicitudes")
                    .build();
        } else if (msg.contains("asignar jurado")) {
            return ChatResponse.builder()
                    .response("La asignación de jurados se realiza en el módulo 'Asignar Jurados'.")
                    .options(Arrays.asList("Asignar jurados"))
                    .route("/admin/asignar-jurados")
                    .build();
        } else if (msg.contains("tribunal")) {
            return ChatResponse.builder()
                    .response("La conformación de tribunales se realiza en 'Asignar Tribunal'.")
                    .options(Arrays.asList("Asignar tribunal"))
                    .route("/admin/asignar-tribunal")
                    .build();
        } else if (msg.contains("cronograma")) {
            return ChatResponse.builder()
                    .response("Puedes gestionar el cronograma de sustentaciones en 'Programar Cronograma'.")
                    .options(Arrays.asList("Programar cronograma"))
                    .route("/admin/cronograma")
                    .build();
        } else if (msg.isEmpty() || msg.contains("hola") || msg.contains("ayuda")) {
            return ChatResponse.builder()
                    .response("Hola 👋 Soy tu asistente. Puedo ayudarte con solicitudes, jurados, tribunales, cronograma y seguimiento.")
                    .options(Arrays.asList("¿Cómo reviso las solicitudes?", "¿Cómo asigno un tutor?"))
                    .build();
        }
        return unknownQueryResponse("COORDINADOR");
    }

    private ChatResponse handleAdministrador(String msg) {
        if (msg.contains("usuario")) {
            return ChatResponse.builder()
                    .response("La gestión de usuarios se encuentra en el módulo 'Gestión de Usuarios'.")
                    .options(Arrays.asList("Gestionar usuarios"))
                    .route("/admin/gestion-usuarios")
                    .build();
        } else if (msg.contains("rol") || msg.contains("roles")) {
            return ChatResponse.builder()
                    .response("Puedes administrar los roles del sistema en 'Gestionar Roles'.")
                    .options(Arrays.asList("Gestionar roles"))
                    .route("/admin/gestionar-roles")
                    .build();
        } else if (msg.contains("permiso")) {
            return ChatResponse.builder()
                    .response("La asignación de permisos se maneja en 'Gestionar Permisos'.")
                    .options(Arrays.asList("Gestionar permisos"))
                    .route("/admin/gestionar-permisos")
                    .build();
        } else if (msg.contains("carrera")) {
            return ChatResponse.builder()
                    .response("Las carreras se configuran en 'Gestionar Carreras'.")
                    .options(Arrays.asList("Gestionar carreras"))
                    .route("/admin/gestionar-carreras")
                    .build();
        } else if (msg.isEmpty() || msg.contains("hola") || msg.contains("ayuda")) {
            return ChatResponse.builder()
                    .response("Hola 👋 Soy tu asistente. Puedo ayudarte con usuarios, roles, permisos, carreras, reportes y administración.")
                    .options(Arrays.asList("¿Cómo creo un usuario?", "¿Cómo gestiono roles?"))
                    .build();
        }
        return unknownQueryResponse("ADMINISTRADOR");
    }

    private ChatResponse handleJurado(String msg) {
        if (msg.contains("evaluacion") || msg.contains("evaluación")) {
            return ChatResponse.builder()
                    .response("Tus evaluaciones pendientes están en 'Mis Asignaciones' o en 'Evaluar Rúbrica'.")
                    .options(Arrays.asList("Mis asignaciones"))
                    .route("/jurado/mis-asignaciones")
                    .build();
        } else if (msg.contains("rubrica") || msg.contains("rúbrica")) {
            return ChatResponse.builder()
                    .response("Puedes evaluar usando la rúbrica designada en el módulo correspondiente.")
                    .options(Arrays.asList("Evaluar rúbrica"))
                    .route("/jurado/evaluar-rubrica")
                    .build();
        } else if (msg.isEmpty() || msg.contains("hola") || msg.contains("ayuda")) {
            return ChatResponse.builder()
                    .response("Hola 👋 Soy tu asistente. Puedo ayudarte con evaluaciones, rúbricas, tribunales y actas.")
                    .options(Arrays.asList("¿Dónde realizo una evaluación?", "¿Cómo reviso la rúbrica?"))
                    .build();
        }
        return unknownQueryResponse("JURADO");
    }

    private ChatResponse unknownQueryResponse(String role) {
        List<String> options = switch (role) {
            case "ESTUDIANTE" -> Arrays.asList("¿Dónde veo mis solicitudes?", "¿Cómo creo una solicitud?");
            case "DOCENTE", "TUTOR" -> Arrays.asList("¿Dónde veo mis estudiantes?", "¿Cómo gestiono una tutoría?");
            case "COORDINADOR" -> Arrays.asList("¿Cómo reviso las solicitudes?", "¿Cómo asigno jurados?");
            case "ADMINISTRADOR" -> Arrays.asList("¿Cómo creo un usuario?", "¿Cómo gestiono roles?");
            case "JURADO" -> Arrays.asList("¿Dónde realizo una evaluación?");
            default -> Arrays.asList();
        };

        return ChatResponse.builder()
                .response("No estoy seguro de haber entendido tu consulta. Puedo ayudarte con estas opciones:")
                .options(options)
                .build();
    }

    private ChatResponse unknownRoleResponse() {
        return ChatResponse.builder()
                .response("Lo siento, no he podido identificar tu rol en el sistema. Inicia sesión nuevamente.")
                .build();
    }
}
