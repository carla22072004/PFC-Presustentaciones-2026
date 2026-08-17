# 🛡️ AUDITORÍA DE SEGURIDAD OWASP TOP 10 — REVISIÓN MANUAL + HERRAMIENTAS AUTOMÁTICAS

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Metodología:** Revisión manual del código fuente (2026-08-11/12) contra los 6 controles listados abajo, **más 3 herramientas automáticas reales corridas el 2026-08-17** (Fase 5): OWASP ZAP baseline (escaneo dinámico contra la app corriendo), SpotBugs + find-sec-bugs (análisis estático, incluye la regla `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE` para SQL dinámico), y `npm audit` (componentes vulnerables del frontend). Evidencia completa en [`docs/seguridad/zap/`](zap/) y en el job `backend` de [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).
**Fecha:** 2026-08-11/12 (revisión manual), 2026-08-17 (herramientas automáticas).

---

## Resumen

La revisión manual encontró y corrigió **3 problemas reales de control de acceso** en la gestión de usuarios. El hallazgo abierto de la ronda anterior (secreto JWT hardcodeado) **se corrigió el 2026-08-17** (ver A02). Las herramientas automáticas corridas en Fase 5 confirman **0 hallazgos de SQL dinámico/inyección** (find-sec-bugs), identifican **41 dependencias vulnerables del frontend** (6 críticas) pendientes de actualizar, y el escaneo dinámico OWASP ZAP no encontró ningún `FAIL` (0 vulnerabilidades de alta confianza), con 7 `WARN` restantes tras corregir 4 en el momento (cabeceras de seguridad faltantes en las respuestas estáticas de nginx).

## A01:2021 — Broken Access Control

**Hallado (corregido):** `UsuarioController` no tenía ninguna anotación `@PreAuthorize` — cualquier usuario autenticado (de cualquier rol) podía listar/crear/editar/desactivar/eliminar usuarios vía `/api/usuarios/**`, incluyendo asignar el rol `ADMIN` a cualquier cuenta. Corregido: los endpoints de gestión ahora requieren `hasRole('ADMIN')`; `GET /{id}` y `PATCH /{id}/perfil` verifican que el `id` corresponda al usuario autenticado (resuelto desde el JWT, no desde el parámetro de la URL) salvo que quien pide sea ADMIN.

**Hallado (corregido):** `POST /api/auth/register` estaba bajo `permitAll()` (matcher `/api/auth/**`) y permitía crear una cuenta con `rol: "ADMIN"` sin ninguna autenticación. No lo usa ningún componente del frontend. Corregido: ahora requiere `hasRole('ADMIN')`.

**Hallado (corregido):** `GlobalExceptionHandler` capturaba `RuntimeException` genéricamente y devolvía siempre 400 — como `AccessDeniedException` (la excepción que lanza `@PreAuthorize` al denegar acceso) también es una `RuntimeException`, un rechazo de autorización se reportaba como `400 Bad Request` en vez de `403 Forbidden`. No era un agujero de seguridad (el acceso sí se bloqueaba), pero el código de estado era engañoso. Se agregó un `@ExceptionHandler(AccessDeniedException.class)` específico que devuelve 403.

**Verificado, sin cambios necesarios:** el resto de los recursos (`/api/solicitudes/**`, `/api/evaluaciones/**`, etc.) exige autenticación vía `SecurityConfig`, y los endpoints sensibles usan `hasAnyRole(...)` a nivel de método. Los controladores que actúan "en nombre del usuario actual" (p. ej. `SolicitudController.crearPorUsuario`) ya resuelven el usuario desde el JWT en vez de confiar en un ID recibido del cliente — ese patrón es el que se replicó al corregir `UsuarioController`.

## A02:2021 — Cryptographic Failures

**Hallado (corregido):** `UsuarioServiceImpl.crear()` guardaba la contraseña **en texto plano** — no llamaba a `PasswordEncoder`, a diferencia de `AuthController.register()` que sí lo hacía. Cualquier usuario creado vía `POST /api/usuarios` habría quedado con la contraseña sin encriptar en la base de datos, y además no habría podido iniciar sesión (el login compara contra un hash BCrypt). Corregido: se inyectó `PasswordEncoder` y se encripta antes de guardar.

**Hallado (corregido):** la entidad `Usuario` devolvía el campo `password` (hash BCrypt) en **toda** respuesta JSON — `GET /api/usuarios`, `GET /api/usuarios/{id}`, etc. — porque los controladores serializan la entidad JPA directamente. Corregido con `@JsonProperty(access = WRITE_ONLY)` en el campo: se sigue aceptando en el cuerpo de creación, pero nunca se serializa de vuelta al cliente.

