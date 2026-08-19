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

### 3. Procedimientos Almacenados Cero-Inyección SQL (OBS-03) — ✅ Resuelto (fusión de ramas, 18 ago 2026)
El trabajo de corrección de esta observación avanzó en paralelo en dos ramas de trabajo del equipo, con hallazgos independientes que coinciden: el SQL original de `V2__stored_procedures.sql` referenciaba nombres de tabla que nunca existieron en el esquema real (`estudiantes`, `solicitudes`, `cronogramas`, `salas`, `jurados` en vez de `estudiante`, `solicitud`, `cronograma`, `sala`, `miembros_tribunal`) y trataba `estado` como columna directa cuando es una FK `estado_id`. Como ninguno se había invocado nunca desde Java, el error no se había manifestado.

Al fusionar, los 6 procedimientos/funciones que exige el criterio P1 (mínimo 6, cubriendo las 5 categorías funcionales) quedan conectados y verificados: `sp_calcular_promedio_evaluacion` y `sp_generar_reporte_defensas` (cálculos agregados / consultas multi-tabla, corregidos y verificados contra Docker), `sp_asignar_jurado_masivo` (actualizaciones masivas, corregido y probado con un caso real de rollback transaccional), `sp_firmar_acta_digital` (actualizaciones masivas), y los 2 nuevos `sp_validar_conflicto_jurado` y el de generación de código secuencial (validaciones cruzadas / generación de códigos, las 2 categorías que faltaban). Detalle completo, firmas reales y evidencia de verificación de cada uno en `docs/basedatos/CATALOGO-SP.md`.

Flyway (`spring.flyway.enabled=false`) sigue pendiente de activar — sin eso no hay garantía de que las migraciones se hayan aplicado de forma reproducible desde una base de datos vacía, que es justamente lo que exige el criterio R1 de reproducibilidad. Ese punto queda documentado como pendiente en la Fase 2, no en esta observación.

---

*Fecha de cierre de observaciones:* 30 de Julio de 2026  
*Responsable de verificación:* Comité Docente / Equipo Titulación UTEQ
