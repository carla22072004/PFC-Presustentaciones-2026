# RUNBOOK.md — Operación del sistema en producción (Railway)

## Arranque

**Arranque normal (deploy automático):** Railway redepliega automáticamente cada servicio (`backend`,
`frontend`) al detectar un push a la rama conectada (por defecto `main`). No requiere intervención manual.

**Arranque manual de un servicio caído:**
1. Dashboard de Railway → proyecto → seleccionar el servicio (`backend`, `frontend`, `postgres` o `redis`).
2. Pestaña "Deployments" → el último deployment → botón "Redeploy".
3. Verificar salud:
   - Backend: `curl https://<dominio-backend>/actuator/health` → debe responder `{"status":"UP",...}`.
   - Frontend: abrir la URL pública en el navegador → debe cargar la pantalla de login sin errores en consola.

**Arranque local (reproducción del entorno completo para depuración, no para producción):**
```bash
docker compose up -d --build
# o, con Make:
make up
```

## Apagado

**Apagado de un servicio individual (sin perder datos):** Dashboard → servicio → "Settings" → "Remove
Service" **NO** se usa para apagado temporal (elimina el servicio). Para pausar sin perder configuración:
"Settings" → "Sleep" (si el plan lo permite) o simplemente reducir réplicas a 0 en "Settings" → "Scaling".

**Apagado completo del entorno local:**
```bash
docker compose down       # detiene contenedores, conserva volúmenes (datos de Postgres)
docker compose down -v    # detiene Y BORRA volúmenes -- usar solo si se quiere reset total
```

## Rotación de secretos

**JWT_SECRET:**
1. Generar uno nuevo: `openssl rand -hex 32`.
2. Actualizar la variable `JWT_SECRET` en el servicio `backend` de Railway (Variables tab).
3. Railway redespliega automáticamente al guardar la variable.
4. **Efecto:** todos los JWT y refresh tokens emitidos con el secreto anterior dejan de ser válidos
   inmediatamente (los usuarios con sesión activa deben volver a iniciar sesión). Esto es esperado y es
   el propósito de rotar el secreto ante una sospecha de compromiso.

**Credenciales de PostgreSQL/Redis:** gestionadas automáticamente por los plugins de Railway; rotarlas
implica recrear el plugin (Railway no ofrece rotación in-place para sus DB gratuitas) — evaluar el
impacto de tiempo de inactividad antes de hacerlo fuera de una ventana de mantenimiento planificada.

**Password del usuario admin/demo:** cambiar directamente en la base de datos (`UPDATE presus.usuarios
SET password = '<hash BCrypt nuevo>' WHERE email = '...'`) o, preferiblemente, mediante el endpoint de
gestión de usuarios (`PUT /api/v1/usuarios/{id}`) autenticado como ADMIN.

## Rotación de contenedores

Railway reconstruye y reemplaza el contenedor completo en cada deploy (no hay "rotación" manual de
contenedores individual como en un Swarm/K8s) — cada `git push` a la rama conectada genera una imagen
nueva y la sustituye con *zero-downtime deploy* (Railway mantiene el contenedor anterior vivo hasta que
el nuevo pasa el healthcheck definido en `railway.json`).

Para forzar una reconstrucción sin cambios de código (p. ej. para aplicar una actualización de la imagen
base `eclipse-temurin:17-jre-alpine` o `nginx:alpine`): Dashboard → servicio → "Deployments" → "Redeploy"
con la opción "Clear build cache" activada.

## Procedimiento de restauración desde respaldo

Ver la estrategia completa de respaldo en [`BACKUP.md`](BACKUP.md). Procedimiento de restauración:

1. **Identificar el respaldo a restaurar** (ver el índice de respaldos en `BACKUP.md`).
2. **Detener temporalmente el tráfico de escritura** (opcional pero recomendado): pausar el servicio
   `backend` en Railway para evitar escrituras concurrentes durante la restauración.
3. **Restaurar el dump de PostgreSQL:**
   ```bash
   # Contra la instancia de Railway, usando la DATABASE_URL publica temporal
   # (Settings -> Networking -> TCP Proxy en el plugin Postgres de Railway)
   pg_restore --clean --if-exists -d "$DATABASE_URL_PUBLICA" backup-YYYY-MM-DD.dump
   ```
4. **Verificar integridad:** correr `./mvnw test` localmente contra un clon de la BD restaurada, y/o
   `SELECT count(*) FROM presus.usuarios;` y comparar contra el conteo esperado del respaldo.
5. **Reanudar el servicio backend** y verificar `/actuator/health` → `db: UP`.
6. **Registrar el incidente y la restauración** en este mismo archivo (agregar entrada abajo) para
   mantener un historial de operaciones reales, no solo el procedimiento teórico.

### Historial de restauraciones reales

| Fecha | Motivo | Respaldo restaurado | Resultado | Responsable |
|---|---|---|---|---|
| — | Ninguna restauración real se ha ejecutado todavía (sistema aún no desplegado) | — | — | — |
