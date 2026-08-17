# 📊 REGISTRO DE IDENTIFICADOR PERSISTENTE DOI EN ZENODO — DEPÓSITO DEL DATASET DE MEDICIONES

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Estado:** ⏳ Pendiente de depósito — este dataset **todavía no tiene un DOI real asignado**.
**Alcance de este documento:** el **conjunto de datos de mediciones empíricas** (rendimiento k6,
seguridad OWASP ZAP + find-sec-bugs, cobertura JaCoCo, calidad web Lighthouse) — **no** el código
del software, que se deposita por separado con su propia licencia MIT en un registro Zenodo
distinto (ver [`ZENODO.md`](ZENODO.md)).

---

## Por qué el dataset se deposita por separado del software

La guía de Fase 10 exige depositar el dataset "en un registro distinto con su propia licencia
Creative Commons Attribution 4.0, siguiendo el principio de citación independiente entre software
y datos". La razón práctica, no solo administrativa: el software se licencia MIT (una licencia de
código), mientras que los *datos* empíricos generados al correr ese software (los JSON de k6, los
reportes de Lighthouse, el XML de SpotBugs) son un producto distinto — alguien podría querer citar
"los datos de rendimiento del sistema X" sin necesariamente citar "el código del sistema X", o
viceversa, y cada uno tiene su propio ciclo de vida de versiones (el dataset de la corrida 3 de k6
no cambia aunque el código del backend reciba un nuevo commit).

## Qué se deposita exactamente

Todos los archivos crudos versionados ya en el repositorio y catalogados en
[`mediciones/DATA-PROVENANCE.md`](mediciones/DATA-PROVENANCE.md):

| Carpeta | Contenido |
|---|---|
| [`mediciones/perf/lighthouse/prod-runs/`](mediciones/perf/lighthouse/prod-runs/) | 6 corridas de Lighthouse (JSON): 3 desktop + 3 mobile |
| `k6/run1-summary.json` … `run5-summary.json` (raíz del repo) | 5 corridas de carga k6 |
| `k6/rate-limiter-evidence-run.json`, `k6/cache-cold-samples.txt`, `k6/cache-warm-samples.txt` | Evidencia de rate limiting y muestreo caché fría/caliente |
| [`mediciones/sec/zap/zap-baseline-report.json`](mediciones/sec/zap/zap-baseline-report.json) (+ `.html`) | Escaneo dinámico OWASP ZAP |
| [`mediciones/sec/static-analysis/spotbugs-findsecbugs-report.xml`](mediciones/sec/static-analysis/spotbugs-findsecbugs-report.xml) | Análisis estático SpotBugs + find-sec-bugs |
| [`mediciones/DATA-DICTIONARY.md`](mediciones/DATA-DICTIONARY.md), [`mediciones/DATA-PROVENANCE.md`](mediciones/DATA-PROVENANCE.md) | Metadatos: qué mide cada campo, y de qué comando salió cada archivo |

**No incluido:** los resultados de JaCoCo y `npm audit` no están versionados como archivos crudos
en el repositorio (se regeneran en cada `make test`/`make audit` y se publican como artefactos de
CI, ver `DATA-PROVENANCE.md`) — no se puede depositar en Zenodo lo que no es un archivo fijo; si en
el futuro se decide congelar una corrida específica de JaCoCo/npm audit como snapshot versionado,
ese snapshot se agregaría a este dataset en un depósito nueva versión.

## ✅ Cómo depositar el dataset (proceso real, ~15-20 minutos)

1. Crear una carpeta o archivo comprimido (`.zip`) local con exactamente los archivos de la tabla
   de arriba, manteniendo la misma estructura de carpetas relativa (facilita rastrear la
   procedencia contra `DATA-PROVENANCE.md` incluso fuera del repositorio Git).
2. Entrar a [zenodo.org](https://zenodo.org) e iniciar sesión con la cuenta de GitHub/ORCID de uno
   de los integrantes (la misma cuenta usada para el depósito del software, o una distinta — no
   es obligatorio que sea la misma).
3. Ir a **New upload** (no usar el flujo automático de GitHub-release aquí — ese es solo para el
   software; el dataset se sube manualmente como un depósito independiente).
4. Subir el `.zip` (o los archivos individuales) preparados en el paso 1.
5. Completar los metadatos del depósito:
   - **Título:** "Dataset de mediciones empíricas — Sistema de Gestión de Pre-Sustentaciones UTEQ (rendimiento, seguridad, calidad web)"
   - **Tipo de recurso:** Dataset
   - **Licencia:** **Creative Commons Attribution 4.0 International (CC-BY 4.0)** — no MIT, esa es la licencia del código
   - **Autores:** los mismos 4 integrantes que en `CITATION.cff` (mismo orden, mismas afiliaciones)
   - **Descripción:** resumir qué contiene cada subcarpeta (usar la tabla de arriba como base) y enlazar al repositorio de software (una vez que tenga su propio DOI, citarlo explícitamente aquí como "Related identifier: is derived from / is supplement to")
   - **Palabras clave:** las mismas de `CITATION.cff` más `dataset`, `rendimiento`, `seguridad`, `k6`, `lighthouse`, `owasp-zap`
6. Publicar el depósito. Zenodo genera un DOI real y permanente para esta versión exacta del dataset.
7. Una vez generado:
   - Agregar el DOI del dataset a este archivo (sección de abajo).
   - Agregar una referencia cruzada en `docs/ZENODO.md` (DOI del software) apuntando al DOI del dataset, y viceversa — así cualquiera que encuentre uno de los dos depósitos encuentra el otro.
   - Citar el DOI del dataset (no el del software) en cualquier tabla o figura del informe final que use estos datos directamente.

## DOI real del dataset

*(pendiente — completar cuando el depósito real se publique; no se fabrica un número de relleno, siguiendo la misma política que [`ZENODO.md`](ZENODO.md))*
