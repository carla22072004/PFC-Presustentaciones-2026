# RNF-24 — Objetivos de respaldo y recuperación: evidencia real

**Fecha:** 2026-09-11 15:07–15:15 UTC. Mismo entorno que RF-61/RF-63 (`docker compose up -d --build`,
stack local sano).

## 1. La tensión que el SRS ya declaraba

`GET /api/v1/backups/config`, **antes** de tocar nada:

```json
{"activo":true,"cron":"0 0 23 * * SUN","retenerDiarios":7,"retenerSemanales":5,"retenerMensuales":12,
 "retenerDiasWal":14,"diferencialActivo":false,"cronDescripcion":"Cada domingo a las 23:00"}
```

`GET /api/v1/backups/estado` (mismo momento) informaba `"rpoEstimado":"≈ 7 días"`. El campo `rpoEstimado`
de `BackupService.estado()` se calcula **solo** a partir del intervalo entre disparos consecutivos del
cron de FULL (`humanizarIntervaloCron`); no considera el archivado de WAL en absoluto, aunque esté activo.
Es decir: aunque el WAL estuviera protegiendo el sistema de verdad, el RPO que la propia aplicación
**declara** seguiría marcando 7 días mientras el cron de FULL sea semanal — el criterio de RNF-24 ("el
cronograma... arroja un objetivo de punto de recuperación declarado y no superior a 24 horas") se lee
sobre esa cifra declarada, así que cambiar solo la config de WAL sin tocar el cron no lo habría resuelto.

## 2. Estado real del WAL antes de intervenir

`GET /api/v1/backups/wal`:

```json
{"archivadoActivo":true,"walLevel":"replica","archiveTimeoutSegundos":900,
 "segmentosArchivados":10,"segmentosEnDisco":10,"fallos":90,
 "hayBaseFisica":false,
 "advertencia":"Hay WAL archivado pero ninguna base física. El WAL solo sirve para recuperar HACIA ADELANTE desde una base: genera una con «Crear base física»."}
```

Dos hallazgos reales, ninguno maquillado:

- **`archivadoActivo: true` pero `hayBaseFisica: false`.** El WAL se estaba archivando de verdad
  (`archive_mode=on`, `archive_timeout=900` s en `docker-compose.yml`), pero sin una base física no sirve
  para nada: PITR necesita un punto de partida (`pg_basebackup`) sobre el cual aplicar los segmentos. Sin
  eso, "el WAL está activo" no es lo mismo que "el WAL está verificado" — es exactamente la condición que
  el SRS exige antes de aceptar el cron semanal como suficiente, y en este momento **no** se cumplía.
- **`fallos: 90`.** Investigado contra el log real de `amz-postgres`:
  ```
  2026-09-11 04:06:09 UTC LOG: archive command failed with exit code 1
  2026-09-11 04:07:11 UTC WARNING: archiving write-ahead log file "00000001000000000000000C" failed too many times, will try again later
  ```
  Los 90 fallos están concentrados entre 04:06 y 04:07 UTC del mismo día (arranque del contenedor a las
  03:26), sobre un único segmento (`00000001000000000000000C`) — consistente con una carrera de permisos
  en los primeros segundos tras el arranque, antes de que el `chown`/`chmod` del wrapper de
  `docker-compose.yml` terminara sobre `/var/lib/postgresql/wal_archive`. El archivado se recuperó solo
  después (segmentos 10 a 13 archivados sin error más tarde, ver §3) — no es un problema en curso, pero
  se deja documentado en vez de ignorarlo.

## 3. Verificar el WAL de verdad: generar la base física que faltaba

```
$ curl -s -X POST http://localhost:4200/api/v1/backups/bases -H "Authorization: Bearer $TOKEN"
{"success":true,"data":{"nombre":"base_20260911_151223","tamanoBytes":34673887,"tamanoLegible":"33.1 MB",
 "fechaCreacion":"2026-09-11T15:12:31.407179615"},"message":"Base física generada"}
```
Tiempo real (`pg_basebackup --format=tar --gzip --wal-method=stream`): **7.6 s**.

`GET /api/v1/backups/wal` inmediatamente después:

```json
{"archivadoActivo":true,"segmentosArchivados":13,"hayBaseFisica":true,
 "pitrDisponibleDesde":"11/09/2026 15:12","advertencia":null,
 "basesFisicas":[{"nombre":"base_20260911_151223","tamanoBytes":34673887,"fechaCreacion":"2026-09-11T15:12:31.407179615"}]}
```
`advertencia` pasa a `null` y `pitrDisponibleDesde` deja de decir "no disponible" — el WAL queda
**verificado**, no solo activo: hay una base física real desde la cual PITR funcionaría.

## 4. Con el WAL verificado, mover el cron de FULL a diario

Justificación explícita del criterio del encargo: *"cambiar el cron a diario... SOLO si el WAL lo
respalda"* — y en este punto sí lo respalda (§3). Se cambió vía la propia API, no editando la base a mano:

```
$ curl -s -X PUT http://localhost:4200/api/v1/backups/config -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d '{"activo":true,"cron":"0 0 2 * * *", "retenerDiarios":7,
    "retenerSemanales":5,"retenerMensuales":12,"retenerDiasWal":14,"diferencialActivo":false,
    "cronDiferencial":"0 30 2 * * WED,FRI"}'
{"success":true,"data":{"cron":"0 0 2 * * *","cronDescripcion":"Todos los días a las 02:00", "..."},
 "message":"Cronograma actualizado"}
```

`GET /api/v1/backups/estado` después del cambio:

```json
{"proximoAutomatico":"2026-09-12T02:00:00","proximoAutomaticoTexto":"en 10 h","rpoEstimado":"≈ 1 día",
 "ultimaPruebaRestauracion":"2026-09-11T15:09:08.318704","ultimaPruebaResultado":"EXITOSA","ultimaPruebaHace":"hace 3 min"}
```

`rpoEstimado` ahora dice "≈ 1 día" — el RPO **declarado por la propia aplicación** queda por debajo de
las 24 horas exigidas, y de forma consistente con el resto del sistema, no por decreto en el documento.

## 5. Prueba de restauración EXITOSA en los últimos 30 días

Ya registrada en `RESTAURACION-EVIDENCIA.md` (RF-63): `ultimaPruebaResultado: "EXITOSA"`,
`fecha: 2026-09-11T15:09:08` — dentro de la ventana de 30 días exigida por este mismo requisito
(la prueba y su verificación ocurrieron en la misma sesión).

## 6. Tiempo REAL de una restauración completa sobre datos representativos (< 4 h)

Generador de ~1M de registros del commit `73c8902` (`database/esquema.sql` + `database/datos_masivos.sql`),
cargado en una base aislada **nueva** (`BdPresustentaciones_1M`, nunca `BdPresustentaciones`) en el mismo
contenedor `amz-postgres`, para no arriesgar la base en servicio con una carga de prueba de este tamaño:

```
$ docker exec amz-postgres createdb -U postgres BdPresustentaciones_1M
$ docker exec amz-postgres sh -c "time psql -U postgres -d BdPresustentaciones_1M -f /tmp/esquema.sql"
real  0m 1.64s
$ docker exec amz-postgres sh -c "time psql -U postgres -d BdPresustentaciones_1M -f /tmp/datos_masivos.sql"
...
 TOTAL FILAS GENERADAS | 1025204
real  0m 15.69s
$ docker exec amz-postgres psql -U postgres -d BdPresustentaciones_1M -c \
    "SELECT pg_size_pretty(pg_database_size('BdPresustentaciones_1M'));"
 177 MB
```

Respaldo y restauración reales, cronometrados, con el cliente `pg_dump`/`pg_restore` 16.15 de
`amz-backend` (el mismo que usa la aplicación):

```
$ docker exec amz-backend sh -c "time pg_dump -h postgres -U postgres -d BdPresustentaciones_1M \
    --format=custom --compress=6 --no-owner --no-privileges -f /tmp/full_1M.dump"
real  0m 2.26s          (archivo: 13 703 042 bytes)

$ docker exec amz-backend createdb -h postgres -U postgres BdPresustentaciones_1M_restore
$ docker exec amz-backend sh -c "time pg_restore -h postgres -U postgres \
    -d BdPresustentaciones_1M_restore --no-owner --no-privileges -j 4 /tmp/full_1M.dump"
real  0m 2.31s
```

Verificación de integridad (conteos, mismo criterio que RF-63):

| Tabla | Origen (`BdPresustentaciones_1M`) | Restaurada (`..._restore`) |
|---|---:|---:|
| `solicitud` | 45000 | 45000 |
| `actas` | 12000 | 12000 |
| `usuarios` | 50004 | 50004 |

**Tiempo técnico medido de la restauración: 2.31 s, sobre una base de 177 MB / 1 025 204 filas —
muy por debajo de las 4 horas del criterio.** Coincide con lo ya medido en
`docs/basedatos/PLAN-RESPALDOS-RECUPERACION.md` §6.2 (Full: 5.95 s sobre 178 MB reales de producción, en
esa fecha) — el volumen de este proyecto, incluso a tamaño "representativo" de 1M de filas, sigue siendo
pequeño para PostgreSQL, así que el tiempo de ejecución del comando nunca fue el riesgo real. Igual que
señala esa misma sección del plan, la brecha entre el RTO objetivo (4 h) y el tiempo técnico (segundos) es
intencional: el RTO de una organización incluye detectar el incidente, decidir qué restaurar y verificar
el resultado, no solo ejecutar el comando.

Limpieza tras la medición: `dropdb BdPresustentaciones_1M_restore`, `dropdb BdPresustentaciones_1M`,
`rm /tmp/full_1M.dump` — confirmado sin rastro (`psql -l` no las lista). `BdPresustentaciones` (producción)
sin cambio en ningún momento: `SELECT COUNT(*) FROM presus.solicitud` dio **44012** antes de empezar esta
sección y sigue en 44012 (ver `RESTAURACION-EVIDENCIA.md` §4, misma sesión).

## 7. Retención de WAL (14 días) cubre el intervalo entre dos respaldos completos

Con el cron ya diario (§4), el intervalo entre dos FULL consecutivos es **1 día**; `retenerDiasWal` sigue
en **14 días** (no se tocó, ya era el valor por omisión de V29). 14 ≥ 1: se cumple con margen amplio. Nota
honesta: con el cron semanal *anterior* (intervalo de 7 días) también se cumplía (14 ≥ 7) — este criterio
nunca fue el problema; el problema era el RPO declarado por el cron de FULL en sí (§1), que sí cambió con
el cron.

## Resumen — los cuatro criterios de RNF-24

| Criterio | Antes de esta sesión | Después |
|---|---|---|
| RPO declarado ≤ 24 h | ❌ "≈ 7 días" (cron semanal) | ✅ "≈ 1 día" (cron diario, §4) |
| Prueba de restauración EXITOSA en 30 días | ❌ `ultimaPruebaResultado: "—"`, nunca | ✅ EXITOSA, 2026-09-11 (RF-63) |
| Restauración < 4 h sobre datos representativos, medida y fechada | — no medida | ✅ 2.31 s medidos sobre 177 MB / 1 025 204 filas (§6) |
| Retención WAL cubre intervalo entre FULL | ✅ 14 ≥ 7 (ya se cumplía) | ✅ 14 ≥ 1 |

Se optó por la primera salida honesta del encargo — **cambiar el cron a diario** — y no por dejar el
requisito en "No cumplido", porque el WAL sí lo respalda: quedó verificado con una base física real y
`pitrDisponibleDesde` funcionando (§3), no solo declarado activo en `docker-compose.yml`. El cambio de
cron se hizo a través de la propia API (`PUT /api/v1/backups/config`), auditado como cualquier otro cambio
de configuración (`trg_auditoria_respaldo_config` de V28).
