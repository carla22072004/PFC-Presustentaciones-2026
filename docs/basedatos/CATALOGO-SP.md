# 🗄️ CATÁLOGO DE PROCEDIMIENTOS ALMACENADOS Y FUNCIONES SQL (PL/pgSQL)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Motor de BD:** PostgreSQL 15.x  
**Esquema de Migración:** Flyway (`V2__stored_procedures.sql`)  

---

## 📌 Introducción y Estrategia Híbrida de Acceso a Datos

El sistema implementa una **estrategia de acceso a datos híbrida**:
1. **Spring Data JPA**: Utilizado para operaciones CRUD elementales (Create, Read, Update, Delete) sobre entidades individuales (`Usuario`, `Solicitud`, `Sala`, `Cronograma`, `Anteproyecto`).
2. **Procedimientos Almacenados y Funciones PL/pgSQL Puras**: Obligatorios para cualquier consulta compleja, uniones multi-tabla (`JOIN`), procesamiento analítico de reportes, cálculos de notas ponderadas y actualizaciones transaccionales masivas.

### 🛡️ Protección contra Inyección SQL
Todas las funciones y procedimientos almacenados están parametrizados nativamente. Se prohíbe terminantemente el uso de SQL dinámico (`EXECUTE string`) o la concatenación de variables de usuario.

---

## 📋 Catálogo de Procedimientos y Funciones

### 1. `sp_calcular_promedio_evaluacion`
* **Tipo:** Función PL/pgSQL (Retorna Tabla)
* **Propósito:** Calcula de forma atómica y determinista la nota final ponderada de una pre-sustentación (60% nota del tutor/instructor + 40% promedio de jurados por criterios de rúbrica). Actualiza automáticamente el estado en la tabla `evaluaciones`.
* **Firma SQL:**
```sql
CREATE OR REPLACE FUNCTION sp_calcular_promedio_evaluacion(p_solicitud_id BIGINT)
RETURNS TABLE (
    solicitud_id BIGINT,
    nota_final DOUBLE PRECISION,
    estado_resultado VARCHAR
)
```
* **Parámetros:**
  - `p_solicitud_id` (BIGINT): Identificador de la solicitud de pre-sustentación.
* **Lógica interna:**
  1. Calcula `AVG(nota_obtenida)` desde `evaluaciones_criterio`.
  2. Obtiene `nota_instructor` desde `evaluaciones`.
  3. Aplica fórmula: `(nota_instructor * 0.60) + (nota_jurado * 0.40)`.
  4. Determina estado: `APROBADO` (>= 7.0), `CON_OBSERVACIONES` (5.0 - 6.9), `REPROBADO` (< 5.0).
  5. Realiza `UPDATE` en `evaluaciones` y retorna la tupla.
* **Invocación desde Java JPA:**
```java
@Query(value = "SELECT * FROM sp_calcular_promedio_evaluacion(:solicitudId)", nativeQuery = true)
List<Object[]> calcularPromedioEvaluacion(@Param("solicitudId") Long solicitudId);
```

---

### 2. `sp_generar_reporte_defensas`
* **Tipo:** Función PL/pgSQL (Retorna Tabla de Reporte)
* **Propósito:** Encapsula la unión compleja de 6 tablas (`solicitudes`, `estudiantes`, `usuarios`, `cronogramas`, `salas`, `evaluaciones`) para generar el reporte general de pre-sustentaciones por carrera.
* **Firma SQL:**
```sql
CREATE OR REPLACE FUNCTION sp_generar_reporte_defensas(p_carrera VARCHAR)
RETURNS TABLE (
    solicitud_id BIGINT,
    estudiante_nombre TEXT,
    expediente VARCHAR,
    titulo_tema VARCHAR,
    estado_solicitud VARCHAR,
    fecha_defensa TIMESTAMP,
    sala_nombre VARCHAR,
    nota_final DOUBLE PRECISION
)
```
* **Parámetros:**
  - `p_carrera` (VARCHAR): Nombre o coincidencia parcial de la carrera (e.g. `'Ingeniería en Software'`).
* **Invocación desde Java JPA:**
```java
@Query(value = "SELECT * FROM sp_generar_reporte_defensas(:carrera)", nativeQuery = true)
List<Object[]> generarReporteDefensas(@Param("carrera") String carrera);
```

---

