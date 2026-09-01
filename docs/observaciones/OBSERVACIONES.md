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

> **Nota de integridad (auditoría 2026-08-29, re-verificada 2026-08-31 con `git cat-file -e`):** los 7
> hashes de commit citados originalmente en esta tabla no existen en el historial de este repositorio
> (ninguno resuelve a un objeto real) — eran marcadores de posición inventados, nunca verificados contra
> `git log`. Se omiten aquí deliberadamente (no se citan strings con forma de hash que no correspondan a
> un commit real, ni siquiera para ilustrar el error) y se reemplazan por los
> commits reales verificados con `git show --stat`. La resolución de OBS-02/03/04 sí ocurrió en un
> único commit (`bda64ec`); OBS-05/06/07 ocurrieron en otro commit único posterior (`015fd6d`), pero
> más amplio (integra Makefile, digests SHA-256, SRS, matriz de trazabilidad, k6, OWASP y SUS de una
> sola vez) en vez de en commits separados por observación. OBS-01 no tiene un commit verificable: su
> migración UUID→Long ocurrió antes de `65403ee "Migración inicial: Proyecto limpio sin binarios ni
> dependencias"`, que reinició el historial visible del repositorio; solo es verificable por
> inspección del código actual, no por hash de commit.

| ID | Fuente (Entrega) | Criterio afectado | Observación del Docente | Decisión Técnica Aplicada | Componente Afectado | Hash del Commit | Relación | Estado |
|---|---|---|---|---|---|---|---|---|
| **OBS-01** | 1A | Modelado de datos / JPA-BD | Incompatibilidad de tipos de clave primaria UUID en entidades JPA al interactuar con PostgreSQL en consultas relacionales. | Migración integral de identificadores `UUID` a `Long` autoincrementales (`BIGSERIAL`) en las 13 entidades, repositorios, servicios y DTOs REST. | `backend/src/main/java/...` | *No verificable — anterior a la limpieza de historial `65403ee`; confirmado por inspección del código actual (entidades usan `Long`/`BIGSERIAL`)* | Precondición de OBS-03 (los SP usan `BIGINT`, no `UUID`) | ✅ Resuelto |
| **OBS-02** | 1A | Seguridad (P5) | Ausencia de capa de seguridad JWT con Spring Security y falta de protección en endpoints críticos del backend. | Implementación completa de Spring Security 6 con `JwtTokenProvider`, `JwtAuthenticationFilter` y soporte para Cookies de seguridad HTTP-Only. | `backend/src/main/java/ec/edu/uteq/presustentaciones/security/` | `bda64ec` (tag `v0.7.1`) | — | ✅ Resuelto |
| **OBS-03** | 1B | Acceso a datos / SP (P1) | Las consultas complejas (uniones de tablas, generación de reportes y promedio de notas) se ejecutaban en la capa de aplicación sin optimización de BD. | Se escribieron 4 procedimientos almacenados PostgreSQL (`sp_calcular_promedio_evaluacion`, `sp_generar_reporte_defensas`, `sp_asignar_jurado_masivo`, `sp_firmar_acta_digital`), técnicamente correctos, invocados desde Java vía `@Procedure`/`@NamedStoredProcedureQuery` (ver `docs/basedatos/CATALOGO-SP.md`). | `backend/src/main/resources/db/migration/V2__stored_procedures.sql` | `bda64ec` (tag `v0.7.1`, conexión de los SP), `ab73b30`/`41796cc` (2026-08-16, activación de Flyway) | Depende de OBS-01 | ✅ Resuelto |
| **OBS-04** | 1B | Documentación API | Falta de documentación interactiva OpenAPI 3.0 (Swagger UI) para pruebas de la API REST por parte del jurado y frontend. | Integración de `springdoc-openapi-starter-webmvc-ui` y configuración de esquema de autenticación Bearer JWT en `OpenApiConfig.java`. | `backend/src/main/java/.../config/OpenApiConfig.java` | `bda64ec` (tag `v0.7.1`) | — | ✅ Resuelto |
| **OBS-05** | 1A | Despliegue / R1 | El despliegue requería múltiples pasos manuales e instalaciones dependientes del sistema operativo. | Creación de `Makefile` unificado con regla `make up` y anclaje criptográfico por digests `sha256` en `docker-compose.yml`. | `Makefile`, `docker-compose.yml` | `015fd6d` | Comparte commit con OBS-06/OBS-07 | ✅ Resuelto |
| **OBS-06** | 1B | Requisitos (D0R) | Documentación de requisitos desactualizada sin alineación con estándares internacionales de ingeniería de software. | Reestructuración total del SRS bajo el estándar ISO/IEC/IEEE 29148:2018 con matriz de trazabilidad bi-direccional en `matriz.csv`; SRS final v1.0.0 con HU/CU completos en la Fase 7. | `docs/requisitos/SRS-v1.0.0.pdf`, `docs/trazabilidad/matriz.csv` | `015fd6d` (versión inicial ISO 29148), Fase 7 (SRS final, ver commits posteriores en `docs/requisitos/`) | Comparte commit con OBS-05/OBS-07 | ✅ Resuelto |
| **OBS-07** | 1B | Evidencia empírica (P4) | Falta de evidencias empíricas de calidad (cobertura de pruebas, pruebas de carga k6, auditoría OWASP, métricas SUS y Lighthouse). | Generación de suite de pruebas unitarias, corridas de carga k6, auditoría OWASP (ZAP + find-sec-bugs), instrumento SUS y corridas de Lighthouse. Ruta reorganizada en Fase 6 (2026-08-17) para coincidir con el árbol exigido por la guía. | `k6/`, `docs/mediciones/sec/`, `docs/mediciones/sus/`, `docs/mediciones/perf/`, `docs/mediciones/jacoco/` | `015fd6d` (original), reorganización Fase 6 | Comparte commit con OBS-05/OBS-06 | ✅ Resuelto |

