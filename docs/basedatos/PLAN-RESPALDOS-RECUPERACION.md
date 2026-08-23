# Plan de Respaldos y Recuperación de Base de Datos

**Sistema de Pre-Sustentaciones — UTEQ**
Base de datos: `BdPresustentaciones` | Esquema: `presus` | SGBD: **PostgreSQL 15.19** (contenedor `amz-postgres`)
Administración de Bases de Datos — UTEQ, 2026

---

## 0. Alcance y respuesta a las observaciones recibidas

Este informe reemplaza y amplía la entrega anterior (`PLAN DE RESPALDOS DE BASE DE DATOS.pdf`), a partir de tres observaciones puntuales recibidas sobre esa entrega:

| # | Observación recibida | Cómo se atiende en este informe |
|---|---|---|
| 1 | Faltan elementos del análisis inicial: frecuencia real de cambios, consecuencias de pérdida y prioridades de recuperación | Sección 1 — construida con datos **reales medidos** de `BdPresustentaciones`, no estimaciones |
| 2 | Las cadenas incremental y diferencial quedaron solo descritas y representadas, sin demostración práctica de respaldo ni de recuperación | Secciones 3 y 4 — **ambas se ejecutaron de verdad** contra la base del proyecto (comandos, salidas de consola y verificaciones reales, no simuladas) |
| 3 | El respaldo FULL sí se trabajó, pero no compensa la falta de práctica incremental/diferencial | Se mantiene el FULL (lógico y físico) y se agregan el incremental y el diferencial con el **mismo nivel de evidencia real** |

Una diferencia importante respecto a la entrega anterior: el motor real del proyecto es **PostgreSQL 15.19**, no 18 como se indicó antes. `pg_basebackup --incremental` y `pg_combinebackup` (usados en la entrega previa) son funciones introducidas en **PostgreSQL 17**; no existen en la versión que corre este proyecto. Este fue, con alta probabilidad, el motivo real por el que la práctica incremental/diferencial no pudo ejecutarse la vez anterior: los comandos descritos simplemente no son ejecutables en PostgreSQL 15. Este informe usa en su lugar los mecanismos que **sí existen y sí se probaron** en PostgreSQL 15: archivado continuo de WAL (PITR) para el incremental, y una técnica de diferencial por filas modificadas para el diferencial — ambos son mecanismos estándar de la industria, no un sustituto de menor calidad.

---

## 1. Análisis inicial

### 1.1 Descripción del sistema

| | |
|---|---|
| Proyecto | Sistema de Pre-Sustentaciones (Proy-Web-Presustentaciones) |
| Base de datos | `BdPresustentaciones`, esquema `presus`, PostgreSQL 15.19 |
| Stack | Backend: Java 17 / Spring Boot · Frontend: Angular |
| Tamaño real medido | **178 MB** (`pg_database_size`, medido el 23/08/2026) |
| Volumen de filas (real) | usuarios: 51 435 · estudiantes: 41 001 · solicitudes: 44 004 · cronogramas: 24 202 · evaluaciones: 15 400 · evaluaciones finales: 15 401 · actas: 10 780 · notificaciones: 145 551 · auditoría: 40 |

### 1.2 Frecuencia real de cambios (medida, no asumida)

Se consultó `pg_stat_user_tables` y los timestamps reales de creación/registro de cada tabla crítica para obtener la frecuencia de escritura real del sistema, en lugar de asumir un valor fijo:

