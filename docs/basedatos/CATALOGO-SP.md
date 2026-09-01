# 🗄️ CATÁLOGO DE PROCEDIMIENTOS ALMACENADOS Y FUNCIONES SQL (PL/pgSQL)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Motor de BD:** PostgreSQL 15.x
**Migraciones Flyway:** [`V2__stored_procedures.sql`](../../backend/src/main/resources/db/migration/V2__stored_procedures.sql) (1-4), [`V3__stored_procedures_validacion_y_codigos.sql`](../../backend/src/main/resources/db/migration/V3__stored_procedures_validacion_y_codigos.sql) (5-6) — ver la decisión explícita de convención de carpetas en [`../adr/ADR-006-estrategia-hibrida-bd-sp.md`](../adr/ADR-006-estrategia-hibrida-bd-sp.md)

---

## ⚠️ Objetos duplicados detectados en auditoría (2026-08-29)

`V10__actualizar_procedimientos_fase3.sql` (cabecera interna aún dice `V3__...`, resto de una
renumeración de la fusión de ramas — ver nota de fusión del ítem 3 más abajo) **también** declara
`sp_calcular_promedio_evaluacion(BIGINT)` y `sp_generar_reporte_defensas(VARCHAR)` como `FUNCTION ...
RETURNS TABLE`, con **un solo parámetro** cada una (sin el `INOUT refcursor`). Como PostgreSQL
identifica una rutina por `(esquema, nombre, tipos de parámetros)`, esta firma de un solo parámetro
es distinta a la `PROCEDURE` de dos parámetros de `V2`/`V3` documentada abajo — no la reemplaza, sino
que **coexiste como una sobrecarga (overload) separada y sin uso**: el código Java (`@Procedure`/
`@NamedStoredProcedureQuery`, ver más abajo) invoca explícitamente la firma de dos parámetros, así que
sigue resolviendo contra la `PROCEDURE` correcta (confirmado: la suite de tests del backend pasa
completa, 61/61). Estas dos `FUNCTION` son objetos huérfanos en el esquema real, resultado de la
fusión de ramas del equipo — quedan documentadas aquí para que no se confundan con la firma que
realmente usa la aplicación; no se modifica `V10` porque es una migración Flyway ya aplicada
(alterarla invalidaría el checksum de `flyway_schema_history` en cualquier base de datos existente).

## ⚠️ Actualización de este documento (Fase 3)

Una versión anterior de este catálogo describía 4 procedimientos con nombres de tabla que
**nunca existieron** en el esquema real (`estudiantes`, `solicitudes`, `cronogramas`,
`salas`, `jurados` en vez de `estudiante`, `solicitud`, `cronograma`, `sala`,
`miembros_tribunal`), y recomendaba invocarlos vía `@Query(nativeQuery = true)` — que ni
siquiera es el mecanismo que la guía exige (`@Procedure` o `@NamedStoredProcedureQuery`).
Como nada de esto se había invocado nunca desde Java, el error nunca se manifestó (ver
`docs/observaciones/OBSERVACIONES.md`, OBS-03). Este documento se reescribe con la firma SQL
real, corregida, y **verificada end-to-end contra un backend corriendo en Docker** (no solo
leída del código) para cada uno de los 6 procedimientos, incluyendo los 2 nuevos que
completan las 5 categorías funcionales exigidas por la guía.

## 📌 Introducción y Estrategia Híbrida de Acceso a Datos

El sistema implementa una **estrategia de acceso a datos híbrida**:
1. **Spring Data JPA**: operaciones CRUD elementales sobre entidades individuales (`Usuario`, `Solicitud`, `Sala`, `Anteproyecto`, etc.) — columna `tipo_acceso = CRUD-ORM` en [`../trazabilidad/matriz.csv`](../trazabilidad/matriz.csv).
2. **Procedimientos y Funciones PL/pgSQL**: obligatorios para uniones multi-tabla, cálculos agregados, actualizaciones masivas, validaciones cruzadas y generación de códigos secuenciales — columna `tipo_acceso = SP`.

### 🛡️ Protección contra inyección SQL
Los 6 procedimientos están parametrizados nativamente (`IN`/`INOUT`), invocados desde Java
exclusivamente vía JPA 2.1 `@Procedure`/`@NamedStoredProcedureQuery` — **cero** usos de
`createNativeQuery` con concatenación de cadenas en todo el backend (verificado con
[`../../scripts/audit-sql-dynamic.sh`](../../scripts/audit-sql-dynamic.sh), que además
rechaza automáticamente `EXECUTE IMMEDIATE`/`sp_executesql`/concatenación en cualquier
archivo `.sql` del repositorio).

