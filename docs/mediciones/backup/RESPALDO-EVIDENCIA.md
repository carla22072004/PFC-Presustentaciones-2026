# RF-61 — Respaldo de la base de datos bajo demanda y programado: evidencia real

**Fecha:** 2026-09-11 15:05–15:07 UTC
**Entorno:** `docker compose up -d --build` (stack local, `PFC-Presustentaciones-2026`), backend sano en `/actuator/health`.
**Cliente `pg_dump` que usa el backend para generar los respaldos** (ejecuta `ProcessBuilder` dentro del contenedor `amz-backend`):

```
$ docker exec amz-backend pg_dump --version
pg_dump (PostgreSQL) 16.15
```

**Motor real (`amz-postgres`):**

```
$ docker exec amz-postgres postgres --version
postgres (PostgreSQL) 15.19
```

Nota honesta: el cliente `pg_dump`/`pg_restore` empaquetado en la imagen del backend es 16.15, más nuevo
que el motor real (15.19) — es forward/backward compatible para dump lógico (`pg_dump` soporta motores
más antiguos), y de hecho todos los `pg_dump`/`pg_restore` de esta evidencia se ejecutaron con éxito
contra el 15.19 real. El único punto donde el desajuste de versión importó fue al intentar leer un dump
con el `pg_restore` 15.19 de `amz-postgres` en vez del 16.15 de `amz-backend` (ver nota en
`RESTAURACION-EVIDENCIA.md`) — el *formato* del dump custom sí es sensible a la versión del *lector*, no
solo del motor de destino.

Todas las peticiones usan el prefijo real `/api/v1/backups` (`CustomWebMvcRegistrations` añade `/api/v1`
a todo `@RestController`, aunque el `@RequestMapping` del controlador declare solo `/api/backups`).

## 1. Autenticación como ADMIN (único rol con `BACKUPS_GESTIONAR`, V27)

```
$ curl -s -X POST http://localhost:4200/api/v1/auth/login -H "Content-Type: application/json" \
    -d '{"email":"admin@uteq.edu.ec","password":"admin123"}'
```
→ `200`, `rol: "ADMIN"`, token JWT capturado para las peticiones siguientes.

## 2. `GET /api/v1/backups` — listado antes de generar nada en esta corrida

```json
{"success":true,"data":[{"nombre":"respaldo_FULL_AUTOMATICO_20260911_032654.dump","tipo":"FULL","origen":"AUTOMATICO","tamanoBytes":14393649,"tamanoLegible":"13.7 MB","fechaCreacion":"2026-09-11T03:26:56.982374998"}]}
```

Ya había un respaldo `FULL AUTOMATICO` (lo generó el propio contenedor al arrancar, antes de esta sesión) —
se deja como estaba, sin editar, como prueba de que el listado refleja el disco real y no un estado
sembrado a mano para la demostración.

## 3. `POST /api/v1/backups` — respaldo FULL bajo demanda

```
$ curl -s -X POST "http://localhost:4200/api/v1/backups?origen=MANUAL" -H "Authorization: Bearer $TOKEN"
```
```json
{"success":true,"data":{"nombre":"respaldo_FULL_MANUAL_20260911_150542.dump","tipo":"FULL","origen":"MANUAL","tamanoBytes":14393649,"tamanoLegible":"13.7 MB","fechaCreacion":"2026-09-11T15:05:44.589992156"},"message":"Respaldo generado correctamente"}
```
Tiempo real (`pg_dump -Fc --compress=6`): **2.66 s**.

## 4. `POST /api/backups/diferencial` — respaldo diferencial

```
$ curl -s -X POST "http://localhost:4200/api/v1/backups/diferencial?origen=MANUAL" -H "Authorization: Bearer $TOKEN"
```
```json
{"success":true,"data":{"nombre":"respaldo_DIFERENCIAL_MANUAL_20260911_150544.tar.gz","tipo":"DIFERENCIAL","origen":"MANUAL","tamanoBytes":1073,"tamanoLegible":"1.0 KB","fechaCreacion":"2026-09-11T15:05:44.902019059"},"message":"Respaldo diferencial generado"}
```
Tiempo real: **0.30 s** (tamaño pequeño porque no hubo cambios en las tablas diferenciadas desde el
último FULL — el `.tar.gz` contiene igual las 8 tablas vacías + `manifest.txt` + `_eliminados.csv`, no un
placeholder).

## 5. `GET /api/v1/backups` — listado después: informa tipo, origen, fecha y tamaño