**Hallado (corregido 2026-08-17):** el secreto JWT (`jwt.secret`) estaba hardcodeado en texto plano en `application.properties` y repetido como valor por defecto en `docker-compose.yml` — visible en el repositorio público. Corregido: `application.properties` ahora exige `${JWT_SECRET}` sin valor por defecto (la app no arranca sin la variable de entorno); `docker-compose.yml` usa la sintaxis `${JWT_SECRET:?mensaje}` para fallar explícitamente si falta; se agregó `.env`/`backend/.env` a `.gitignore` (no estaba). Los tests usan un secreto dummy propio en `backend/src/test/resources/application.properties`, aislado del secreto real. BCrypt factor 10, firma HS512, ahora con 7 claims RFC 7519 (`jti`, `iss`, `sub`, `aud`, `iat`, `nbf`, `exp`) y refresh token con rotación en Redis — ver `JwtTokenProvider`.

## A03:2021 — Injection

**Verificado, sin hallazgos:** todo el acceso a datos pasa por Spring Data JPA (parámetros ligados) o por los procedimientos almacenados PL/pgSQL parametrizados de `V2__stored_procedures.sql`. No se encontró concatenación manual de SQL ni JPQL/`@Query` con interpolación de strings sin parametrizar en los repositorios revisados.

**Confirmado con herramienta automática (2026-08-17):** SpotBugs + find-sec-bugs, filtrado a la categoría `SECURITY` (incluye la regla `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`), corrido con `./mvnw com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs` sobre todo el código fuente compilado — **0 hallazgos de SQL dinámico/inyección**, confirmando la revisión manual. El mismo análisis sí encontró 189 hallazgos reales en otras categorías de seguridad (ver sección "Análisis estático SpotBugs / find-sec-bugs" más abajo).

## A04:2021 — Insecure Design

**Hallado (sin corregir, riesgo bajo):** la mayoría de los controladores devuelve entidades JPA directamente en vez de DTOs (documentado también en `CLAUDE.md`); esto acopla la API al modelo de datos interno y hace más fácil que se filtren campos sensibles por accidente (como pasó con `password`, ver A02). Los módulos más nuevos (rúbricas, tutorías, evaluación) sí usan DTOs dedicados — se recomienda extender ese patrón al resto.

## A05:2021 — Security Misconfiguration

**Verificado:** CORS restringido explícitamente a `http://localhost:4200` y `http://localhost:3000` (`SecurityConfig` + `WebConfig`, más `@CrossOrigin` por controlador) — configurado en 3 lugares distintos que hay que mantener sincronizados si se agrega un origen nuevo (riesgo de mantenimiento, no de seguridad activa). CSRF deshabilitado deliberadamente (correcto para una API JWT stateless sin cookies de sesión). Sesión configurada como `STATELESS`.

## A06:2021 — Vulnerable and Outdated Components

**Confirmado con herramienta automática (2026-08-17):** `npm audit` sobre `Frontend/package-lock.json` real (no simulado) reporta **41 vulnerabilidades: 6 críticas, 25 altas, 7 moderadas, 3 bajas**. Las 3 críticas identificadas por nombre: `form-data <=2.5.5`, `minimist <=0.2.3`, `tar <=7.5.20` — todas dependencias transitivas de herramientas de build/test, no del código de aplicación en sí, pero igual de reales y explotables si algún flujo del pipeline de build queda expuesto. **No corregido en esta ronda** (requiere `npm audit fix` y probar que el build de producción y los tests siguen pasando tras el bump de versiones — trabajo no trivial dado el volumen, se deja para una iteración dedicada). El backend no se auditó con `mvn dependency-check:check` porque requiere descargar la base de datos NVD completa (varios GB, puede tardar más de una hora en la primera corrida) — queda pendiente para una corrida con tiempo dedicado, idealmente como job de CI con caché persistente de la base NVD.

---

## Matriz consolidada

| Control OWASP | Estado | Hallazgos | Acción |
|---|---|---|---|
| A01: Broken Access Control | 🟢 Corregido | 2 reales (usuarios sin `@PreAuthorize`, registro público) | `@PreAuthorize('ADMIN')` + validación de propiedad |
| A02: Cryptographic Failures | 🟢 Corregido | 3 corregidos (password en texto plano, filtrado en JSON, secreto JWT hardcodeado) | Ver detalle arriba |
| A03: Injection | 🟢 Sin hallazgos (manual + find-sec-bugs) | 0 | — |
| A04: Insecure Design | 🟡 Riesgo bajo, sin corregir | 1 (entidades expuestas directamente) | Recomendado para próxima entrega |
| A05: Security Misconfig | 🟡 Corregido parcialmente | 4 corregidos por ZAP (headers nginx), 7 abiertos de menor severidad | Ver sección OWASP ZAP |
| A06: Vulnerable Components | 🔴 Verificado, sin corregir | 41 (6 críticas) en frontend vía `npm audit`; backend no auditado (NVD pendiente) | `npm audit fix` + `dependency-check` en próxima iteración |