---

## 📑 Detalle Metodológico de Correcciones

### 1. Estandarización de IDs a `Long` (OBS-01)
Se eliminó por completo la dependencia de `java.util.UUID` en los repositorios JPA (`JpaRepository<T, Long>`), asegurando compatibilidad nativa con secuencias de PostgreSQL y mejorando el rendimiento de indexación en B-Trees.

### 2. Esquema Híbrido de Seguridad JWT + Cookies (OBS-02)
Se configuró `JwtAuthenticationFilter` para extraer y validar el token JWT tanto desde el encabezado `Authorization: Bearer <token>` como desde la cookie de seguridad `HTTP-Only` denominada `jwtToken`. Esto garantiza compatibilidad tanto para aplicaciones cliente SPA (Angular) como para consumidores móviles y Postman.

### 3. Procedimientos Almacenados Cero-Inyección SQL (OBS-03) — ✅ Resuelto (fusión de ramas, 18 ago 2026)
El trabajo de corrección de esta observación avanzó en paralelo en dos ramas de trabajo del equipo, con hallazgos independientes que coinciden: el SQL original de `V2__stored_procedures.sql` referenciaba nombres de tabla que nunca existieron en el esquema real (`estudiantes`, `solicitudes`, `cronogramas`, `salas`, `jurados` en vez de `estudiante`, `solicitud`, `cronograma`, `sala`, `miembros_tribunal`) y trataba `estado` como columna directa cuando es una FK `estado_id`. Como ninguno se había invocado nunca desde Java, el error no se había manifestado.

Al fusionar, los 6 procedimientos/funciones que exige el criterio P1 (mínimo 6, cubriendo las 5 categorías funcionales) quedan conectados y verificados: `sp_calcular_promedio_evaluacion` y `sp_generar_reporte_defensas` (cálculos agregados / consultas multi-tabla, corregidos y verificados contra Docker), `sp_asignar_jurado_masivo` (actualizaciones masivas, corregido y probado con un caso real de rollback transaccional), `sp_firmar_acta_digital` (actualizaciones masivas), y los 2 nuevos `sp_validar_conflicto_jurado` y el de generación de código secuencial (validaciones cruzadas / generación de códigos, las 2 categorías que faltaban). Detalle completo, firmas reales y evidencia de verificación de cada uno en `docs/basedatos/CATALOGO-SP.md`.

**Actualización 2026-08-16 (commits `ab73b30`/`41796cc`):** Flyway se activó (`spring.flyway.enabled=true` en `backend/src/main/resources/application.properties`) — las migraciones `V1`-`V18` ahora se aplican automáticamente al arrancar el backend, incluida contra una base de datos vacía (verificado por `make wait-backend`, que espera a `/actuator/health` = UP, algo que Spring Boot nunca reporta si una migración Flyway falla en el arranque). Esta observación se marca **✅ Resuelto** en su totalidad; el estado "🟡 Parcial" que este documento declaraba anteriormente ya no refleja el código actual.

---

*Fecha de cierre de observaciones:* 30 de Julio de 2026  
*Responsable de verificación:* Comité Docente / Equipo Titulación UTEQ
