# Sistema de Gestión de Pre-Sustentaciones UTEQ

[![Version](https://img.shields.io/badge/version-v0.9.0--rc-blue.svg)](https://github.com/carla22072004/PFC-Presustentaciones-2026)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.14892026.svg)](https://doi.org/10.5281/zenodo.14892026)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JaCoCo Coverage](https://img.shields.io/badge/coverage-%3E60%25-brightgreen.svg)](docs/informe-entrega-3.pdf)
[![OWASP Top 10](https://img.shields.io/badge/OWASP-PASSED-success.svg)](docs/seguridad/OWASP-AUDIT.md)

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
| **Especificación SRS** | [`docs/srs/SRS.md`](docs/srs/SRS.md) | Documento de requisitos bajo la norma ISO/IEC/IEEE 29148:2018 (15 HUs, 15 CUs). |
| **Matriz Trazabilidad** | [`docs/trazabilidad/matriz.csv`](docs/trazabilidad/matriz.csv) | Conexión Requisito → HU → Módulo → Endpoint → Test → Evidencia Empírica. |
| **Despliegue & Docker** | [`Makefile`](Makefile) / [`docker-compose.yml`](docker-compose.yml) | Comando `make up` y anclaje criptográfico de imágenes con digests `sha256`. |
| **Pruebas de Carga k6** | [`docs/pruebas/k6/`](docs/pruebas/k6/) | Script k6 y 3 corridas de resultados crudos JSON (`run1`, `run2`, `run3`). |
| **Auditoría OWASP** | [`docs/seguridad/OWASP-AUDIT.md`](docs/seguridad/OWASP-AUDIT.md) | Evaluación automática de 6 controles de seguridad OWASP Top 10 con logs. |
| **Usabilidad SUS** | [`docs/usabilidad/SUS-RESULTS.md`](docs/usabilidad/SUS-RESULTS.md) | Evaluación Cuestionario SUS con 10 personas externas (Score: 91.25 / 100 - Grado A+). |
| **Lighthouse Frontend** | [`docs/mediciones/lighthouse/LIGHTHOUSE-REPORT.md`](docs/mediciones/lighthouse/LIGHTHOUSE-REPORT.md) | Métricas Frontend: Rendimiento (94), Accesibilidad (98), Buenas Prácticas (96). |
| **Arquitectura C4** | [`docs/arquitectura/README.md`](docs/arquitectura/README.md) | Diagramas de Arquitectura Modelo C4 (Niveles 1 Contexto, 2 Contenedores, 3 Componentes). |
| **Registros ADR** | [`docs/arquitectura/adrs/`](docs/arquitectura/adrs/) | 6 Registros de Decisiones Arquitectónicas (ADR-001 a ADR-006). |
| **Taxonomía CRediT** | [`CONTRIBUTORS.md`](CONTRIBUTORS.md) | Asignación explícita de roles CRediT para los 4 integrantes del grupo universitario. |
| **Citación & Licencia** | [`CITATION.cff`](CITATION.cff) / [`LICENSE`](LICENSE) | Archivo de citación CFF válido y Licencia Open Source MIT. |
| 📖 **Diccionario Datos** | [`docs/mediciones/DATA-DICTIONARY.md`](docs/mediciones/DATA-DICTIONARY.md) | Explicación detallada de variables medidas en pruebas empíricas. |
| **Documento Ético** | [`docs/etica/ETHICS.md`](docs/etica/ETHICS.md) | Principios bioéticos y plantillas de consentimientos informados de pruebas SUS. |
| **Colección Postman** | [`docs/postman/PFC-Collection.json`](docs/postman/PFC-Collection.json) | Colección v2.1 con 22 peticiones HTTP RESTful probadas y documentadas. |
| **Informe Técnico PDF** | [`docs/informe-entrega-3.pdf`](docs/informe-entrega-3.pdf) | Informe técnico final en PDF (25 páginas) con todos los capítulos integrados. |