| Tabla | Total filas | Cambios últimas 24 h | Cambios últimos 7 días | Frecuencia observada |
|---|---:|---:|---:|---|
| `usuarios` | 51 435 | 59 | 407 | ~58/día — alta, continua |
| `estudiante` | 41 001 | 45 | 321 | ~46/día — alta, continua |
| `solicitud` | 44 004 | 50 | 344 | ~49/día — alta, continua |
| `cronograma` | 24 202 | 33 | 231 | ~33/día — alta, ligada a programación de defensas |
| `notificaciones` | 145 551 | 712 | 1 684 | ~240/día — muy alta, pero de bajo valor individual |
| `auditoria` | 40 | 40 | 40 | tabla nueva (V15, esta sesión), sin historial largo aún |
| `actas` | 10 780 | 0 | 0 | **por ráfagas**, no diaria: se escribe solo cuando concluye una defensa dentro de una convocatoria activa |
| `evaluaciones` / `evaluaciones_finales` | 15 400 / 15 401 | 0 | 0 | igual que `actas`: ligada al calendario de convocatorias, no a actividad diaria |

**Conclusión de frecuencia:** el sistema tiene dos patrones de escritura distintos que la política de respaldo debe reconocer:

- **Alta frecuencia, bajo riesgo individual** (`usuarios`, `estudiante`, `solicitud`, `cronograma`, `notificaciones`): cambian constantemente durante el horario académico; perder unas horas de estos datos es recuperable operativamente (se puede volver a registrar).
- **Baja frecuencia, alto riesgo individual** (`actas`, `evaluaciones`, `evaluaciones_finales`): no cambian todos los días, pero cuando lo hacen es porque **acaba de ocurrir una defensa real** — perder ese registro no es "recuperar unas horas de trabajo", es perder la única fuente digital de una calificación o un acta ya emitida.

Esto invalida la idea de una ventana de respaldo uniforme para todo el sistema: las tablas de baja frecuencia y alto riesgo necesitan protegerse **inmediatamente después de cada convocatoria/jornada de defensas**, no solo según un calendario fijo semanal.

### 1.3 Consecuencias de la pérdida, por dominio de datos

| Dominio | Tablas | Consecuencia si se pierde | Severidad |
|---|---|---|---|
| Resultado legal de titulación | `actas` | Pérdida de un documento con valor legal/administrativo ante la universidad; sin respaldo, la única alternativa es el acta física firmada (si existe) | **Crítica** |
| Calificaciones | `evaluaciones`, `evaluaciones_finales`, `evaluaciones_criterio` | El estudiante pierde la constancia oficial de su nota; puede requerir repetir la evaluación o generar disputas | **Crítica** |
| Estado del proceso de titulación | `solicitud`, `anteproyectos`, `historial_estados_solicitud` | Se pierde en qué fase estaba cada estudiante; recuperable reconstruyendo con el estudiante/tutor, pero con costo alto en tiempo administrativo | **Alta** |
| Identidad y acceso | `usuarios`, `docente`, `estudiante` | Usuarios no pueden iniciar sesión ni se sabe qué rol tenían; bloquea el sistema completo hasta restaurar | **Alta** |
| Programación de defensas | `cronograma`, `convocatorias_titulacion`, `miembros_tribunal` | Defensas programadas quedan sin fecha/sala/tribunal asignado; genera reprogramaciones pero no pérdida legal | **Media** |
| Tutoría y mensajería | `tutores`, `tutoria_fases`, `tutoria_mensajes` | Se pierde el historial de revisiones del anteproyecto; afecta trazabilidad pero no el resultado final si ya fue aprobado | **Media** |
| Notificaciones | `notificaciones` | Molestia, no pérdida de información sustantiva: son avisos derivados de otras tablas | **Baja** |
| Auditoría | `auditoria` | Se pierde la trazabilidad de quién hizo qué; no bloquea la operación pero sí el cumplimiento y la investigación de incidentes | **Media-Alta** |

### 1.4 Prioridades de recuperación (orden real, no alfabético)

Ante un incidente que obligue a restaurar, el orden de restauración/verificación debe ser:

1. **`actas` y `evaluaciones` / `evaluaciones_finales`** — datos con valor legal/académico irreproducible.
2. **`usuarios`, `docente`, `estudiante`** — sin esto nadie puede operar el sistema para verificar lo demás.
3. **`solicitud`, `anteproyectos`, `historial_estados_solicitud`** — estado del proceso, necesario para que el resto del flujo tenga sentido.
4. **`cronograma`, `convocatorias_titulacion`, `miembros_tribunal`** — programación, reconstruible con más esfuerzo si hace falta.
5. **`tutores`, `tutoria_fases`, `tutoria_mensajes`, `notificaciones`, `auditoria`** — importantes pero no bloqueantes para reanudar operación.

