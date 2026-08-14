package ec.edu.uteq.presustentaciones.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class CustomWebMvcRegistrations implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
                if (info != null && (handlerType.isAnnotationPresent(RestController.class))) {
                    // Obtenemos los patrones de URL actuales
                    Set<String> patterns = info.getPatternValues();
                    
                    // Si no hay patrones, retornamos la info base
                    if (patterns.isEmpty()) {
                        return info;
                    }

                    // Modificamos dinámicamente cada patrón para asegurar el prefijo /api/v1
                    String[] newPatterns = patterns.stream()
                            .map(pattern -> {
                                if (pattern.startsWith("/api/v1")) {
                                    return pattern; // ya versionado
                                } else if (pattern.startsWith("/api")) {
                                    // Reemplaza "/api" con "/api/v1" al inicio
                                    return "/api/v1" + pattern.substring(4);
                                } else {
                                    // Antepone "/api/v1"
                                    return "/api/v1" + (pattern.startsWith("/") ? pattern : "/" + pattern);
                                }
                            })
                            .toArray(String[]::new);

                    // Reconstruimos la información de mapeo con los nuevos patrones
                    return info.mutate()
                            .paths(newPatterns)
                            .build();
                }
                return info;
            }
        };
    }
}
