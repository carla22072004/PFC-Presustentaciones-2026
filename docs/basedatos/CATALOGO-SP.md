# 🗄️ CATÁLOGO DE PROCEDIMIENTOS ALMACENADOS Y FUNCIONES SQL (PL/pgSQL)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Motor de BD:** PostgreSQL 15.x  
**Esquema de Migración:** Flyway — [`backend/src/main/resources/db/migration/V2__stored_procedures.sql`](../../backend/src/main/resources/db/migration/V2__stored_procedures.sql) (ver la decisión explícita de convención de carpetas en [`../adr/ADR-003-estrategia-hibrida-bd-sp.md`](../adr/ADR-003-estrategia-hibrida-bd-sp.md))  

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

### 3. `sp_asignar_jurado_masivo`
* **Tipo:** Procedimiento Almacenado PL/pgSQL (`PROCEDURE`)
* **Propósito:** Realiza la asignación masiva y transaccional de un arreglo de docentes como jurados a un arreglo de solicitudes de pre-sustentación en un único paso.
* **Firma SQL:**
```sql
CREATE OR REPLACE PROCEDURE sp_asignar_jurado_masivo(
    p_solicitud_ids BIGINT[],
    p_docente_ids BIGINT[],
    p_rol VARCHAR
)
```
* **Parámetros:**
  - `p_solicitud_ids` (BIGINT[]): Arreglo de IDs de solicitudes.
  - `p_docente_ids` (BIGINT[]): Arreglo de IDs de docentes.
  - `p_rol` (VARCHAR): Rol del jurado (`'PRESIDENTE'`, `'VOCAL1'`, `'VOCAL2'`).
* **Manejo de Errores:** Lanza excepción si las longitudes de los arreglos no coinciden.
* **Invocación desde Java JPA:**
```java
@Procedure(procedureName = "sp_asignar_jurado_masivo")
void asignarJuradoMasivo(Long[] solicitudIds, Long[] docenteIds, String rol);
```

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