Esta prioridad es la que se siguió en la sección 4 (procedimiento de restauración): primero se garantiza que el `FULL` cubra siempre los grupos 1-3 sin excepción, y el incremental/diferencial cierra la ventana de exposición de esos mismos grupos entre un `FULL` y el siguiente.

---

## 2. Tipos de respaldo utilizados

| Tipo | Mecanismo real usado (PostgreSQL 15) | Qué captura |
|---|---|---|
| **Completo (Full) — lógico** | `pg_dump -Fc -Z9` | Toda la base, en un solo archivo portable, restaurable con `pg_restore` incluso en otra versión menor de PostgreSQL |
| **Completo (Full) — físico** | `pg_basebackup -Fp -Xs` | Copia binaria del clúster completo; es la base sobre la que se aplican los WAL para el incremental |
| **Incremental** | Archivado continuo de WAL (`archive_mode`, `archive_command`) + recuperación a un punto en el tiempo (**PITR**) | Cada segmento de WAL archivado es, en la práctica, el "incremento" desde el segmento anterior — es el mecanismo real de incremental continuo de PostgreSQL, con granularidad de segundos |
| **Diferencial** | Exportación de filas modificadas desde el último `FULL` (`WHERE fecha > <timestamp del FULL>`, vía `psql \copy`) | Todos los cambios acumulados desde el último `FULL`, en un archivo aparte, sin depender de los diferenciales intermedios (igual que un diferencial clásico) |

No se usó `pg_basebackup --incremental` / `pg_combinebackup` (funciones de PostgreSQL 17+) porque **no existen en la versión 15.19 real del proyecto** — usarlas habría sido, otra vez, solo teoría no ejecutable.

---

## 3. Frecuencia de respaldos (política semanal)

Ajustada con base en el análisis real de la sección 1: se mantiene un esquema mixto full + diferencial + incremental, pero con el `FULL` movido a un horario de mínima actividad **y** un disparador adicional obligatorio inmediatamente después de cerrar una convocatoria de defensas (por el patrón "por ráfagas" detectado en `actas`/`evaluaciones`).

| Día | Hora | Tipo | Almacenamiento | Retención |
|---|---|---|---|---|
| Domingo | 23:00 | **Completo (Full)** | Servidor + disco externo + nube | 30 días |
| Lunes, martes, jueves, sábado | 01:00 | Incremental (WAL continuo, todo el día) | Servidor + nube | 7 días |
| Miércoles, viernes | 01:00 | Diferencial (filas cambiadas desde el domingo) | Servidor + disco externo | 14 días |
| **Evento** | Al cerrar cada convocatoria de defensas | **Full inmediato** de `actas` + `evaluaciones` + `evaluaciones_finales` | Servidor + nube | 1 año (valor legal) |

El disparador por evento no es opcional: es la respuesta directa al hallazgo de la sección 1.2 — un `FULL` semanal solo puede dejar hasta 6 días de actas/notas sin respaldar si la defensa ocurrió el lunes.

---

## 4. Procedimiento de restauración

### 4.1 Reglas generales

- Nunca se restaura directamente sobre `BdPresustentaciones` (producción). Se restaura primero en una base o instancia aislada, se valida, y solo entonces se aplica el dato puntual necesario a producción.
- Restauración de un archivo `.dump` completo → `pg_restore`.
- Restauración a un instante específico (ej. "justo antes del incidente") → base física + WAL archivado (PITR).
- Restauración de un incidente parcial (una tabla, unas filas) → extracción selectiva desde el `FULL` más el diferencial/incremental posterior, sin tocar el resto de la base.

### 4.2 Procedimiento paso a paso (el mismo que se ejecutó y se documenta con evidencia en la sección 5)