### Nota técnica: por qué 4 de los 6 son `PROCEDURE` con `INOUT`/`REFCURSOR` y no `FUNCTION`

Probando la conexión real desde JPA se encontró que Hibernate invoca `@Procedure` y
`@NamedStoredProcedureQuery` con la sintaxis JDBC `{call proc(...)}` (CALL literal) — y
PostgreSQL **rechaza CALL para objetos `FUNCTION`** sin importar cuántos parámetros se
declaren (`"... is not a procedure. Hint: To call a function, use SELECT"`, o directamente
`"... does not exist"` si el número de parámetros no coincide). La única forma de que la
firma real en Postgres coincida con la sintaxis que Hibernate genera es declarar los
procedimientos como `PROCEDURE`:
- Retorno **escalar** (`sp_generar_codigo_expediente`, `sp_validar_conflicto_jurado`): parámetro `INOUT` para el valor de retorno.
- Retorno de **conjunto de filas** (`sp_calcular_promedio_evaluacion`, `sp_generar_reporte_defensas`): parámetro `INOUT ... refcursor`, mapeado en Java con `ParameterMode.REF_CURSOR` — y el método que lo invoca debe ser `@Transactional`, porque Postgres solo mantiene el cursor abierto dentro de la misma transacción que lo abrió.

---

## 📋 Catálogo de Procedimientos y Funciones

### 1. `sp_calcular_promedio_evaluacion` — categoría: cálculos agregados
* **Tipo:** `PROCEDURE` con parámetro `INOUT ... refcursor`
* **Propósito:** Calcula la nota final ponderada de una pre-sustentación (60% instructor + 40% promedio del tribunal por criterio de rúbrica) y persiste el resultado en `evaluaciones`.
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE presus.sp_calcular_promedio_evaluacion(
    IN p_solicitud_id BIGINT,
    INOUT p_resultado refcursor DEFAULT 'promedio_evaluacion_cursor'
)
```
* **Parámetros:** `p_solicitud_id` (IN, BIGINT) · `p_resultado` (INOUT, refcursor) — filas: `solicitud_id BIGINT`, `nota_final DOUBLE PRECISION`, `estado_resultado VARCHAR`.
* **Tablas que afecta:** lee `presus.evaluaciones_criterio` y `presus.evaluaciones`; escribe (`UPDATE`) `presus.evaluaciones`.
* **Invocación real desde Java** (`Evaluacion.java` + `EvaluacionRepository.java`):
```java
@NamedStoredProcedureQuery(
    name = "Evaluacion.calcularPromedioEvaluacion",
    procedureName = "presus.sp_calcular_promedio_evaluacion",
    resultSetMappings = "PromedioEvaluacionMapping",
    parameters = {
        @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_solicitud_id", type = Long.class),
        @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, name = "p_resultado", type = void.class)
    })
// repositorio:
@Procedure(name = "Evaluacion.calcularPromedioEvaluacion")
List<PromedioEvaluacionResult> calcularPromedioEvaluacion(@Param("p_solicitud_id") Long solicitudId);
```
* **Flujo real:** `EvaluacionServiceImpl.calcularPromedioSp()` (`@Transactional`), expuesto en `POST /api/v1/evaluaciones/{solicitudId}/calcular-promedio`.
* **Verificado real** (2026-08-17, contra Docker): `POST /api/v1/evaluaciones/1/calcular-promedio` → `200 {"solicitudId":1,"notaFinal":4.2,"estadoResultado":"REPROBADO"}`.
* **Prueba unitaria (2026-08-29):** `EvaluacionServiceImplTest` — cubre la fila base creada automáticamente si no existe, la reutilización si ya existe, y el caso en que el procedimiento no devuelve filas. Antes de esta fecha solo estaba verificado manualmente (brecha declarada explícitamente en `docs/mediciones/jacoco/COVERAGE.md`).

---

### 2. `sp_generar_reporte_defensas` — categoría: consultas multi-tabla
* **Tipo:** `PROCEDURE` con parámetro `INOUT ... refcursor`
* **Propósito:** Reporte consolidado de defensas por carrera, uniendo 6 tablas.
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE presus.sp_generar_reporte_defensas(
    IN p_carrera VARCHAR,
    INOUT p_resultado refcursor DEFAULT 'reporte_defensas_cursor'
)
```
* **Parámetros:** `p_carrera` (IN, VARCHAR) · `p_resultado` (INOUT, refcursor) — filas: `solicitud_id`, `estudiante_nombre`, `expediente`, `titulo_tema`, `estado_solicitud`, `fecha_defensa`, `sala_nombre`, `nota_final`.
* **Tablas que afecta (solo lectura):** `presus.solicitud`, `presus.estudiante`, `presus.usuarios`, `presus.estados_solicitud`, `presus.cronograma`, `presus.sala`, `presus.evaluaciones`.
* **Invocación real desde Java** (`Solicitud.java` + `SolicitudRepository.java`), mismo patrón `@NamedStoredProcedureQuery` + `ParameterMode.REF_CURSOR` que el anterior.
* **Flujo real:** `GET /api/v1/reportes/defensas?carrera=...` en `ReporteController` (`@Transactional(readOnly = true)`, requerido por el mismo motivo del refcursor).
* **Verificado real:** `GET /api/v1/reportes/defensas?carrera=Software` → `200`, con `expediente` y `notaFinal` ya calculados por los otros dos procedimientos, confirmando el cruce real entre los 6 SPs.
* **Prueba unitaria (2026-08-29):** `SolicitudServiceImplTest.testGenerarReporteDefensasSPMapeaCadaColumnaDeLaFilaCruda` — confirma que cada posición del `Object[]` crudo se mapea a la clave correcta (protege contra un cambio de orden de columnas en el SP que rompería el mapeo sin que ningún test lo detectara).

