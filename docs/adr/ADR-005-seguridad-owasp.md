# ADR-005: Cumplimiento de Controles de Seguridad OWASP Top 10

**Estado:** Aceptado  
**Fecha:** 2026-07-25  
**Decisores:** Equipo de Seguridad y Calidad UTEQ  

## Contexto
El sistema procesa información académica oficial, notas de titulación y documentos firmados digitalmente, por lo que requiere mitigación rigurosa de vulnerabilidades web.

## Decisión
Adopción obligatoria de 6 controles de seguridad basados en OWASP Top 10:
1. Control de acceso RBAC estricto en backend (`@PreAuthorize`).
2. Encriptación BCrypt (factor 10) para contraseñas.
3. Parametrización total de SQL.
4. Sanitización de entradas REST con `@Valid` y `Bean Validation`.
5. Restricción CORS estricta y headers de seguridad HTTP.
6. Escaneo automático de dependencias vulnerables con `dependency-check`.

## Consecuencias
- **Positivas:** Reducción a cero de vulnerabilidades críticas; cumplimiento de auditorías académicas de software.
- **Negativas:** Reclama esfuerzo continuo de actualización de dependencias de terceros.

**Actualización (2026-08-29):** el control #1 ("RBAC estricto con `@PreAuthorize`") describe el
enfoque original con roles fijos en código. Ese enfoque fue reemplazado por un sistema de permisos
dinámicos consultados contra la base de datos — ver [`ADR-007-permisos-dinamicos.md`](ADR-007-permisos-dinamicos.md)
para el detalle completo y el porqué del cambio. No se reescribe este control retroactivamente para
conservar el registro histórico de la decisión tal como se tomó.
