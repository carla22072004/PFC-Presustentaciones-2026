# ADR-001: Arquitectura de Software N-Capas Orientada a Servicios RESTful

**Estado:** Aceptado  
**Fecha:** 2026-07-15  
**Decisores:** Equipo de Desarrollo Titulación UTEQ  

## Contexto
El proyecto requiere automatizar el flujo académico de pre-sustentaciones de la UTEQ, garantizando alta disponibilidad, mantenibilidad, desacoplamiento y escalabilidad.

## Decisión
Adoptar una arquitectura monolítica modular en capas (N-Tier Architecture) con separación estricta entre el Frontend SPA (Angular 17) y el Backend REST (Spring Boot 3.2.1).

## Consecuencias
- **Positivas:** Desacoplamiento total de la interfaz visual respecto a la lógica de negocio; facilidad de despliegue mediante contenedores Docker; mantenimiento simplificado.
- **Negativas:** Reclama configuración explícita de seguridad CORS y manejo de tokens para comunicación inter-servicios.