---

### 3. `sp_asignar_jurado_masivo` — categoría: actualizaciones masivas — ✅ conectado y probado
* **Tipo:** Procedimiento Almacenado PL/pgSQL (`PROCEDURE`)
* **Propósito:** Asigna (upsert) un docente como jurado de una solicitud, resolviendo el código de rol contra `roles_jurado`. La semántica "masiva" se logra invocándolo una vez por par `(solicitud, docente)` desde un único método de servicio `@Transactional`: si un par falla, se revierte también lo ya insertado del lote en esa misma llamada.
* **Corrección aplicada (14-18 ago 2026):** la versión original recibía `BIGINT[]` y escribía en una tabla `jurados(docente_id, solicitud_id, rol VARCHAR, ...)` que **no es la que usa la capa JPA real** — la entidad `Jurado.java` mapea a `miembros_tribunal(solicitud_id, docente_id, rol_jurado_id FK, ...)`. Se reescribió el procedimiento para escribir en `miembros_tribunal` resolviendo `rol_jurado_id`. También se detectó y corrigió que el `search_path` del rol de conexión (`JEAN`) no incluía el esquema `presus`, causando `relation "roles_jurado" does not exist` en tiempo de ejecución (`ALTER ROLE "JEAN" SET search_path TO presus, public;`).
* **Nota de fusión de ramas (18 ago 2026):** también se desarrolló en paralelo una variante con parámetros `BIGINT[]` (arreglo completo en una sola llamada). Se mantiene la firma escalar en el repositorio porque es la que quedó verificada end-to-end de forma reproducible, incluyendo un caso real de rollback (ver abajo); si se retoma la variante con arreglos, revalidar el binding de `Long[]` vía `@Procedure` antes de reemplazar esta.
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE presus.sp_asignar_jurado_masivo(
    p_solicitud_id BIGINT,
    p_docente_id BIGINT,
    p_rol_codigo VARCHAR
)
```
* **Parámetros:**
  - `p_solicitud_id` (BIGINT): ID de la solicitud.
  - `p_docente_id` (BIGINT): ID del docente.
  - `p_rol_codigo` (VARCHAR): código de `roles_jurado` (`'PRESIDENTE'`, `'VOCAL'`, `'SECRETARIO'`).
* **Tablas que afecta:** lee `presus.roles_jurado`; escribe (`INSERT ... ON CONFLICT DO UPDATE`) `presus.miembros_tribunal`.
* **Manejo de Errores:** `RAISE EXCEPTION` si el código de rol no existe en `roles_jurado`; la FK de `miembros_tribunal` rechaza `docente_id`/`solicitud_id` inexistentes.
* **Invocación real desde Java JPA** (`JuradoRepository.java`):
```java
@Procedure(procedureName = "sp_asignar_jurado_masivo")
void spAsignarJuradoMasivo(@Param("p_solicitud_id") Long solicitudId,
                            @Param("p_docente_id") Long docenteId,
                            @Param("p_rol_codigo") String rolCodigo);