### 3. `sp_asignar_jurado_masivo` — ✅ Conectado y probado (única SP verificada end-to-end)
* **Tipo:** Procedimiento Almacenado PL/pgSQL (`PROCEDURE`)
* **Propósito:** Asigna (upsert) un docente como jurado de una solicitud, resolviendo el código de rol contra `roles_jurado`. La semántica "masiva" se logra invocándolo una vez por par `(solicitud, docente)` desde un único método de servicio `@Transactional`: si un par falla, se revierte también lo ya insertado del lote en esa misma llamada.
* **Corrección aplicada (14-18 ago 2026):** la versión original recibía `BIGINT[]` y escribía en una tabla `jurados(docente_id, solicitud_id, rol VARCHAR, ...)` que **no es la que usa la capa JPA real** — la entidad `Jurado.java` mapea a `miembros_tribunal(solicitud_id, docente_id, rol_jurado_id FK, ...)`. Se reescribió el procedimiento para escribir en `miembros_tribunal` resolviendo `rol_jurado_id`, y se cambió de parámetros array a escalares por par (los arrays de PostgreSQL no se bindean de forma fiable vía `@Procedure`/JPA). También se detectó y corrigió que el `search_path` del rol de conexión (`JEAN`) no incluía el esquema `presus`, causando `relation "roles_jurado" does not exist` en tiempo de ejecución (`ALTER ROLE "JEAN" SET search_path TO presus, public;`).
* **Firma SQL real:**
```sql
CREATE OR REPLACE PROCEDURE sp_asignar_jurado_masivo(
    p_solicitud_id BIGINT,
    p_docente_id BIGINT,
    p_rol_codigo VARCHAR
)
```
* **Parámetros:**
  - `p_solicitud_id` (BIGINT): ID de la solicitud.
  - `p_docente_id` (BIGINT): ID del docente.
  - `p_rol_codigo` (VARCHAR): código de `roles_jurado` (`'PRESIDENTE'`, `'VOCAL'`, `'SECRETARIO'`).
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

> **Nota de estado real:** `sp_calcular_promedio_evaluacion` (#1), `sp_generar_reporte_defensas` (#2) y `sp_firmar_acta_digital` (#4) documentados arriba **siguen sin invocarse desde código Java** y, al verificar sus cuerpos SQL contra el esquema real (`presus`), `sp_generar_reporte_defensas` referencia tablas en plural (`solicitudes`, `estudiantes`, `cronogramas`, `salas`) que no existen — las tablas reales son singulares (`solicitud`, `estudiante`, `cronograma`, `sala`) — y `sp_calcular_promedio_evaluacion` actualiza columnas (`estado`, `actualizado_en`) que no existen en la tabla real `evaluaciones` (las columnas reales son `resultado`, sin columna de fecha de actualización). Ambas quedan pendientes de corrección y conexión.

---

### 4. `sp_firmar_acta_digital`
* **Tipo:** Procedimiento Almacenado PL/pgSQL (`PROCEDURE`)
* **Propósito:** Gestiona el flujo transaccional de firma digital multi-actor de actas de pre-sustentación. Garantiza auditoría con marca de tiempo e impresión de observaciones por rol.
* **Firma SQL:**
```sql
CREATE OR REPLACE PROCEDURE sp_firmar_acta_digital(
    p_acta_id BIGINT,
    p_rol VARCHAR,
    p_observacion TEXT
)
```
* **Parámetros:**
  - `p_acta_id` (BIGINT): ID del acta a firmar.
  - `p_rol` (VARCHAR): Rol que efectúa la firma (`'PRESIDENTE'`, `'VOCAL1'`, `'VOCAL2'`, `'TUTOR'`).
  - `p_observacion` (TEXT): Comentario u observación opcional anexada a la firma.

---

## 📊 Matriz de Cobertura de Operaciones Complejas

| Operación Compleja | Enfoque Anterior | Enfoque Actual (SP/PL SQL) | Inyección SQL Posible |
|---|---|---|---|
| Cálculo Nota Final | Lógica en Java con bucles en memoria | `sp_calcular_promedio_evaluacion` | ❌ Imposible (Parámetros tipados) |
| Reporte de Titulación | 6 consultas JPA iterativas | `sp_generar_reporte_defensas` | ❌ Imposible (Binds nativos ILIKE) |
| Asignación de Tribunal | N inserciones en bucle `@Transactional` | `sp_asignar_jurado_masivo` | ❌ Imposible (Arrays Postgres) |
| Firma Digital de Acta | Múltiples updates en entidad JPA | `sp_firmar_acta_digital` | ❌ Imposible (Caso enum explícito) |
