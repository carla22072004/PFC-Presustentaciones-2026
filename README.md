# Sistema de Gestión de Pre-Sustentaciones UTEQ

[![Version](https://img.shields.io/badge/version-v0.9.0--rc-blue.svg)](https://github.com/carla22072004/PFC-Presustentaciones-2026)
[![CI](https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/workflows/ci.yml/badge.svg)](https://github.com/carla22072004/PFC-Presustentaciones-2026/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JaCoCo Coverage](https://img.shields.io/badge/coverage-22.7%25-orange.svg)](docs/mediciones/jacoco/COVERAGE.md)
[![OWASP Top 10](https://img.shields.io/badge/OWASP-revisión_manual-yellow.svg)](docs/mediciones/sec/owasp/OWASP-AUDIT.md)

Sistema web para la automatización, gestión y evaluación de pre-sustentaciones de trabajos de titulación de la **Universidad Técnica Estatal de Quevedo (UTEQ)**.

---

## Despliegue Rápido (Comando Único)

Para levantar el entorno completo (Base de Datos PostgreSQL 15, Caché Redis, Backend Spring Boot y Frontend Angular):

```bash
# 1. Clonar el repositorio
git clone https://github.com/carla22072004/PFC-Presustentaciones-2026.git
cd PFC-Presustentaciones-2026

# 2. Levantar la infraestructura completa
docker compose up -d
```

El sistema estará disponible en:
- **Frontend Angular:** `http://localhost:4200`
- **API REST Backend:** `http://localhost:8080/api`
- **Documentación Swagger / OpenAPI 3.0:** `http://localhost:8080/swagger-ui/index.html`

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
| **Despliegue & Docker** | [`Makefile`](Makefile) / [`docker-compose.yml`](docker-compose.yml) | `make up/down/restart/logs/ps/clean` + `make all/test/bench/audit/docs` (build, tests, k6, auditoría de seguridad, figuras). |
| **Procedencia de datos** | [`docs/mediciones/DATA-PROVENANCE.md`](docs/mediciones/DATA-PROVENANCE.md) | Mapea cada cifra citada en el informe a su archivo crudo y comando de origen. |
| **Versiones del entorno** | [`docs/entorno/versions.txt`](docs/entorno/versions.txt) | Versiones exactas de Docker, JDK, Node, Angular CLI y k6 usadas para generar la evidencia. |
| **Checklists metodológicos** | [`docs/checklists/`](docs/checklists/) | Autoevaluación honesta contra Ralph 2021 (General + Engineering Research + Benchmarking), PRISMA 2020, INCOSE y FAIR. |
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
| **Colección Postman** | [`docs/postman/PFC-Collection.json`](docs/postman/PFC-Collection.json) | Colección v2.1 con 22 peticiones HTTP RESTful probadas y documentadas. |
| **Informe Técnico (Unidad IV)** | [`Informe-UNIDAD-4-PRESUS/`](Informe-UNIDAD-4-PRESUS/) | Informe técnico en `.docx`/`.tex` con todos los capítulos integrados (el PDF de la Entrega 3 anterior se eliminó del repositorio; este es el informe vigente). |
