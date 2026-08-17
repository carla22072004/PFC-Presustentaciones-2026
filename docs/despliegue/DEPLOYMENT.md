# DEPLOYMENT.md — Despliegue en producción (criterio P5)

## ⚠️ Estado real a la fecha de este documento (2026-08-17)

**El sistema todavía NO está desplegado públicamente.** Este documento deja lista la configuración,
probada localmente, para que el despliegue sea un procedimiento corto y reproducible — pero la creación
de la cuenta en el proveedor y el clic de "Deploy" son acciones que requieren la identidad real de un
integrante del equipo (correo, y eventualmente una tarjeta si se supera el trial gratuito) y **no se
pueden generar de forma automatizada**. Ver el estado real actualizado en la sección final de este
archivo antes de la defensa.

**No se declara ninguna URL pública en el `README.md` hasta que exista de verdad y responda con HTTPS
válido** — declarar una URL que no responde el día de la evaluación reprueba automáticamente el
criterio P5 sin importar el resto de la documentación, así que es preferible no declarar nada hasta que
esté confirmado.

## Proveedor elegido: Railway

**Por qué Railway y no las otras opciones permitidas por la guía** (investigado en vivo el 2026-08-17,
no de memoria — estos términos cambian seguido):

| Proveedor | Web service gratis | Postgres gratis | Redis gratis | Tarjeta requerida |
|---|---|---|---|---|
| **Railway** | ✅ Sí (trial 30 días, $5 crédito) | ✅ Sí (plugin, mismo proyecto) | ✅ Sí (plugin, mismo proyecto) | ❌ No para empezar |
| Render | ✅ Sí | 🟡 Expira a los 30 días (duro) | ❌ No en free tier | ❌ No para empezar |
| Fly.io | 🟡 Ya no tiene free tier claro, pago por uso desde el inicio | 🟡 Ídem | 🟡 Ídem | ✅ Sí |
| Oracle Cloud Free Tier | ✅ Sí, gratis para siempre | Autogestionado en la misma VM | Autogestionado en la misma VM | ✅ Sí, para verificación de identidad |

Railway es la única opción que permite correr **backend + Postgres + Redis + frontend en un solo
proyecto**, sin partir el despliegue en 3 plataformas distintas, y sin pedir tarjeta para arrancar —
coincide casi 1:1 con la topología ya probada en `docker-compose.yml`.

**Riesgo real declarado:** el trial gratuito de Railway dura 30 días con $5 de crédito. El criterio de
`BACKUP.md` exige respaldos diarios durante los 30 días posteriores a la defensa — si el despliegue se
hace mucho antes de la defensa, el trial podría agotarse dentro de esa ventana de 30 días. **Mitigación:**
agregar un método de pago al plan Hobby de Railway ($5/mes de crédito incluido) antes de que se agote el
trial; el consumo real de esta app (un solo usuario/tribunal, sin carga sostenida) es mínimo y
probablemente no exceda ese crédito.

## Arquitectura de despliegue

Railway no soporta `docker-compose.yml` directamente: cada servicio del compose se despliega como un
**servicio Railway independiente dentro del mismo proyecto**, comunicándose por red privada
(`*.railway.internal`), replicando la topología real:

```
Proyecto Railway "presustentaciones-uteq"
├── postgres          (plugin oficial de Railway, un clic, sin Dockerfile)
├── redis             (plugin oficial de Railway, un clic, sin Dockerfile)
├── backend           (Dockerfile: backend/Dockerfile, railway.json: backend/railway.json)
│     expone /actuator/health publicamente (SecurityConfig ya lo permite)
└── frontend           (Dockerfile: Frontend/Dockerfile, railway.json: Frontend/railway.json)
      nginx interno hace proxy de /api/v1/ y /actuator/ al servicio backend
      por red privada (Frontend/nginx.railway.conf.template)
      ES EL UNICO SERVICIO QUE NECESITA DOMINIO PUBLICO PARA EL TRIBUNAL
```