```
Invocado desde `JuradoServiceImpl.asignarJuradoMasivo(List<Long>, List<Long>, String)`, anotado `@Transactional`, expuesto en `POST /api/v1/jurados/asignar-masivo` (roles `ADMIN`/`COORDINADOR`).
* **Prueba de control transaccional (verificada manualmente):** lote de 2 pares donde el primero es válido y el segundo viola la FK de `docente_id` → el `INSERT` del primer par se ejecuta pero, al fallar el segundo, Spring revierte la transacción completa; se confirmó que **ningún** registro del lote queda en `miembros_tribunal`.
* **Prueba unitaria (2026-08-29):** `JuradoServiceImplTest` — cubre el rechazo por longitud de arreglos distinta, que el procedimiento se invoca una vez por par, y que una excepción a mitad de lote detiene el `for` sin intentar los pares restantes (el rollback real de la fila ya insertada lo hace `@Transactional`, no el bucle Java).

---

### 4. `sp_firmar_acta_digital` — categoría: actualizaciones masivas
* **Tipo:** `PROCEDURE`
* **Propósito:** Firma digital multi-actor de actas, con auditoría de fecha y bitácora de observaciones por rol.
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE presus.sp_firmar_acta_digital(
    p_acta_id BIGINT,
    p_rol VARCHAR,
    p_observacion TEXT
)
```
* **Parámetros:** `p_acta_id` (IN, BIGINT) · `p_rol` (IN, VARCHAR — `PRESIDENTE`/`VOCAL_1`/`VOCAL_2`/`TUTOR`, alineado con la convención real del resto del backend) · `p_observacion` (IN, TEXT, opcional).
* **Tablas que afecta:** `UPDATE presus.actas` (columnas `firmada_*`, `fecha_firma_*`, y **agrega** una línea a `observaciones_acta` — un campo que la implementación Java anterior nunca escribía).
* **Invocación real desde Java** (`ActaRepository.java`):
```java
@Procedure(procedureName = "presus.sp_firmar_acta_digital")
void firmarActaDigital(@Param("p_acta_id") Long actaId, @Param("p_rol") String rol, @Param("p_observacion") String observacion);
```
* **Flujo real:** `ActaServiceImpl.firmarActa()` invoca el SP y luego `entityManager.refresh(acta)` para que el resto del flujo (cambio de estado a `COMPLETADA`, regeneración de PDF) vea lo que el procedimiento realmente persistió. Expuesto en `POST /api/v1/actas/firmar/{actaId}`.
* **Verificado real:** firma de PRESIDENTE → `200`, con `observacionesActa: "\n[PRESIDENTE]: Todo correcto"` confirmado en la respuesta.
* **Prueba unitaria (2026-08-29):** `ActaServiceImplTest` — cubre rol inválido, firma parcial (no completa la solicitud), firma completa (las 4 firmas → transición a `COMPLETADA` + regeneración real de PDF con iText contra un directorio temporal), y que un fallo en la notificación no interrumpe la firma.

---

### 5. `sp_validar_conflicto_jurado` — categoría: validaciones cruzadas *(nuevo, Fase 3)*
* **Tipo:** `PROCEDURE` con parámetro `INOUT` escalar
* **Propósito:** Antes de programar una defensa, verifica que ningún docente ya asignado como jurado de la solicitud tenga **otra** defensa programada en un horario que se solape — cruza `miembros_tribunal` con `cronograma`, dos tablas distintas.
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE presus.sp_validar_conflicto_jurado(
    IN p_solicitud_id BIGINT,
    IN p_docente_id BIGINT,
    IN p_fecha_inicio TIMESTAMP,
    IN p_duracion_min INTEGER,
    INOUT p_disponible BOOLEAN DEFAULT NULL
)
```
* **Tablas que afecta (solo lectura):** `presus.miembros_tribunal`, `presus.cronograma`.
* **Invocación real desde Java** (`Jurado.java` + `JuradoRepository.java`):
```java
@Procedure(name = "Jurado.validarConflictoJurado")
Boolean validarConflictoJurado(@Param("p_solicitud_id") Long solicitudId, @Param("p_docente_id") Long docenteId,
                                @Param("p_fecha_inicio") LocalDateTime fechaInicio, @Param("p_duracion_min") Integer duracionMin,
                                @Param("p_disponible") Boolean disponibleInicial);