```json
{"success":true,"data":[
  {"nombre":"respaldo_DIFERENCIAL_MANUAL_20260911_150544.tar.gz","tipo":"DIFERENCIAL","origen":"MANUAL","tamanoBytes":1073,"tamanoLegible":"1.0 KB","fechaCreacion":"2026-09-11T15:05:44.902019059"},
  {"nombre":"respaldo_FULL_MANUAL_20260911_150542.dump","tipo":"FULL","origen":"MANUAL","tamanoBytes":14393649,"tamanoLegible":"13.7 MB","fechaCreacion":"2026-09-11T15:05:44.589992156"},
  {"nombre":"respaldo_FULL_AUTOMATICO_20260911_032654.dump","tipo":"FULL","origen":"AUTOMATICO","tamanoBytes":14393649,"tamanoLegible":"13.7 MB","fechaCreacion":"2026-09-11T03:26:56.982374998"}
]}
```
Cumple el criterio: cada entrada trae `tipo`, `origen`, `fechaCreacion` y `tamanoBytes`/`tamanoLegible`.

## 6. `GET /api/v1/backups/estado`

```json
{"ultimoRespaldo":{"nombre":"respaldo_DIFERENCIAL_MANUAL_20260911_150544.tar.gz", "..."},
 "programacionActiva":true,"proximoAutomatico":"2026-09-13T23:00:00","proximoAutomaticoTexto":"en 2 días",
 "totalRespaldos":3,"conteoPorTipo":{"DIFERENCIAL":1,"FULL":2},"conteoPorOrigen":{"MANUAL":2,"AUTOMATICO":1},
 "espacioUsadoBytes":28788371,"espacioUsadoLegible":"27.5 MB","espacioLibreLegible":"929.95 GB",
 "rpoEstimado":"≈ 7 días","ultimaPruebaRestauracion":null,"ultimaPruebaResultado":"—","ultimaPruebaHace":"nunca"}
```
(`rpoEstimado` en ese momento seguía en "≈ 7 días" porque el cron de FULL todavía era el semanal por
omisión; el cambio de cronograma y su efecto en el RPO se documentan en `RPO-RTO.md`, RNF-24 — aquí solo
se deja constancia del estado que RF-61 exige que el panel informe.)

## 7. `GET /api/v1/backups/{nombre}/descargar`

```
$ curl -s -o respaldo_descargado.dump -D - \
    "http://localhost:4200/api/v1/backups/respaldo_FULL_MANUAL_20260911_150542.dump/descargar" \
    -H "Authorization: Bearer $TOKEN"
HTTP/1.1 200
Content-Type: application/octet-stream
Content-Length: 14393649
Content-Disposition: attachment; filename="respaldo_FULL_MANUAL_20260911_150542.dump"

$ ls -la respaldo_descargado.dump
-rw-r--r-- 1 jeana 197609 14393649  respaldo_descargado.dump
$ file respaldo_descargado.dump
respaldo_descargado.dump: PostgreSQL custom database dump - v1.15-0
```
El archivo descargado es un dump `pg_dump` custom válido y de tamaño idéntico al informado por la API
(14 393 649 bytes) — no un placeholder. Este mismo archivo es el que se usó para la prueba de
restauración real de `RESTAURACION-EVIDENCIA.md` (RF-63).

## 8. Rechazo: nombre que no sigue el formato → `400`

El patrón exigido es `^[A-Za-z0-9._-]+\.(dump|tar\.gz)$` (`BackupService.NOMBRE_VALIDO`).

```
$ curl -s -i "http://localhost:4200/api/v1/backups/nombre-cualquiera/descargar" -H "Authorization: Bearer $TOKEN"
HTTP/1.1 400
{"success":false,"data":null,"message":"Nombre de respaldo inválido.","errors":null,"meta":null}
```

## 9. Rechazo: en ningún caso permite salir del directorio de respaldos (`../`)

Se probó en **tres capas**, de afuera hacia adentro, porque cada una bloquea el intento antes de que la
siguiente llegue a ejercitarse — es la explicación honesta de por qué no se pudo demostrar la ruta `../`
llegando literalmente hasta el `if (nombre.contains(".."))` de `BackupService.resolverExistente()`:

**a) A través de nginx (`http://localhost:4200`, como lo usaría el navegador real):**
```
$ curl -s -i "http://localhost:4200/api/v1/backups/..%2F..%2F..%2Fetc%2Fpasswd/descargar" -H "Authorization: Bearer $TOKEN"
HTTP/1.1 200 OK
Content-Type: text/html
```
nginx normaliza `../` en la URI *antes* de aplicar `location /api/`; la petición ya normalizada deja de
calzar con ese `location` y cae al `location /` (SPA de Angular), que sirve `index.html` — la petición
**nunca llega al backend**. No es un `403`/`400` explícito, pero el efecto es el mismo: cero acceso a
`/etc/passwd` ni a ningún archivo fuera de `uploads/backups`.