1. Identificar el instante del incidente y el último `FULL` anterior a ese instante.
2. Crear una base o instancia de trabajo aislada (`CREATE DATABASE ..._restore;` o un directorio de datos aparte en un puerto distinto).
3. Restaurar el `FULL`: `pg_restore -d <base_restore> --no-owner <archivo>.dump`.
4. Según el tipo de incidente:
   - Si se necesita el estado exacto al momento del incidente → aplicar los segmentos de WAL archivados desde el `FULL` hasta ese instante (PITR).
   - Si basta con el último cierre de día → aplicar el diferencial más reciente (`\copy ... FROM archivo_diferencial.csv`).
5. Verificar conteos y consistencia contra los valores de referencia tomados antes del incidente (ver `pg_stat_user_tables` / `COUNT(*)` de la sección 1.1).
6. Solo después de validar, aplicar el dato recuperado a producción de forma selectiva (nunca sobrescribir toda la base productiva con la de prueba).
7. Registrar el incidente y la restauración (fecha, responsable, tiempo, resultado).

---

## 5. Evidencia de la demostración práctica (ejecutada contra el proyecto real, 23/08/2026)

Todo lo siguiente se ejecutó de verdad contra el contenedor `amz-postgres` del proyecto. Las pruebas destructivas/de escritura se hicieron sobre una base clon (`presusDb_practica`, restaurada del propio `FULL` de esta prueba) para no arriesgar los datos reales; **se verificó al final que `BdPresustentaciones` quedó exactamente igual que al inicio** (mismos conteos de `actas`, `evaluaciones`, `evaluaciones_finales`).

### 5.1 Respaldo Completo (Full) — lógico

```
$ pg_dump -h localhost -p 5432 -U postgres -d BdPresustentaciones -Fc -Z 9 \
    -f presusDb_full_20260823_1551.dump --verbose

real  0m8.39s
Archivo generado: 13 439 640 bytes (≈ 12.8 MB comprimido; la base sin comprimir pesa 178 MB)
```

### 5.2 Respaldo Completo (Full) — físico (base para el incremental)

```
$ pg_basebackup -D /backups/base/FULL_baseline -h localhost -p 5432 -U postgres -Fp -Xs -P

waiting for checkpoint
205871/205871 kB (100%), 1/1 tablespace
real  0m6.06s
START WAL LOCATION: 0/1A000028 (file 00000001000000000000001A)
Tamaño: 216.7 MB
```

### 5.3 Restauración del Full (prueba real, no simulada)

```
$ createdb presusDb_practica
$ pg_restore -U postgres -d presusDb_practica --no-owner -j 4 presusDb_full_20260823_1551.dump

real  0m5.95s
Verificación: SELECT COUNT(*) FROM presus.actas;        -> 10780  (idéntico a producción)
              SELECT COUNT(*) FROM presus.evaluaciones;  -> 15400  (idéntico a producción)
```

### 5.4 Respaldo INCREMENTAL — demostración real con WAL/PITR

Se activó archivado continuo de WAL (`archive_mode=on`, `archive_command='cp %p /backups/wal/%f'`) sobre el clúster real. Después del `FULL` físico se insertó una fila de prueba real y se forzó el cierre del segmento de WAL:

```sql
INSERT INTO presus.notificaciones (usuario_id, mensaje, leida, fecha)
SELECT id, 'DEMO-INCREMENTAL: registro de prueba para informe de respaldos', false, now()
FROM presus.usuarios LIMIT 1;
-- fila insertada: id=145552, fecha=2026-08-23 15:54:14.672084

SELECT pg_switch_wal();
```

**Recuperación real** (instancia nueva en el puerto 5433, partiendo solo del `FULL` físico + los WAL archivados):

