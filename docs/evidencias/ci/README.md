# Evidencia de corridas verdes de integración continua

**Repositorio:** `carla22072004/PFC-Presustentaciones-2026`  
**Workflow:** `.github/workflows/ci.yml`  
**Fecha de captura:** 2026-09-05

La guía de la Entrega Final pide *capturas* de tres corridas verdes de CI. En vez de una
imagen —que no se puede volver a verificar sin confiar en quien la tomó— esta evidencia se
captura desde la **API pública de GitHub**, que cualquiera puede reconsultar con el mismo
comando y obtener el mismo resultado. Los JSON crudos de respuesta están versionados junto a
este archivo, sin editar.

## Cómo reproducir esta evidencia

```bash
REPO=carla22072004/PFC-Presustentaciones-2026

# 1. Listado de corridas (no requiere autenticación: el repositorio es público)
curl -s "https://api.github.com/repos/$REPO/actions/runs?per_page=8"

# 2. Detalle job por job y paso por paso de una corrida concreta
curl -s "https://api.github.com/repos/$REPO/actions/runs/<RUN_ID>/jobs"
```

## Las tres corridas

### Corrida `33978578357`

| Campo | Valor |
|---|---|
| Conclusión | **success** |
| Commit | `99a6636` |
| Mensaje | docs(informe): actualizar cobertura a la cifra de cierre (63.17% line… |
| Rama | `main` |
| Fecha (UTC) | 2026-09-05T16:40:07Z |
| Enlace | https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/runs/33978578357 |
| Evidencia cruda | [`run-33978578357-jobs.json`](run-33978578357-jobs.json) |

Jobs (3) y pasos (33), todos con conclusión `success`:

- **Backend — build, tests, JaCoCo, análisis estático de seguridad** — `success`
  - Set up job — `success`
  - Initialize containers — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Compilar y ejecutar pruebas (JaCoCo se genera en la fase test) — `success`
  - Publicar reporte de cobertura JaCoCo — `success`
  - Publicar resultados de pruebas (surefire) — `success`
  - Análisis estático de seguridad (SpotBugs + find-sec-bugs) — `success`
  - Publicar reporte SpotBugs / find-sec-bugs — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Stop containers — `success`
  - Complete job — `success`
- **Entorno — genera docs/entorno/versions.txt desde el runner real de CI** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias del frontend (para que 'npx ng version' resuelva el Angular CLI local) — `success`
  - Generar docs/entorno/versions.txt con las versiones reales de este runner — `success`
  - Publicar versions.txt como artefacto verificable — `success`
  - Post Configurar Node 20 — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Complete job — `success`
- **Frontend — build de producción** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias — `success`
  - Build de producción (ng build) — `success`
  - Publicar build de producción — `success`
  - Post Configurar Node 20 — `success`
  - Post Checkout — `success`
  - Complete job — `success`

### Corrida `33949588592`

| Campo | Valor |
|---|---|
| Conclusión | **success** |
| Commit | `581d45d` |
| Mensaje | docs(informe): agregar diagramas C4, listados de codigo, anexos y uni… |
| Rama | `main` |
| Fecha (UTC) | 2026-09-05T06:21:27Z |
| Enlace | https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/runs/33949588592 |
| Evidencia cruda | [`run-33949588592-jobs.json`](run-33949588592-jobs.json) |

Jobs (3) y pasos (33), todos con conclusión `success`:

- **Frontend — build de producción** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias — `success`
  - Build de producción (ng build) — `success`
  - Publicar build de producción — `success`
  - Post Configurar Node 20 — `success`
  - Post Checkout — `success`
  - Complete job — `success`
- **Entorno — genera docs/entorno/versions.txt desde el runner real de CI** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias del frontend (para que 'npx ng version' resuelva el Angular CLI local) — `success`
  - Generar docs/entorno/versions.txt con las versiones reales de este runner — `success`
  - Publicar versions.txt como artefacto verificable — `success`
  - Post Configurar Node 20 — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Complete job — `success`
- **Backend — build, tests, JaCoCo, análisis estático de seguridad** — `success`
  - Set up job — `success`
  - Initialize containers — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Compilar y ejecutar pruebas (JaCoCo se genera en la fase test) — `success`
  - Publicar reporte de cobertura JaCoCo — `success`
  - Publicar resultados de pruebas (surefire) — `success`
  - Análisis estático de seguridad (SpotBugs + find-sec-bugs) — `success`
  - Publicar reporte SpotBugs / find-sec-bugs — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Stop containers — `success`
  - Complete job — `success`

### Corrida `33944802465`

| Campo | Valor |
|---|---|
| Conclusión | **success** |
| Commit | `76250b7` |
| Mensaje | Merge remote-tracking branch origin/main |
| Rama | `main` |
| Fecha (UTC) | 2026-09-05T04:31:54Z |
| Enlace | https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/runs/33944802465 |
| Evidencia cruda | [`run-33944802465-jobs.json`](run-33944802465-jobs.json) |

Jobs (3) y pasos (33), todos con conclusión `success`:

- **Entorno — genera docs/entorno/versions.txt desde el runner real de CI** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias del frontend (para que 'npx ng version' resuelva el Angular CLI local) — `success`
  - Generar docs/entorno/versions.txt con las versiones reales de este runner — `success`
  - Publicar versions.txt como artefacto verificable — `success`
  - Post Configurar Node 20 — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Complete job — `success`
- **Frontend — build de producción** — `success`
  - Set up job — `success`
  - Checkout — `success`
  - Configurar Node 20 — `success`
  - Instalar dependencias — `success`
  - Build de producción (ng build) — `success`
  - Publicar build de producción — `success`
  - Post Configurar Node 20 — `success`
  - Post Checkout — `success`
  - Complete job — `success`
- **Backend — build, tests, JaCoCo, análisis estático de seguridad** — `success`
  - Set up job — `success`
  - Initialize containers — `success`
  - Checkout — `success`
  - Configurar JDK 17 (Temurin) — `success`
  - Compilar y ejecutar pruebas (JaCoCo se genera en la fase test) — `success`
  - Publicar reporte de cobertura JaCoCo — `success`
  - Publicar resultados de pruebas (surefire) — `success`
  - Análisis estático de seguridad (SpotBugs + find-sec-bugs) — `success`
  - Publicar reporte SpotBugs / find-sec-bugs — `success`
  - Post Configurar JDK 17 (Temurin) — `success`
  - Post Checkout — `success`
  - Stop containers — `success`
  - Complete job — `success`

## Qué cubre cada corrida

El workflow ejecuta tres jobs en paralelo, y ninguno es decorativo:

1. **Backend** — levanta Postgres y Redis reales como *service containers* (no mocks),
   compila, corre la suite completa con JaCoCo y publica el reporte de cobertura y los
   resultados de surefire como artefactos; después corre SpotBugs + find-sec-bugs y publica
   ese reporte.
2. **Frontend** — build de producción de Angular (`ng build`) y publicación del bundle.
3. **Entorno** — regenera `docs/entorno/versions.txt` con las versiones reales de la máquina
   de CI y lo publica como artefacto, para que las versiones declaradas en el informe sean
   verificables y no dependan de la máquina de un integrante.

Es decir: que estas tres corridas estén en verde significa que, en una máquina limpia y
contra una base de datos real, el proyecto compila, sus 395 pruebas pasan y el análisis
estático de seguridad corre sin romper el pipeline.

