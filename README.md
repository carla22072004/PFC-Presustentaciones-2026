# Sistema de Gestión de Pre-Sustentaciones UTEQ

[![Version](https://img.shields.io/badge/version-v0.9.0--rc-blue.svg)](https://github.com/carla22072004/PFC-Presustentaciones-2026)
[![CI](https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/workflows/ci.yml/badge.svg)](https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JaCoCo Coverage](https://img.shields.io/badge/coverage-28.5%25-orange.svg)](docs/mediciones/jacoco/COVERAGE.md)
[![OWASP Top 10](https://img.shields.io/badge/OWASP-revisión_manual-yellow.svg)](docs/mediciones/sec/owasp/OWASP-AUDIT.md)

Sistema web para la automatización, gestión y evaluación de pre-sustentaciones de trabajos de titulación de la **Universidad Técnica Estatal de Quevedo (UTEQ)**.

---

## Despliegue Rápido (Comando Único)

Para levantar el entorno completo (Base de Datos PostgreSQL 15, Caché Redis, Backend Spring Boot y Frontend Angular servido por nginx):

```bash
# 1. Clonar el repositorio
git clone https://github.com/carla22072004/PFC-Presustentaciones-2026.git
cd PFC-Presustentaciones-2026

# 2. Copiar la plantilla de variables de entorno y generar un JWT_SECRET real
cp .env.example .env
# Editar .env y reemplazar JWT_SECRET con el resultado de: openssl rand -hex 32

# 3. Construir el frontend (nginx solo sirve el build ya generado, no lo construye)
cd Frontend && npx ng build --configuration production && cd ..

# 4. Levantar la infraestructura completa
docker compose up -d --build
```

El sistema estará disponible en:
- **Frontend Angular (servido por nginx):** `http://localhost`
- **API REST Backend versionada:** `http://localhost:8080/api/v1` (proxied también en `http://localhost/api/v1` vía nginx)
- **Documentación Swagger / OpenAPI 3.0:** `http://localhost:8080/swagger-ui/index.html`
- **Estado de salud:** `http://localhost:8080/actuator/health`

**Verificación completa de reproducibilidad (Criterio R1):** los pasos 1-2 de arriba (clonar y
crear `.env`) más `make all` ejecutan **todo** el pipeline — build, contenedores, espera de
migraciones, tests con JaCoCo, benchmarks k6, auditoría de seguridad, regeneración de figuras/
trazabilidad, y compilación del PDF final — terminando con código de salida 0 si todo funcionó:

```bash
cp .env.example .env
# Editar .env y reemplazar JWT_SECRET con el resultado de: openssl rand -hex 32
make all
```

Ver [`docs/entorno/VIDEO-DEMO-SCRIPT.md`](docs/entorno/VIDEO-DEMO-SCRIPT.md) para el guion del
video de demostración de este comando (⏳ video pendiente de grabar).

## Despliegue Público (Producción)

**Estado:** el sistema **aún no está desplegado públicamente** — ver
[`docs/despliegue/DEPLOYMENT.md`](docs/despliegue/DEPLOYMENT.md) para el proveedor elegido (Railway), el
procedimiento paso a paso y el estado real actualizado. Esta sección se completará con la URL pública y
la confirmación de HTTPS válido en cuanto el despliegue esté confirmado — **no se declara una URL que no
se haya verificado funcionando**, porque una URL pública inactiva el día de la evaluación reprueba
automáticamente el criterio P5 sin importar el resto de la documentación.

**Usuario de demostración** (una vez desplegado, para que el tribunal entre sin registrarse):
- **Email:** `demo@uteq.edu.ec`
- **Contraseña:** `Demo2026!`
- **Rol:** Coordinador (acceso a asignación de jurados, cronograma, reportes)

También existe un usuario **Administrador** (`admin@uteq.edu.ec` / `admin123`) para explorar la gestión de usuarios y catálogos.

---

## Resumen de Entregables y Documentación (Entrega 3)

| Componente | Ubicación en el Repositorio | Descripción |
|---|---|---|
| **Observaciones 1A & 1B** | [`docs/observaciones/OBSERVACIONES.md`](docs/observaciones/OBSERVACIONES.md) | Criterios, decisiones y hashes de commits de corrección. Etiquetas `v0.7.0` y `v0.7.1`. |
| **Catálogo de SP / SQL** | [`docs/basedatos/CATALOGO-SP.md`](docs/basedatos/CATALOGO-SP.md) | Documentación de procedimientos almacenados PL/pgSQL y funciones puras. |
| **Especificación SRS v1.0.0** | [`docs/requisitos/SRS-v1.0.0.pdf`](docs/requisitos/SRS-v1.0.0.pdf) / [`.tex`](docs/requisitos/SRS-v1.0.0.tex) | ISO/IEC/IEEE 29148:2018, 12 HU ([`historias/`](docs/requisitos/historias/)) + 12 CU Cockburn ([`casos-de-uso/`](docs/requisitos/casos-de-uso/)). Versión anterior en [`historico/`](docs/requisitos/historico/) afirmaba "15 HUs, 15 CUs" sin que existieran — corregido. |
| **Matriz Trazabilidad** | [`docs/trazabilidad/matriz.csv`](docs/trazabilidad/matriz.csv) | Requisito → HU → Módulo → Endpoint → Prioridad MoSCoW → Test real → Evidencia. 5/8 Must verificados con test real (62.5%), no el 100% — declarado explícitamente. |
| **Checklist INCOSE + elicitación** | [`docs/checklists/INCOSE-REQUIREMENTS.md`](docs/checklists/INCOSE-REQUIREMENTS.md) / [`docs/requisitos/elicitacion/`](docs/requisitos/elicitacion/) | 9 características INCOSE × 12 RF + 6 de conjunto; evidencia real de técnicas de elicitación (2/5 con evidencia documentada). |
| **Bitácora de requisitos** | [`docs/requisitos/CHANGELOG-REQ.md`](docs/requisitos/CHANGELOG-REQ.md) | Cambios entre SRS v0.9.0-rc y v1.0.0, tasa de estabilidad calculada. |
| **Despliegue & Docker** | [`Makefile`](Makefile) / [`docker-compose.yml`](docker-compose.yml) | `make up/down/restart/logs/ps/clean` + `make build/test/bench/audit/docs/pdf`, y **`make all`**: el objetivo de reproducibilidad end-to-end del Criterio R1 (Fase 10) — desde una clonación limpia levanta todos los contenedores, espera a que las migraciones Flyway se apliquen, corre tests + benchmarks + auditoría + reportes, y compila el PDF final, saliendo con código 0 solo si todo funcionó. Verificado real: `docker compose down -v && docker compose up -d --build` sobre un volumen de Postgres limpio llega a healthy, y `make pdf` compila el informe de 36 páginas sin errores. Imágenes ancladas por digest SHA-256. Guion para el video de demostración: [`docs/entorno/VIDEO-DEMO-SCRIPT.md`](docs/entorno/VIDEO-DEMO-SCRIPT.md) (⏳ video pendiente de grabar). |
| **Despliegue en producción** | [`docs/despliegue/`](docs/despliegue/) | `DEPLOYMENT.md` (proveedor y procedimiento), `RUNBOOK.md` (arranque/apagado/rotación), `BACKUP.md` (respaldo diario automatizado vía GitHub Actions). Estado real: aún no desplegado — ver detalle. |
| **Procedencia de datos** | [`docs/mediciones/DATA-PROVENANCE.md`](docs/mediciones/DATA-PROVENANCE.md) | Mapea cada cifra citada en el informe a su archivo crudo y comando de origen. |
| **Versiones del entorno** | [`docs/entorno/versions.txt`](docs/entorno/versions.txt) | Versiones exactas de Docker, JDK, Node, Angular CLI y k6 usadas para generar la evidencia. Regenerable con [`scripts/gen-versions.sh`](scripts/gen-versions.sh); el job `entorno` de [`ci.yml`](.github/workflows/ci.yml) publica su propia copia como artefacto en cada push. |
| **Checklists metodológicos** | [`docs/checklists/`](docs/checklists/) | Autoevaluación honesta contra Ralph 2021 (General + Engineering Research + Benchmarking), PRISMA 2020 (evaluado contra el procedimiento de búsqueda real del capítulo de trabajos relacionados), Runeson & Höst (no aplica — el proyecto es DSR+GQM, no estudio de caso), INCOSE y FAIR. |
| **DOI del software / dataset (Zenodo)** | [`docs/ZENODO.md`](docs/ZENODO.md) / [`docs/ZENODO-DATASET.md`](docs/ZENODO-DATASET.md) | ⏳ Pendiente de depósito real — proceso documentado paso a paso para depositar el software (release v1.0.0, licencia MIT) y el dataset de mediciones (CC-BY 4.0) en registros Zenodo **separados**, siguiendo el principio de citación independiente. `CITATION.cff` ya tiene los campos `orcid`/`doi` listos (comentados) para completarse cuando existan valores reales — ORCID requiere que cada integrante se registre personalmente en orcid.org. |
| **Scripts de análisis** | [`scripts/`](scripts/) | Notebooks reales y ejecutados (`perf-analysis.ipynb`, `sus-analysis.ipynb`) + `validate-traceability.sh`, `audit-sql-dynamic.sh`, `gen-figuras.py`. |
| **Pruebas de Carga k6** | [`k6/`](k6/) | Script k6, 5 corridas reales (`run1`-`run5`) y análisis estadístico caché fría/caliente (Wilcoxon, IC 95%, tamaño de efecto). |
| **Auditoría OWASP** | [`docs/mediciones/sec/owasp/OWASP-AUDIT.md`](docs/mediciones/sec/owasp/OWASP-AUDIT.md) | Revisión manual de 6 controles OWASP Top 10 + herramientas automáticas reales: OWASP ZAP (0 FAIL, 4 hallazgos corregidos), SpotBugs/find-sec-bugs (0 SQL dinámico, 1 timing-attack corregido), `npm audit` (41 deps vulnerables detectadas). |
| **Escaneo OWASP ZAP** | [`docs/mediciones/sec/zap/`](docs/mediciones/sec/zap/) | Reporte HTML/JSON de la corrida real `zap-baseline.py`. |
| **Análisis estático** | [`docs/mediciones/sec/static-analysis/STATIC-ANALYSIS.md`](docs/mediciones/sec/static-analysis/STATIC-ANALYSIS.md) | SpotBugs + find-sec-bugs: 189 hallazgos reales, 0 de SQL dinámico. |
| **Usabilidad SUS** | [`docs/mediciones/sus/SUS-RESULTS.md`](docs/mediciones/sus/SUS-RESULTS.md) | Instrumento SUS listo; pendiente de aplicar a usuarios reales. |
| **Lighthouse Frontend** | [`docs/mediciones/perf/lighthouse/LIGHTHOUSE-REPORT.md`](docs/mediciones/perf/lighthouse/LIGHTHOUSE-REPORT.md) | 6 corridas reales contra build de producción (3 desktop + 3 mobile): Rendimiento 64/61, Accesibilidad 89, Buenas Prácticas 100, SEO 91. Rendimiento aún bajo el umbral de 80 (ver nota metodológica sobre contención de CPU). |
| **Arquitectura C4** | [`docs/arquitectura/README.md`](docs/arquitectura/README.md) | Diagramas de Arquitectura Modelo C4 (Niveles 1 Contexto, 2 Contenedores, 3 Componentes). |
| **Registros ADR** | [`docs/adr/`](docs/adr/) | 6 Registros de Decisiones Arquitectónicas (ADR-001 a ADR-006). |
| **Taxonomía CRediT** | [`CONTRIBUTORS.md`](CONTRIBUTORS.md) | Asignación explícita de roles CRediT para los 4 integrantes del grupo universitario. |
| **Citación & Licencia** | [`CITATION.cff`](CITATION.cff) / [`LICENSE`](LICENSE) | Archivo de citación CFF válido y Licencia Open Source MIT. |
| 📖 **Diccionario Datos** | [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md) | Explicación detallada de variables medidas en pruebas empíricas. |
| **Documento Ético** | [`docs/etica/`](docs/etica/) | Principios bioéticos ([`ETHICS.md`](docs/etica/ETHICS.md)), plantilla de consentimiento informado ([`consentimientos/`](docs/etica/consentimientos/)) y declaración de uso de IA ([`ai-disclosure.md`](docs/etica/ai-disclosure.md)). |
| **Colección Postman** | [`docs/postman/PFC-Collection.json`](docs/postman/PFC-Collection.json) | Colección v2.1 con 27 peticiones HTTP RESTful (éxito, validación 400, autorización 401/403, no encontrado 404), verificada en la Fase 10 contra el backend real corriendo en Docker — no solo contra el código. Corrigió rutas que nunca existieron en una versión anterior (`/asignar-masivo`, `/{id}/firmar`, `/{id}/calcular-promedio`, `/reportes/defensas`, `POST /api/solicitudes` sin path) y el prefijo de versión `/api/v1/` faltante en todas las peticiones. |
| **Informe Final (académico, 12 capítulos)** | [`Informe-Final/informe-final.pdf`](Informe-Final/informe-final.pdf) | Documento IMRaD ampliado: SRS, DSR+GQM, arquitectura, evaluación empírica, discusión, amenazas a la validez, CRediT, 32 referencias verificadas (19 de alto impacto). Es el informe vigente para la defensa. |
| **Informe Técnico (Unidad IV, histórico)** | [`Informe-UNIDAD-4-PRESUS/`](Informe-UNIDAD-4-PRESUS/) | Informe técnico de la Unidad IV en `.docx`/`.tex`, anterior al Informe Final. |
