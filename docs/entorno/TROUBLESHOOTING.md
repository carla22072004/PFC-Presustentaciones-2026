# Solución de problemas — `make all` en Windows

Este documento detalla causas reales encontradas y corregidas al verificar `make all` en un entorno
Windows real (Git Bash / MINGW64), separadas del `README.md` para no saturar la guía de arranque rápido.
Todas fueron reproducidas, diagnosticadas con causa raíz confirmada, y corregidas o documentadas con su
workaround exacto — ninguna es un bug de lógica del `Makefile`, de los scripts, ni de la aplicación.

## 1. `make audit` falla con "No such file or directory" si la ruta tiene espacios

**Síntoma:** `bash: D:\...\scripts\...: No such file or directory` al correr `make audit`.

**Causa raíz** (confirmada con `make -d audit`): el `make.exe` nativo de Windows resuelve el shebang
(`#!/usr/bin/env bash`) de los scripts armando él mismo un `CreateProcess("env bash <ruta>")` **sin
comillas** — si la ruta del repositorio contiene espacios (p. ej. `.../6. Quinto y Sexto Semestre.../...`),
Windows corta el comando en el primer espacio.

**No hay fix posible dentro del `Makefile`** — es una limitación del binario de `make`, no del proyecto.
**Solución:** clonar o copiar el repositorio a una ruta sin espacios (p. ej. `C:\ProyectoWebFinal\...`).
Verificado: `make audit` corre limpio (exit 0, 0 hallazgos de SQL dinámico) una vez movido.

## 2. `password authentication failed` con un PostgreSQL nativo de Windows adicional

**Síntoma:** `make test`/`make all` fallan con `FATAL: password authentication failed for user "postgres"`,
aunque Docker Compose esté corriendo sano.

**Causa raíz:** un PostgreSQL instalado como servicio nativo de Windows (verificable con
`Get-Service | Where-Object Name -like "*postgres*"`) compite por el puerto 5432 con el contenedor de
Docker — la conexión llega al Postgres equivocado, con credenciales distintas.

**Solución:** detener el servicio nativo (PowerShell como administrador):
```powershell
Stop-Service -Name "<nombre-del-servicio>" -Force
# Para reactivarlo despues: Start-Service -Name "<nombre-del-servicio>"
```

## 3. `password authentication failed` incluso con el puerto libre

**Síntoma:** con el conflicto de puerto ya resuelto, `make test` seguía fallando con el mismo error.

**Causa raíz:** `make test` corría `./mvnw test` en crudo, sin cargar `.env` — así que
`spring.datasource.password` caía al valor por defecto hardcodeado en `application.properties`
(`postgresAdmin`), que nunca coincide con la contraseña real que usa Docker Compose (el `DB_PASSWORD`
real vive solo en `.env`).

**Corregido en el [`Makefile`](../../Makefile):** el target `test` ahora carga `.env` automáticamente
antes de correr `./mvnw test`, igual que ya hace Docker Compose — no es un workaround manual, corre así
para cualquiera que clone el repositorio.

## Resultado tras las tres correcciones

`make all` verificado con éxito de punta a punta (2026-08-31, exit 0, sin overrides manuales): build,
contenedores, migraciones Flyway, 109/109 tests, k6 (0 % de fallos), auditoría (0 hallazgos SQL dinámico),
trazabilidad (8/8 Must) y PDF final, todo en una sola corrida real.

## Hallazgo real de reproducibilidad encontrado en el camino (no relacionado con Windows)

Al convertir `PreSustentacionesApplicationTests` de un `assertTrue(true)` a un `@SpringBootTest` real
(ver [`README.md`](../../README.md) y [`DATA-PROVENANCE.md`](../mediciones/DATA-PROVENANCE.md)), la
verificación contra una base de datos genuinamente vacía encontró un bug real preexistente en
`V1__schema_inicial.sql`: dos secuencias (`roles_usuario_seq`, `modalidades_titulacion_seq`) se creaban
sin cualificar el esquema, mientras que migraciones posteriores (`V14`, `V17`) sí las referenciaban
cualificadas (`presus.roles_usuario_seq`) — una migración limpia desde cero fallaba. Nunca se había
detectado porque ningún entorno de desarrollo había migrado nunca desde una base de datos realmente
vacía. Corregido en `V1` y verificado dos veces: migración limpia real y suite completa (109/109) contra
la base de datos de desarrollo ya migrada.
