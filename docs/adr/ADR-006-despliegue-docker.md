# ADR-006: Despliegue Reproducible mediante Docker Compose y Anclaje Criptográfico (SHA-256)

**Estado:** Aceptado  
**Fecha:** 2026-07-28  
**Decisores:** Equipo DevOps y Arquitectura UTEQ  

## Contexto
Se debe garantizar que el proyecto se levante en cualquier infraestructura limpia de forma idéntica mediante el comando único `make up`.

## Decisión
Contenerizar todos los componentes (PostgreSQL 15, Redis 7, Backend Java 17, Frontend Nginx) anclando cada imagen base en `docker-compose.yml` utilizando su digest criptográfico exacto `sha256:...` en lugar de etiquetas variables.

## Consecuencias
- **Positivas:** Reproducibilidad determinista al 100%; inmunidad a cambios o actualizaciones no deseadas en registros públicos de Docker Hub; despliegue con comando único `make up`.
- **Negativas:** Obliga a actualizar manualmente los hashes SHA-256 cuando se decida subir de versión una imagen base.

**Corrección de integridad (Fase 8, 2026-08-17):** esta ADR afirmaba desde su redacción original que las
imágenes base ya estaban anclas por digest SHA-256 en `docker-compose.yml`. Verificado contra el archivo
real: **eso era falso** — las tres imágenes (`postgres:15-alpine`, `redis:7-alpine`, `nginx:alpine`) usaban
solo tags mutables, sin ningún digest. Se corrigió implementándolo de verdad en esta misma revisión:
```yaml
image: postgres:15-alpine@sha256:fe0737ba566a2c5b2a28f34433c0a423261900ec17b9bf7ad115e1aae7e57f1b
image: redis:7-alpine@sha256:e7723ff73d963f5cc6d9c4643ea3d989527a402a319239054e9472a7fb9219a2
image: nginx:alpine@sha256:4a73073bd557c65b759505da037898b61f1be6cbcc3c2c3aeac22d2a470c1752
```
Verificado con `docker compose config` tras el cambio (sintaxis válida, se resuelve correctamente). La
imagen del `backend` no se ancla por digest porque se construye desde `backend/Dockerfile` en cada
build (`build:`, no `image:`), no se descarga de un registro — el mecanismo de reproducibilidad ahí es el
código fuente versionado en Git, no un digest de imagen.

## Decisión ampliada: proveedor de despliegue en producción (Fase 8, criterio P5)

**Contexto adicional:** el criterio P5 exige que el sistema esté desplegado públicamente con HTTPS
válido durante la semana de la defensa. `docker-compose.yml` (esta ADR) resuelve la reproducibilidad
*local*, pero no resuelve el hosting público — son decisiones relacionadas pero distintas, y se
documentan juntas aquí porque el proveedor elegido determina cómo se traduce este mismo
`docker-compose.yml` a servicios en la nube.

**Decisión:** desplegar en **Railway** (railway.com), un servicio por cada componente del compose
(`postgres`, `redis`, `backend`, `frontend`) dentro de un mismo proyecto, comunicados por red privada.

**Alternativas consideradas y descartadas** (investigadas en vivo el 2026-08-17, no de memoria):
- **Render:** el plan gratuito de PostgreSQL expira a los 30 días de forma dura (no hay forma de
  extenderlo sin pasar a un plan pago), lo cual entra en conflicto directo con el requisito de
  `BACKUP.md` de mantener el sistema operativo 30 días después de la defensa.
- **Fly.io:** ya no ofrece un tier gratuito claro en su modelo de precios actual; es pago por uso desde
  el arranque, lo que exige tarjeta de crédito antes de poder probar el despliegue.
- **Oracle Cloud Free Tier:** técnicamente la opción más fiel al `docker-compose.yml` actual (una sola
  VM gratuita para siempre podría correrlo sin modificaciones), pero el proceso de verificación de
  identidad con tarjeta de crédito es más largo y propenso a rechazos, y no ofrece HTTPS gratuito
  integrado (requeriría configurar Let's Encrypt manualmente con Certbot).

**Consecuencias de elegir Railway:**
- **Positivas:** no requiere tarjeta para empezar; HTTPS automático (Let's Encrypt gestionado por
  Railway) sin configuración manual; red privada entre servicios evita exponer backend/DB/Redis
  directamente a Internet; el mismo Dockerfile de cada componente sirve tanto para `docker-compose.yml`
  local como para Railway (no se duplica la definición de build).
- **Negativas:** el trial gratuito es de 30 días — pasado ese plazo, mantener el sistema activo durante
  los 30 días post-defensa que exige `BACKUP.md` probablemente requiere agregar un método de pago
  (consumo estimado mínimo, ver `docs/despliegue/DEPLOYMENT.md`). El filesystem del contenedor
  `backend` es efímero en Railway (a diferencia de un volumen Docker local): los PDFs de anteproyectos y
  actas subidos se pierden en cada redeploy — limitación real, documentada en `docs/despliegue/BACKUP.md`,
  no resuelta en el alcance de esta fase.

Procedimiento completo de despliegue, variables de entorno y verificación: [`../despliegue/DEPLOYMENT.md`](../despliegue/DEPLOYMENT.md).
