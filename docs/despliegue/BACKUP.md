# BACKUP.md — Estrategia de respaldo

## ⚠️ Estado real (2026-08-17)

**No hay respaldos automáticos corriendo todavía** porque el sistema no está desplegado (ver
[`DEPLOYMENT.md`](DEPLOYMENT.md)). El mecanismo descrito abajo está **implementado y listo** en
[`.github/workflows/backup.yml`](../../.github/workflows/backup.yml), pero necesita el secret
`DATABASE_URL_PUBLICA` configurado en GitHub (Settings → Secrets → Actions) apuntando a la instancia real
de Railway antes de poder correr — ese secret solo puede configurarse después del despliegue real.

## Mecanismo

**Herramienta:** `pg_dump` en formato `--custom` (comprimido, restaurable con `pg_restore`), ejecutado
por un workflow de GitHub Actions programado.
**Frecuencia:** diaria, 07:00 UTC (02:00 hora Ecuador) — cron `0 7 * * *` en
[`.github/workflows/backup.yml`](../../.github/workflows/backup.yml).
**Destino:** artefactos de GitHub Actions (almacenamiento incluido en el plan gratuito de GitHub para
repositorios, sin costo adicional ni cuenta nueva que crear).
**Retención:** **90 días** (`retention-days: 90` en el workflow) — cubre de sobra el mínimo de 30 días
posteriores a la defensa que exige el criterio.
**Alcance:** la base de datos completa de PostgreSQL (esquema `presus`), incluyendo tablas transaccionales
y catálogos. Redis **no** se respalda: es una caché derivable (TTL de 5-10 minutos, ver
`RedisConfig.java`), no una fuente de verdad — perderla no pierde datos, solo obliga a recalcular/volver
a pedir a la API externa en la siguiente petición.

## Por qué GitHub Actions y no un backup nativo del proveedor

Se investigó si Railway ofrece respaldos automáticos gestionados para su plugin de PostgreSQL en el plan
gratuito/Hobby: **no los ofrece** — los backups automáticos con retención son una función de planes
pagos superiores en la mayoría de proveedores de BD gestionada, incluido Railway. Usar GitHub Actions
(ya gratuito, ya integrado al repositorio, sin cuenta adicional) es la alternativa más simple que cumple
el requisito sin costo extra.

## Prueba de restauración periódica

**Procedimiento de prueba** (a ejecutar mensualmente mientras el sistema esté en producción, y
obligatoriamente una vez antes de declarar este documento "completo" para la defensa):

1. Descargar el artefacto de respaldo más reciente desde la pestaña "Actions" del repositorio en GitHub.
2. Levantar un Postgres local desechable: `docker run -d --name restore-test -e POSTGRES_PASSWORD=test -p 5555:5432 postgres:15-alpine`.
3. Restaurar: `pg_restore --clean --if-exists -h localhost -p 5555 -U postgres -d postgres backup-YYYY-MM-DD.dump`.
4. Verificar: conectar con `psql` y correr `SELECT count(*) FROM presus.usuarios;` — el conteo debe ser
   mayor a 0 y consistente con lo esperado (al menos el admin y el usuario demo).
5. Destruir el contenedor de prueba: `docker rm -f restore-test`.
6. Registrar el resultado en la tabla de abajo.

### Registro real de pruebas de restauración

| Fecha de la prueba | Respaldo probado | Resultado | Responsable |
|---|---|---|---|
| — | Ninguna prueba real se ha ejecutado todavía (sistema aún no desplegado, workflow sin secret configurado) | — | — |

**Esta tabla debe llenarse con datos reales antes de la defensa** — una fila vacía o con datos
inventados sería exactamente el tipo de fabricación que ya se corrigió varias veces en este repositorio
(SUS, Lighthouse, evidencia de trazabilidad). Si no hay tiempo de ejecutar una prueba real de
restauración antes de la defensa, es preferible dejar esta tabla vacía y explicada, como está ahora, que
inventar una fila.

## Qué NO se respalda y por qué

- **Archivos subidos (PDFs de anteproyectos y actas):** almacenados en el sistema de archivos del
  contenedor `backend`, que es efímero en Railway (se pierde en cada redeploy). **Esta es una limitación
  real y no resuelta** — para producción real (más allá del periodo de evaluación) se necesitaría un
  volumen persistente de Railway o migrar a almacenamiento de objetos (S3-compatible). Documentado aquí
  explícitamente como brecha conocida, no oculta.
- **Logs de aplicación:** no se consideran datos críticos para este alcance; Railway conserva logs
  recientes en su propio dashboard con retención estándar del plan.