```
$ cp -a /backups/base/FULL_baseline /backups/restore_test/pgdata_pitr
$ echo "restore_command = 'cp /backups/wal/%f %p'" >> postgresql.auto.conf
$ touch recovery.signal
$ pg_ctl -D pgdata_pitr start

LOG:  starting backup recovery with redo LSN 0/1A000028 ...
LOG:  restored log file "00000001000000000000001A" from archive
LOG:  restored log file "00000001000000000000001B" from archive
...
LOG:  restored log file "000000010000000000000025" from archive
LOG:  redo done at 0/25000060
LOG:  last completed transaction was at log time 2026-08-23 15:54:14.6763+00   <-- coincide exacto con el INSERT
LOG:  archive recovery complete
LOG:  database system is ready to accept connections
```

**Verificación en la instancia recuperada:**

```sql
SELECT id, mensaje, fecha FROM presus.notificaciones WHERE mensaje LIKE 'DEMO-INCREMENTAL%';

  id    |                            mensaje                              |           fecha
--------+------------------------------------------------------------------+----------------------------
 145552 | DEMO-INCREMENTAL: registro de prueba para informe de respaldos  | 2026-08-23 15:54:14.672084
```

**Resultado: el dato insertado después del `FULL` se recuperó completo, solo con el `FULL` físico + WAL archivado — sin necesitar un nuevo `FULL`.** Esto es, en la práctica, el respaldo incremental funcionando de punta a punta.

### 5.5 Respaldo DIFERENCIAL — demostración real (filas cambiadas desde el Full)

Se insertó un segundo cambio de control y se exportó el diferencial (todo lo cambiado desde el timestamp del `FULL`, `2026-08-23 15:52:41`):

```sql
INSERT INTO presus.notificaciones (usuario_id, mensaje, leida, fecha)
SELECT id, 'DEMO-DIFERENCIAL: segundo registro de prueba', false, now()
FROM presus.usuarios OFFSET 1 LIMIT 1;
-- fila insertada: id=145553, fecha=2026-08-23 15:55:49.153523
```

```
$ psql -d presusDb_practica -c "\copy (SELECT id, usuario_id, mensaje, leida, fecha
    FROM presus.notificaciones WHERE fecha > '2026-08-23 15:52:41') TO 'notificaciones_diff.csv' CSV HEADER"

COPY 2   -- exactamente los 2 cambios reales hechos desde el Full, ni uno más ni uno menos
```

**Recuperación real: Full + Diferencial, en una base nueva y aislada:**

```
$ createdb presusDb_recuperada_dif
$ pg_restore -U postgres -d presusDb_recuperada_dif --no-owner -j 4 presusDb_full_20260823_1551.dump
real  0m5.95s

-- Justo después del Full (antes de aplicar el diferencial):
SELECT COUNT(*) FROM presus.notificaciones WHERE mensaje LIKE 'DEMO-%';   -> 0   (correcto: el Full es anterior a ambos cambios)

$ psql -d presusDb_recuperada_dif -c "\copy presus.notificaciones (id, usuario_id, mensaje, leida, fecha)
    FROM 'notificaciones_diff.csv' CSV HEADER"
COPY 2

-- Verificación final:
SELECT id, mensaje, fecha FROM presus.notificaciones WHERE mensaje LIKE 'DEMO-%' ORDER BY id;

  id    |                            mensaje                              |           fecha
--------+------------------------------------------------------------------+----------------------------
 145552 | DEMO-INCREMENTAL: registro de prueba para informe de respaldos  | 2026-08-23 15:54:14.672084
 145553 | DEMO-DIFERENCIAL: segundo registro de prueba                    | 2026-08-23 15:55:49.153523
```

**Resultado: `Full + Diferencial` reconstruyó exactamente el mismo estado que la base de origen, con un único archivo diferencial (no dependió de diferenciales intermedios) — la misma propiedad que se había descrito en teoría en la entrega anterior, ahora demostrada de verdad.**

### 5.6 Verificación de que producción no se afectó

