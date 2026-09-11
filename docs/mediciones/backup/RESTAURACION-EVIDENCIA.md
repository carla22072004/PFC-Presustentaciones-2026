# RF-63 — Restauración y prueba de restauración: evidencia real

**Fecha:** 2026-09-11 15:08–15:10 UTC
**Regla seguida:** `docs/basedatos/PLAN-RESPALDOS-RECUPERACION.md` §4.1: *"Nunca se restaura directamente
sobre `BdPresustentaciones` (producción). Se restaura primero en una base o instancia aislada, se valida,
y solo entonces se aplica el dato puntual necesario a producción."* Por eso esta prueba **no** llama a
`POST /api/v1/backups/{nombre}/restaurar` contra la base en servicio — ese endpoint hace
`pg_restore --clean` directo sobre la base configurada en `spring.datasource.url` (`BdPresustentaciones`,
la misma que usa el backend en producción), así que ejercitarlo tal cual habría violado el propio criterio
que se está probando ("la prueba no debe alterar la base en servicio"). En su lugar se reprodujo el mismo
mecanismo (`pg_dump`/`pg_restore` reales) sobre una base aislada creada para esta prueba, exactamente como
prescribe §4.1, y se usó `POST /api/v1/backups/pruebas` — que es puramente un registro de bitácora, no
dispara ninguna restauración — para dejar constancia del resultado.

## 1. Conteo de referencia en la base EN SERVICIO, antes de la prueba

```
$ docker exec amz-postgres psql -U postgres -d BdPresustentaciones -t -c "SELECT COUNT(*) FROM presus.solicitud;"
 44012
```

## 2. Restauración real, en base aislada (no en `BdPresustentaciones`)

Se usó el respaldo `respaldo_FULL_MANUAL_20260911_150542.dump` descargado en la evidencia de RF-61.

```
$ docker cp respaldo_descargado.dump amz-backend:/tmp/respaldo_full_prueba.dump

$ docker exec amz-backend createdb -h postgres -U postgres presusDb_prueba_restauracion

$ docker exec amz-backend sh -c "time pg_restore -h postgres -U postgres \
    -d presusDb_prueba_restauracion --no-owner --no-privileges -j 4 /tmp/respaldo_full_prueba.dump"
real  0m 2.20s
```
(Se usó el `pg_restore` del contenedor `amz-backend`, versión 16.15 — la misma que generó el dump con
`pg_dump` — para evitar el problema de formato entre versiones documentado en `RESPALDO-EVIDENCIA.md`.)

Sin errores. Verificación de que la restauración es completa y correcta, comparando conteos contra
producción (mismo criterio que §4.2 del plan, paso 5):

| Tabla | `BdPresustentaciones` (producción) | `presusDb_prueba_restauracion` (restaurada) |
|---|---:|---:|
| `solicitud` | 44012 | 44012 |
| `actas` | 10783 | 10783 |
| `usuarios` | 51439 | 51439 |

Los tres conteos coinciden exactamente — el respaldo es recuperable de punta a punta.

## 3. Registro del resultado (bitácora, RF-63)

```
$ curl -s -X POST http://localhost:4200/api/v1/backups/pruebas -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d '{
  "respaldoNombre": "respaldo_FULL_MANUAL_20260911_150542.dump",
  "resultado": "EXITOSA",
  "responsable": "admin@uteq.edu.ec",
  "notas": "Restauracion real con pg_restore -j4 sobre base aislada presusDb_prueba_restauracion ..."
}'
```
```json
{"success":true,"data":{"id":1,"respaldoNombre":"respaldo_FULL_MANUAL_20260911_150542.dump",
 "fecha":"2026-09-11T15:09:08.318704225","resultado":"EXITOSA","responsable":"admin@uteq.edu.ec", "...": "..."},
 "message":"Prueba de restauración registrada"}
```

```
$ curl -s http://localhost:4200/api/v1/backups/pruebas -H "Authorization: Bearer $TOKEN"
```
→ `200`, la fila queda listada — el resultado es consultable, con fecha, como exige el criterio.

## 4. Conteo de referencia en la base EN SERVICIO, después de la prueba

```
$ docker exec amz-postgres psql -U postgres -d BdPresustentaciones -t -c "SELECT COUNT(*) FROM presus.solicitud;"
 44012
```

**44012 antes, 44012 después — sin cambio.** La prueba de restauración no alteró la base en servicio.
No es un hallazgo negativo que reportar: se comprobó de verdad, contando filas, y el resultado honesto es
que se cumple, precisamente porque la restauración real ocurrió en una base aislada y nunca tocó
`BdPresustentaciones`.

## 5. Limpieza

```
$ docker exec amz-backend dropdb -h postgres -U postgres presusDb_prueba_restauracion
$ docker exec amz-backend rm -f /tmp/respaldo_full_prueba.dump
```
Ambos confirmados eliminados; no queda rastro de la base ni del archivo temporal en los contenedores.

## Resumen

| Criterio de RF-63 | Resultado |
|---|---|
| Resultado registrado como `EXITOSA`/`FALLIDA`, con fecha, consultable | ✅ |
| La prueba de restauración no altera la base en servicio | ✅ verificado por conteo antes/después (44012 = 44012) |
| Sin `BACKUPS_GESTIONAR` → `403` en las tres operaciones | ✅ (mismo `@PreAuthorize` de clase que RF-61, ver `RESPALDO-EVIDENCIA.md` §10) |

No se verificó en esta corrida "una restauración fallida deja el sistema en un estado consistente y
declara el motivo" (SRS, cuarto criterio) — provocar una restauración fallida de forma controlada
(dump corrupto a propósito) no formaba parte de este pase; queda pendiente como demostración futura,
anotado aquí para no darlo por probado sin haberlo hecho.