## Análisis estático SpotBugs / find-sec-bugs (2026-08-17)

Corrido con `./mvnw com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs`, filtrado a la categoría `SECURITY` ([`backend/spotbugs-security-include.xml`](../../backend/spotbugs-security-include.xml)), integrado como paso no bloqueante en `.github/workflows/ci.yml` (se publica el reporte XML como artefacto en cada push). **189 hallazgos reales**, ninguno de tipo `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`:

| Regla | Cantidad | Severidad | Estado |
|---|---|---|---|
| `SPRING_ENDPOINT` | 121 | Informativa | Sin acción (marca cada endpoint REST, no es una vulnerabilidad) |
| `CRLF_INJECTION_LOGS` | 44 | Baja/Media | Abierto — logs con `Logger.info(fmt, objetoUsuario)` podrían permitir forjar entradas de log si el objeto contiene `\r\n`; mitigación: usar un `encoder` de logging que escape saltos de línea |
| `IMPROPER_UNICODE` | 13 | Baja | Abierto — comparaciones/transformaciones de String sin especificar `Locale` |
| `PATH_TRAVERSAL_IN` | 9 | Media | Abierto — `Paths.get()` en `ActaServiceImpl`, `AnteproyectoServiceImpl`, `TutoriaServiceImpl` construye rutas de archivo a partir de datos que en última instancia vienen de la base de datos (IDs `Long`, no strings de usuario libres) — riesgo real bajo pero sin validación explícita de que la ruta resultante permanezca dentro del directorio esperado |
| `UNSAFE_HASH_EQUALS` | 1 | Media | **Corregido en esta ronda** — `AnteproyectoServiceImpl.verificarIntegridad()` comparaba hashes SHA-256 con `String.equals()` (vulnerable a timing attack); se cambió a `MessageDigest.isEqual()` |
| `SPRING_CSRF_PROTECTION_DISABLED` | 1 | Informativa | Sin acción — deshabilitado deliberadamente por ser API JWT stateless (ver A05) |

## Escaneo dinámico OWASP ZAP (2026-08-17)

Corrido con `zap-baseline.py` (imagen oficial `zaproxy/zap-stable`) contra la app real corriendo con la topología de producción (nginx sirviendo el build de Angular + proxy inverso a `/api/v1/` y `/actuator/`, backend Spring Boot, Postgres y Redis). Reportes completos: [`docs/seguridad/zap/zap-baseline-report.html`](zap/zap-baseline-report.html) y [`zap-baseline-report.json`](zap/zap-baseline-report.json).

**Antes de corregir:** `FAIL-NEW: 0` · `WARN-NEW: 11` · `PASS: 56`
**Después de corregir 4 hallazgos:** `FAIL-NEW: 0` · `WARN-NEW: 7` · `PASS: 60`

**0 alertas de alta confianza (`FAIL`) en ambas corridas** — ningún hallazgo crítico tipo XSS reflejado, inyección SQL detectable dinámicamente, o cookie insegura. Los 4 hallazgos corregidos en el momento (real, verificado con `curl -I` antes/después):

- `10020` Missing Anti-clickjacking Header, `10021` X-Content-Type-Options Missing, `10038` CSP Header Not Set, `10036` Server Version Disclosure — las 4 causadas por el mismo hueco: **nginx sirve el HTML/JS/CSS del frontend directamente sin pasar por el backend**, así que las cabeceras de seguridad que Spring Security agrega (Requisito E14) nunca llegaban a esas respuestas. Se agregaron las mismas cabeceras (`X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`, `Permissions-Policy`, `server_tokens off`) directamente en [`nginx/nginx.conf`](../../nginx/nginx.conf).

Los 7 `WARN` restantes son de severidad menor y quedan documentados para una próxima iteración: `10003` Vulnerable JS Library (relacionado con A06), `10049` Storable-but-Non-Cacheable en assets estáticos, `10055` CSP sin `object-src`/`base-uri` explícitos, `10109`/`10110` informativos (SPA moderna, uso de `eval` requerido por el compilador JIT de Angular), `90003` Subresource Integrity ausente en los `<script>`, `90004` Cross-Origin-Embedder-Policy ausente.

**Nota de proceso:** al agregar las cabeceras de seguridad a `nginx.conf` se detectó y corrigió un efecto secundario real: la Content-Security-Policy heredada de `SecurityConfig.java` no incluía `cdn.jsdelivr.net` (de donde carga `bootstrap-icons`), lo que habría roto los íconos del frontend en producción; se agregó el dominio a `style-src`/`font-src` en ambos lugares (backend y nginx) tras verificarlo con un login real de punta a punta contra la topología de producción completa (nginx + backend + Postgres + Redis, todos en Docker).