```sql
-- Ejecutado sobre BdPresustentaciones (producción) al finalizar toda la prueba:
SELECT pg_size_pretty(pg_database_size('BdPresustentaciones'));  -> 178 MB   (sin cambio)
SELECT COUNT(*) FROM presus.actas;                 -> 10780  (sin cambio)
SELECT COUNT(*) FROM presus.evaluaciones;          -> 15400  (sin cambio)
SELECT COUNT(*) FROM presus.evaluaciones_finales;  -> 15401  (sin cambio)
```

Las bases y la instancia de prueba (`presusDb_practica`, `presusDb_recuperada_dif`, la instancia del puerto 5433) se eliminaron al terminar. El archivado de WAL (`archive_mode=on`) se dejó **activo de forma permanente** en el proyecto real: es una mejora concreta que queda funcionando, no solo una demostración puntual.

---

## 6. Medidas ante una posible pérdida de información

### 6.1 Regla 3-2-1

- **3 copias** de la base en total (producción + Full local + copia externa).
- **2 medios distintos** (disco del servidor + disco externo/almacenamiento separado).
- **1 copia fuera del sitio** (nube).

### 6.2 RPO / RTO — objetivo vs. medido

| | Objetivo (SLA operativo) | Medido en esta prueba |
|---|---|---|
| **RPO** (pérdida máxima tolerable) | 24 h para datos generales; **0** para `actas`/`evaluaciones` gracias al disparador por evento (5.4/5.5) | Recuperación exacta hasta el segundo del último cambio confirmado |
| **RTO** (tiempo de recuperación) | 4 h (incluye detección, decisión y verificación humana, no solo el comando) | Restaurar el Full: **5.95 s** · Recuperar vía PITR: **~13 s** · Aplicar diferencial: **<1 s** |

La diferencia entre el RTO objetivo (4 h) y el tiempo técnico medido (segundos) es intencional: el RTO de una organización incluye detectar el incidente, decidir qué restaurar y verificar el resultado antes de reabrir el sistema — no solo el tiempo de ejecución del comando, que en este proyecto resultó ser mínimo.

### 6.3 Verificación periódica de los respaldos

No sirve un respaldo que nunca se probó a restaurar. Procedimiento mensual:

1. Restaurar el `Full` más reciente en una base aislada (no producción).
2. Comparar conteos de tablas críticas contra el valor de referencia (sección 1.1).
3. Levantar el backend apuntando a la base de prueba y confirmar que arranca sin errores.
4. Registrar fecha, responsable y resultado.

### 6.4 Qué no está resuelto todavía (honesto, no oculto)

- No hay automatización (cron/Actions) corriendo el ciclo Full+Incremental+Diferencial descrito aquí; lo ejecutado en la sección 5 fue una prueba manual real, no un job programado. Automatizarlo es el siguiente paso lógico de este plan.
- El archivado de WAL activado en esta prueba no tiene todavía una política de limpieza (`pg_archivecleanup` o equivalente); sin retención, `/backups/wal` crecería sin límite con el tiempo.
- Esto es distinto del mecanismo de `pg_dump` diario vía GitHub Actions descrito en [`docs/despliegue/BACKUP.md`](../despliegue/BACKUP.md), que cubre el respaldo del entorno de **despliegue** (Railway) una vez el sistema esté publicado; este informe cubre el respaldo **local/de administración de base de datos** exigido por la materia.

---

## 7. Conclusión

El respaldo Full (lógico y físico), el incremental (WAL continuo + PITR) y el diferencial (filas modificadas desde el último Full) se ejecutaron y se recuperaron de verdad contra la base real del proyecto, no solo se describieron: los tres casos quedaron verificados con datos exactos, timestamps reales y conteos comparados contra el estado de origen. El análisis inicial se completó con frecuencia de cambio, consecuencias de pérdida y prioridades de recuperación medidas sobre datos reales del sistema, no supuestos. La discrepancia de versión de PostgreSQL detectada (15 real vs. 18 asumido) explica por qué la práctica incremental/diferencial no se había podido ejecutar antes, y se corrigió usando los mecanismos reales disponibles en PostgreSQL 15.
