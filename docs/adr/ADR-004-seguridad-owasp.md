# ADR-004: Cumplimiento de Controles de Seguridad OWASP Top 10

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
dinámicos consultados contra la base de datos — ver [`ADR-005-permisos-dinamicos.md`](ADR-005-permisos-dinamicos.md)
para el detalle completo y el porqué del cambio. No se reescribe este control retroactivamente para
conservar el registro histórico de la decisión tal como se tomó.

## Actualización (2026-09-05): endurecimiento de la CSP y su costo

El control #5 se implementó originalmente con una `Content-Security-Policy` que incluía
`script-src 'self' 'unsafe-inline' 'unsafe-eval'`. Esa combinación anula buena parte de la protección
contra XSS que la cabecera debería dar (ZAP la levanta como alerta `10055`), así que se retiraron ambos
tokens en las dos capas que emiten la cabecera (`SecurityConfig.java` y `nginx/nginx.conf`), junto con
los orígenes `localhost`, los `ws://` (no hay WebSocket: el estado en tiempo real es *polling*) y
`universities.hipolabs.com` de `connect-src` (ese lo consume el backend server-side, no el navegador).

**Alternativa descartada y por qué.** Al probar la app en el navegador con la política nueva, la
interfaz quedó **sin estilos**: el build de producción de Angular aplica la optimización
`inlineCritical`, que difiere la hoja de estilos con
`<link rel="stylesheet" media="print" onload="this.media='all'">`. Ese `onload` es un manejador inline
y la nueva política lo bloquea, así que el `media` nunca pasaba de `print` a `all` (el `<noscript>` de
respaldo sólo actúa con JavaScript deshabilitado). Había dos salidas:

1. Reponer `'unsafe-inline'` (o añadir `'unsafe-hashes'` con el hash del manejador) — descartada:
   volvería a debilitar justamente el control que se está corrigiendo, por una optimización de carga.
2. Desactivar `inlineCritical` en la configuración de producción de `angular.json` — **elegida**. La
   hoja de estilos vuelve a cargarse de forma normal (bloqueante) y no se necesita ningún script inline.

**Costo asumido y cómo verificarlo.** Perder la carga diferida de CSS puede empeorar ligeramente el
First Contentful Paint, que ya era el cuello de botella de la métrica Performance de Lighthouse
(65 escritorio / 61 móvil). Se acepta ese costo por tratarse de un control de seguridad frente a una
optimización de percepción de carga, y queda pendiente volver a correr Lighthouse tras este cambio para
medir el impacto real en vez de suponerlo.