```
* **Flujo real:** `CronogramaServiceImpl.crearCronograma()` lo llama para cada jurado ya asignado antes de guardar el cronograma; si algún docente tiene conflicto, se rechaza con un mensaje explícito.
* **Verificado real:** (a) creación de cronograma con 3 jurados sin conflictos previos → `200`; (b) prueba directa por SQL (`CALL` con un docente ya ocupado en un horario solapado) → `p_disponible = f`, confirmando también la rama de conflicto.
* **Prueba unitaria:** `CronogramaServiceImplTest.testCrearCronogramaFallaPorConflictoDeJurado` mockea la respuesta `Boolean.FALSE` del procedimiento y confirma que el servicio la traduce en el mensaje de conflicto esperado.

---

### 6. `sp_generar_codigo_expediente` — categoría: generación de códigos secuenciales *(nuevo, Fase 3)*
* **Tipo:** `PROCEDURE` con parámetro `INOUT` escalar
* **Propósito:** Genera el código de expediente de un estudiante (formato `EXP-<año>-NNNNN`) usando `nextval()` sobre una secuencia dedicada — atómico a nivel de motor, así que dos altas concurrentes nunca reciben el mismo código (a diferencia de calcular `MAX(id)+1` en Java, que sí tendría condición de carrera).
* **Firma SQL real:**
```sql
CREATE SEQUENCE IF NOT EXISTS presus.expediente_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE PROCEDURE presus.sp_generar_codigo_expediente(
    IN p_anio INTEGER,
    INOUT p_codigo VARCHAR DEFAULT NULL
)
```
* **Tablas que afecta:** ninguna tabla — solo la secuencia `presus.expediente_codigo_seq`.
* **Invocación real desde Java** (`Estudiante.java` + `EstudianteRepository.java`):
```java
@Procedure(name = "Estudiante.generarCodigoExpediente")
String generarCodigoExpediente(@Param("p_anio") Integer anio, @Param("p_codigo") String codigoInicial);
```
* **Flujo real:** `SolicitudServiceImpl.crearPerfilEstudiante()` lo llama al crear automáticamente el perfil de un estudiante nuevo.
* **Verificado real:** creación de solicitud de punta a punta → `expedienteCodigo: "EXP-2026-00001"` en la respuesta real del backend.
* **Prueba unitaria:** `SolicitudServiceImplTest.testCrearSolicitudPorUsuarioCreaPerfilEstudianteAutomaticamente` verifica que `crearPerfilEstudiante()` invoca `generarCodigoExpediente` y que el código devuelto por el procedimiento (no calculado en Java) queda en el estudiante guardado.

---

## 📊 Cobertura de las categorías funcionales exigidas por la guía

La guía enumera 6 categorías (multi-tabla, agregados, **reportes**, actualizaciones masivas,
validaciones cruzadas, códigos secuenciales); "reportes" y "consultas multi-tabla" se satisfacen
aquí con el **mismo** procedimiento porque `sp_generar_reporte_defensas` es, por definición, un
reporte que cruza 6 tablas — no hay dos procedimientos separados para esas dos filas, es una
única pieza de SQL sirviendo ambos requisitos, documentado así explícitamente para que quede
trazable en vez de implícito.

| Categoría (guía) | Procedimiento(s) | Estado | Prueba unitaria |
|---|---|---|---|
| Consultas multi-tabla / Reportes | `sp_generar_reporte_defensas` | ✅ Conectado y verificado | ✅ `SolicitudServiceImplTest` |
| Cálculos agregados | `sp_calcular_promedio_evaluacion` | ✅ Conectado y verificado | ✅ `EvaluacionServiceImplTest` |
| Actualizaciones masivas | `sp_asignar_jurado_masivo`, `sp_firmar_acta_digital` | ✅ Conectados y verificados | ✅ `JuradoServiceImplTest`, `ActaServiceImplTest` |
| Validaciones cruzadas | `sp_validar_conflicto_jurado` | ✅ Conectado y verificado (Fase 3) | ✅ `CronogramaServiceImplTest` |
| Generación de códigos secuenciales | `sp_generar_codigo_expediente` | ✅ Conectado y verificado (Fase 3) | ✅ `SolicitudServiceImplTest` |

**Total: 6 procedimientos, 6 conectados desde código Java real vía JPA 2.1, 6 verificados end-to-end
contra un backend corriendo en Docker, y (actualizado 2026-08-29) 6 con prueba unitaria dedicada**
— antes de esta fecha, `sp_calcular_promedio_evaluacion` y `sp_generar_reporte_defensas` solo
estaban verificados manualmente (brecha que declaraba explícitamente `docs/mediciones/jacoco/COVERAGE.md`);
`sp_asignar_jurado_masivo` tampoco tenía prueba dedicada pese a que `JuradoServiceImplTest` ya
existía para otras responsabilidades de esa clase. Supera el mínimo de 6 exigido por el criterio P1.