Se probó localmente antes de escribir este documento:
- `backend/Dockerfile` compila con `docker build` (verificado 2026-08-17).
- `Frontend/Dockerfile` compila con `docker build` **tras corregir un `package-lock.json` desincronizado**
  que rompía `npm ci` (faltaban 16 paquetes específicos de la plataforma Linux — el lockfile se había
  regenerado solo en Windows en una sesión anterior). Corregido regenerando el lockfile dentro de un
  contenedor `node:20-alpine` para incluir las dependencias opcionales de ambas plataformas.
- `/actuator/health` responde con el detalle de los 4 componentes (`db`, `redis`, `diskSpace`, `ping`)
  todos `UP`, tras habilitar `management.endpoint.health.show-details=always` en
  `application.properties` (antes solo devolvía `{"status":"UP"}` sin desglose).
- El usuario de demostración (`demo@uteq.edu.ec` / ver credenciales en `README.md`) hace login
  correctamente end-to-end.

## Procedimiento paso a paso (a ejecutar por un integrante del equipo)

1. **Crear cuenta en Railway** (https://railway.com) con GitHub OAuth — no requiere tarjeta.
2. **Crear un nuevo proyecto** → "Deploy from GitHub repo" → seleccionar este repositorio.
3. **Agregar el plugin PostgreSQL**: dentro del proyecto, "+ New" → "Database" → "Add PostgreSQL".
   Railway genera automáticamente `DATABASE_URL`, `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`.
4. **Agregar el plugin Redis**: "+ New" → "Database" → "Add Redis". Genera `REDIS_URL`, `REDISHOST`, `REDISPORT`.
5. **Agregar el servicio backend**: "+ New" → "GitHub Repo" → mismo repo → en "Settings" fijar
   **Root Directory = `backend`** (para que use `backend/Dockerfile` y `backend/railway.json`).
   Variables de entorno a configurar en la pestaña "Variables" de este servicio:
   ```
   DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   DB_USERNAME=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   REDIS_HOST=${{Redis.REDISHOST}}
   REDIS_PORT=${{Redis.REDISPORT}}
   JWT_SECRET=<generar con: openssl rand -hex 32>
   JWT_EXPIRATION=3600000
   ```
   Railway resuelve las referencias `${{Postgres.PGHOST}}` automáticamente al desplegar (sintaxis de
   variables compartidas de Railway). Activar "Generate Domain" para tener una URL pública de prueba
   directa del backend (útil para verificar `/actuator/health` independientemente del frontend).
6. **Agregar el servicio frontend**: "+ New" → "GitHub Repo" → mismo repo → Root Directory = `Frontend`.
   Variables de entorno:
   ```
   BACKEND_INTERNAL_URL=${{backend.RAILWAY_PRIVATE_DOMAIN}}:8080
   ```
   Activar "Generate Domain" (o configurar un dominio propio) — **esta es la URL pública que va en el
   README** como acceso del sistema para el tribunal.
7. **Verificar HTTPS válido**: Railway emite certificados TLS automáticamente (Let's Encrypt) para
   cualquier dominio `*.up.railway.app` generado o dominio propio conectado — no requiere configuración
   manual. Confirmar visitando la URL con el navegador y comprobando que no hay advertencias.
8. **Verificar `/actuator/health`**: `curl https://<dominio-backend>.up.railway.app/actuator/health` debe
   devolver `{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},...}}`.
9. **Verificar login del usuario demo**: entrar a la URL pública del frontend, iniciar sesión con las
   credenciales publicadas en `README.md`.
10. **Actualizar `README.md`** con la URL pública real, solo después de confirmar los pasos 7-9.

## Recursos consumidos (estimado — pendiente de confirmar tras el despliegue real)

| Servicio | Recurso estimado | Costo |
|---|---|---|
| backend | ~512 MB RAM, tráfico bajo (defensa + demo, no producción real) | Dentro del trial de $5 |
| frontend (nginx) | ~50 MB RAM, sirve estáticos | Dentro del trial de $5 |
| postgres | Free tier de Railway (compartido) | Incluido |
| redis | Free tier de Railway (compartido) | Incluido |

Estos números son una estimación basada en los límites documentados del plan Free/Hobby de Railway, no
una medición real todavía — se actualizarán aquí con datos reales del dashboard de Railway una vez
desplegado.
