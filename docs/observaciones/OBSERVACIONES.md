# 📋 REGISTRO DE OBSERVACIONES Y RETROALIMENTACIÓN (ENTREGAS 1A Y 1B)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Asignatura:** Proyecto Integrador de Saberes / Titulación  
**Institución:** Universidad Técnica Estatal de Quevedo (UTEQ)  

---

## 📌 Resumen de Etiquetado y Versionado Git

- **Etiqueta Git previa:** `v0.7.0` (Asignada a la finalización de la Entrega 1B).
- **Etiqueta Git de corrección:** `v0.7.1` (Asignada en el commit que resuelve íntegramente todas las observaciones de retroalimentación).
- **Etiqueta Git versión final:** `v0.9.0-rc` (Release Candidate para la defensa y evaluación de la Entrega 3).

---

## 🛠️ Matriz de Resoluciones de Retroalimentación

| ID | Entrega | Criterio / Observación del Docente | Decisión Técnica Aplicada | Componente Afectado | Hash del Commit | Estado |
|---|---|---|---|---|---|---|
| **OBS-01** | 1A | Incompatibilidad de tipos de clave primaria UUID en entidades JPA al interactuar con PostgreSQL en consultas relacionales. | Migración integral de identificadores `UUID` a `Long` autoincrementales (`BIGSERIAL`) en las 13 entidades, repositorios, servicios y DTOs REST. | `backend/src/main/java/...` | `6e06438` | ✅ Resuelto |
| **OBS-02** | 1A | Ausencia de capa de seguridad JWT con Spring Security y falta de protección en endpoints críticos del backend. | Implementación completa de Spring Security 6 con `JwtTokenProvider`, `JwtAuthenticationFilter` y soporte para Cookies de seguridad HTTP-Only. | `backend/src/main/java/ec/edu/uteq/presustentaciones/security/` | `d7aeb1a` | ✅ Resuelto |
| **OBS-03** | 1B | Las consultas complejas (uniones de tablas, generación de reportes y promedio de notas) se ejecutaban en la capa de aplicación sin optimización de BD. | Se escribieron 4 procedimientos almacenados PostgreSQL (`sp_calcular_promedio_evaluacion`, `sp_generar_reporte_defensas`, `sp_asignar_jurado_masivo`, `sp_firmar_acta_digital`), técnicamente correctos, pero **ninguno está invocado desde código Java** (verificado: cero referencias en `backend/src/main/java`) y **Flyway está desactivado** (`spring.flyway.enabled=false`), por lo que el archivo de migración que los contiene nunca se ejecuta automáticamente contra la base de datos. Pendiente: conectar cada SP vía `@Procedure`/`@NamedStoredProcedureQuery` desde la capa de repositorio/servicio y activar Flyway. | `backend/src/main/resources/db/migration/V2__stored_procedures.sql` | `a3b89f1` | 🟡 Parcial |
| **OBS-04** | 1B | Falta de documentación interactiva OpenAPI 3.0 (Swagger UI) para pruebas de la API REST por parte del jurado y frontend. | Integración de `springdoc-openapi-starter-webmvc-ui` y configuración de esquema de autenticación Bearer JWT en `OpenApiConfig.java`. | `backend/src/main/java/.../config/OpenApiConfig.java` | `b7c12d4` | ✅ Resuelto |
| **OBS-05** | 1A | El despliegue requería múltiples pasos manuales e instalaciones dependientes del sistema operativo. | Creación de `Makefile` unificado con regla `make up` y anclaje criptográfico por digests `sha256` en `docker-compose.yml`. | `Makefile`, `docker-compose.yml` | `c4e56f7` | ✅ Resuelto |
| **OBS-06** | 1B | Documentación de requisitos desactualizada sin alineación con estándares internacionales de ingeniería de software. | Reestructuración total del SRS bajo el estándar ISO/IEC/IEEE 29148:2018 con matriz de trazabilidad bi-direccional en `matriz.csv`; SRS final v1.0.0 con HU/CU completos en la Fase 7. | `docs/requisitos/SRS-v1.0.0.pdf`, `docs/trazabilidad/matriz.csv` | `e8f90a1` (original), Fase 7 (SRS final) | ✅ Resuelto |
| **OBS-07** | 1B | Falta de evidencias empíricas de calidad (cobertura de pruebas, pruebas de carga k6, auditoría OWASP, métricas SUS y Lighthouse). | Generación de suite de pruebas unitarias (46 tests, 22.7% JaCoCo), 5 corridas de carga k6, auditoría OWASP (ZAP + find-sec-bugs), instrumento SUS listo y 6 corridas reales de Lighthouse. Ruta reorganizada en Fase 6 (2026-08-17) para coincidir con el árbol exigido por la guía. | `k6/`, `docs/mediciones/sec/`, `docs/mediciones/sus/`, `docs/mediciones/perf/`, `docs/mediciones/jacoco/` | `f1a23b4` (original), reorganización Fase 6 | ✅ Resuelto |

---

## 📑 Detalle Metodológico de Correcciones

### 1. Estandarización de IDs a `Long` (OBS-01)
Se eliminó por completo la dependencia de `java.util.UUID` en los repositorios JPA (`JpaRepository<T, Long>`), asegurando compatibilidad nativa con secuencias de PostgreSQL y mejorando el rendimiento de indexación en B-Trees.

### 2. Esquema Híbrido de Seguridad JWT + Cookies (OBS-02)
Se configuró `JwtAuthenticationFilter` para extraer y validar el token JWT tanto desde el encabezado `Authorization: Bearer <token>` como desde la cookie de seguridad `HTTP-Only` denominada `jwtToken`. Esto garantiza compatibilidad tanto para aplicaciones cliente SPA (Angular) como para consumidores móviles y Postman.

### 3. Procedimientos Almacenados Cero-Inyección SQL (OBS-03) — 🟡 Parcial, no cerrado
Las operaciones de cálculo de notas, generación de actas y asignación masiva se encapsularon en 4 funciones PL/pgSQL parametrizadas en `V2__stored_procedures.sql`, correctas y sin concatenación manual de SQL. Sin embargo, a la fecha de esta revisión ninguna está conectada desde la capa de aplicación: no existe invocación Java (ni vía `@Procedure`, `@NamedStoredProcedureQuery` ni de ninguna otra forma) y, al estar Flyway desactivado, ni siquiera hay garantía de que el script se haya ejecutado contra la base de datos real. Declarar esta observación como cerrada sin esa conexión sería reportar evidencia que no corresponde al estado real del código. Queda pendiente como parte del trabajo de la Entrega Final.

---

*Fecha de cierre de observaciones:* 30 de Julio de 2026  
*Responsable de verificación:* Comité Docente / Equipo Titulación UTEQ
