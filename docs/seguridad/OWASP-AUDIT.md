# 🛡️ AUDITORÍA DE SEGURIDAD OWASP TOP 10 — REVISIÓN MANUAL DE CÓDIGO

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Metodología:** Revisión manual del código fuente (backend Spring Security + controladores REST) contra los 6 controles listados abajo. **No se ejecutaron herramientas automáticas** (OWASP ZAP, SpotBugs, Dependency-Check) — una versión anterior de este documento citaba salidas de esas herramientas que nunca se corrieron; se retiraron.
**Fecha:** 2026-08-11/12.

---

## Resumen

De los 6 controles revisados, esta ronda encontró y corrigió **3 problemas reales de control de acceso** en la gestión de usuarios, y deja **1 hallazgo abierto** (secreto JWT hardcodeado) pendiente de una decisión de gestión de secretos que excede el alcance de esta corrección.

## A01:2021 — Broken Access Control

**Hallado (corregido):** `UsuarioController` no tenía ninguna anotación `@PreAuthorize` — cualquier usuario autenticado (de cualquier rol) podía listar/crear/editar/desactivar/eliminar usuarios vía `/api/usuarios/**`, incluyendo asignar el rol `ADMIN` a cualquier cuenta. Corregido: los endpoints de gestión ahora requieren `hasRole('ADMIN')`; `GET /{id}` y `PATCH /{id}/perfil` verifican que el `id` corresponda al usuario autenticado (resuelto desde el JWT, no desde el parámetro de la URL) salvo que quien pide sea ADMIN.

**Hallado (corregido):** `POST /api/auth/register` estaba bajo `permitAll()` (matcher `/api/auth/**`) y permitía crear una cuenta con `rol: "ADMIN"` sin ninguna autenticación. No lo usa ningún componente del frontend. Corregido: ahora requiere `hasRole('ADMIN')`.

**Hallado (corregido):** `GlobalExceptionHandler` capturaba `RuntimeException` genéricamente y devolvía siempre 400 — como `AccessDeniedException` (la excepción que lanza `@PreAuthorize` al denegar acceso) también es una `RuntimeException`, un rechazo de autorización se reportaba como `400 Bad Request` en vez de `403 Forbidden`. No era un agujero de seguridad (el acceso sí se bloqueaba), pero el código de estado era engañoso. Se agregó un `@ExceptionHandler(AccessDeniedException.class)` específico que devuelve 403.

**Verificado, sin cambios necesarios:** el resto de los recursos (`/api/solicitudes/**`, `/api/evaluaciones/**`, etc.) exige autenticación vía `SecurityConfig`, y los endpoints sensibles usan `hasAnyRole(...)` a nivel de método. Los controladores que actúan "en nombre del usuario actual" (p. ej. `SolicitudController.crearPorUsuario`) ya resuelven el usuario desde el JWT en vez de confiar en un ID recibido del cliente — ese patrón es el que se replicó al corregir `UsuarioController`.

## A02:2021 — Cryptographic Failures

**Hallado (corregido):** `UsuarioServiceImpl.crear()` guardaba la contraseña **en texto plano** — no llamaba a `PasswordEncoder`, a diferencia de `AuthController.register()` que sí lo hacía. Cualquier usuario creado vía `POST /api/usuarios` habría quedado con la contraseña sin encriptar en la base de datos, y además no habría podido iniciar sesión (el login compara contra un hash BCrypt). Corregido: se inyectó `PasswordEncoder` y se encripta antes de guardar.

**Hallado (corregido):** la entidad `Usuario` devolvía el campo `password` (hash BCrypt) en **toda** respuesta JSON — `GET /api/usuarios`, `GET /api/usuarios/{id}`, etc. — porque los controladores serializan la entidad JPA directamente. Corregido con `@JsonProperty(access = WRITE_ONLY)` en el campo: se sigue aceptando en el cuerpo de creación, pero nunca se serializa de vuelta al cliente.

**Hallado (abierto, no corregido):** el secreto JWT (`jwt.secret`) está hardcodeado en texto plano en `application.properties`, en el repositorio. Correcto: BCrypt con factor 10, algoritmo de firma HS512. Pendiente: mover el secreto a una variable de entorno / gestor de secretos antes de cualquier despliegue real — no se cambió aquí porque invalidaría todas las sesiones activas y requiere decidir el mecanismo de configuración por entorno, fuera del alcance de esta corrección puntual.

## A03:2021 — Injection

**Verificado, sin hallazgos:** todo el acceso a datos pasa por Spring Data JPA (parámetros ligados) o por los procedimientos almacenados PL/pgSQL parametrizados de `V2__stored_procedures.sql`. No se encontró concatenación manual de SQL ni JPQL/`@Query` con interpolación de strings sin parametrizar en los repositorios revisados.

## A04:2021 — Insecure Design

**Hallado (sin corregir, riesgo bajo):** la mayoría de los controladores devuelve entidades JPA directamente en vez de DTOs (documentado también en `CLAUDE.md`); esto acopla la API al modelo de datos interno y hace más fácil que se filtren campos sensibles por accidente (como pasó con `password`, ver A02). Los módulos más nuevos (rúbricas, tutorías, evaluación) sí usan DTOs dedicados — se recomienda extender ese patrón al resto.

## A05:2021 — Security Misconfiguration

**Verificado:** CORS restringido explícitamente a `http://localhost:4200` y `http://localhost:3000` (`SecurityConfig` + `WebConfig`, más `@CrossOrigin` por controlador) — configurado en 3 lugares distintos que hay que mantener sincronizados si se agrega un origen nuevo (riesgo de mantenimiento, no de seguridad activa). CSRF deshabilitado deliberadamente (correcto para una API JWT stateless sin cookies de sesión). Sesión configurada como `STATELESS`.

## A06:2021 — Vulnerable and Outdated Components

**No verificado en esta ronda** — requiere correr `mvn dependency-check:check` o `npm audit` contra las versiones reales del `pom.xml`/`package.json`, que no se ejecutaron como parte de esta revisión manual. Pendiente para una próxima corrida real (con las herramientas efectivamente instaladas) en vez de asumir "0 vulnerabilidades" sin haberlas corrido.

---

## Matriz consolidada

| Control OWASP | Estado | Hallazgos | Acción |
|---|---|---|---|
| A01: Broken Access Control | 🟢 Corregido | 2 reales (usuarios sin `@PreAuthorize`, registro público) | `@PreAuthorize('ADMIN')` + validación de propiedad |
| A02: Cryptographic Failures | 🟡 Corregido parcialmente | 2 corregidos (password en texto plano, filtrado en JSON), 1 abierto (secreto JWT hardcodeado) | Ver detalle arriba |
| A03: Injection | 🟢 Sin hallazgos | 0 | — |
| A04: Insecure Design | 🟡 Riesgo bajo, sin corregir | 1 (entidades expuestas directamente) | Recomendado para próxima entrega |
| A05: Security Misconfig | 🟢 Sin hallazgos | 0 | — |
| A06: Vulnerable Components | ⚪ No verificado | — | Pendiente de correr la herramienta real |