**b) Directo contra el contenedor del backend (bypass de nginx, mismo `docker network`), con `../` codificado y sin codificar:**
```
$ docker run --rm --network pfc-presustentaciones-2026_default curlimages/curl:8.10.1 -s -i \
    "http://backend:8080/api/v1/backups/..%2F..%2F..%2Fetc%2Fpasswd/descargar" -H "Authorization: Bearer $TOKEN"
HTTP/1.1 400
<!doctype html>...<h1>HTTP Status 400 – Bad Request</h1>...

$ docker run --rm --network pfc-presustentaciones-2026_default curlimages/curl:8.10.1 -s -i \
    "http://backend:8080/api/v1/backups/%2e%2e%2fapplication.yml/descargar" -H "Authorization: Bearer $TOKEN"
HTTP/1.1 400
<!doctype html>...<h1>HTTP Status 400 – Bad Request</h1>...
```
Esta vez el `400` es genérico de Tomcat (`Content-Type: text/html`, no el `ResponseWrapper` JSON de la
app) — Tomcat rechaza por diseño una secuencia `..` (codificada o no) en el *request target* antes de
que `DispatcherServlet`/Spring Security la vean.

**c) Verificación por inspección del código, para el caso en que un nombre contuviera `..` sin ser un
separador de ruta que ni nginx ni Tomcat interceptaran** (por ejemplo, si llegara ya decodificado por
otro cliente): `BackupService.resolverExistente()` valida, en este orden, (1) el nombre contra
`NOMBRE_VALIDO` — que de por sí ya rechaza cualquier cosa con `/` porque `[A-Za-z0-9._-]+` no lo permite
— y (2) explícitamente `nombre.contains("..")`; y aparte, `dir.resolve(nombre).normalize()` se compara
con `!archivo.getParent().equals(dir)`, así que incluso un nombre que pasara ambos filtros y aun así
resolviera fuera de `uploads/backups` sería rechazado por esa comparación de directorio padre. Tres
controles independientes sobre la misma operación.

**Conclusión honesta:** no se logró que un payload de traversal llegara vivo hasta el código Java para
verlo rechazar en tiempo real (nginx y Tomcat lo impiden antes) — lo cual en sí mismo demuestra el
criterio ("en ningún caso permite salir del directorio de respaldos"), solo que la capa que lo demuestra
no es la que se esperaba inicialmente. El control de la aplicación (b) queda verificado por inspección de
código, no por explotación en vivo.

## 10. Rechazo por falta de permiso: `403` con COORDINADOR

```
$ curl -s -X POST http://localhost:4200/api/v1/auth/login -d '{"email":"demo@uteq.edu.ec","password":"Demo2026!"}'
```
→ `rol: "COORDINADOR"` (V27 solo asigna `BACKUPS_GESTIONAR` al rol 1 = ADMIN).

```
$ curl -s -i "http://localhost:4200/api/v1/backups" -H "Authorization: Bearer $TOKEN_COORDINADOR"
HTTP/1.1 403
$ curl -s -i -X POST "http://localhost:4200/api/v1/backups" -H "Authorization: Bearer $TOKEN_COORDINADOR"
HTTP/1.1 403
$ curl -s -i "http://localhost:4200/api/v1/backups/estado" -H "Authorization: Bearer $TOKEN_COORDINADOR"
HTTP/1.1 403
```
Los tres, `403` — confirma que `@PreAuthorize` a nivel de clase de `BackupController` rechaza a
COORDINADOR igual que a cualquier rol sin `BACKUPS_GESTIONAR`.

## Resumen

| Criterio de RF-61 | Resultado |
|---|---|
| `POST /api/v1/backups` genera FULL bajo demanda | ✅ real, 2.66 s |
| `POST /api/v1/backups/diferencial` genera diferencial | ✅ real, 0.30 s |
| `GET /api/v1/backups` informa tipo/origen/fecha/tamaño | ✅ |
| `GET /api/v1/backups/{nombre}/descargar` | ✅ dump válido, tamaño exacto |
| `GET /api/v1/backups/estado` | ✅ |
| Nombre con formato inválido → `400` | ✅ |
| Ningún caso permite salir del directorio de respaldos | ✅ (3 capas: nginx, Tomcat, código — ver §9) |
| Sin `BACKUPS_GESTIONAR` (COORDINADOR) → `403` | ✅ |

No se verificó en esta corrida el criterio del SRS "si el respaldo termina sin error pero produce un
archivo vacío, responde `500`" — forzar un `pg_dump` que termine en 0 sin error no es reproducible sin
manipular el binario; queda como verificación por inspección del código (`BackupService.generar()`,
`if (!Files.isRegularFile(destino) || tamano(destino) == 0L) throw ...`), no por demostración en vivo.
