# 🌐 REGISTRO DE IDENTIFICADOR PERSISTENTE DOI EN ZENODO — DEPÓSITO DEL SOFTWARE

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Estado:** ✅ **Archivado y Verificado** — DOI de la versión actual: [10.5281/zenodo.22445216](https://doi.org/10.5281/zenodo.22445216)  
**Versión archivada actual:** `v1.0.1` (cierre real de la Entrega Final, tag Git `v1.0.1`)  
**DOI de concepto (resuelve siempre a la última versión):** [10.5281/zenodo.21988563](https://doi.org/10.5281/zenodo.21988563)  
**Licencia:** MIT Open Source License  
**Alcance de este documento:** el DOI del **software** (el código de este repositorio). El
conjunto de datos de mediciones (k6, ZAP, Lighthouse, JaCoCo) se deposita por separado, con su
propia licencia CC-BY 4.0 y su propio DOI ([10.5281/zenodo.22398713](https://doi.org/10.5281/zenodo.22398713)),
siguiendo el principio de citación independiente entre software y datos — ver
[`ZENODO-DATASET.md`](ZENODO-DATASET.md).

---

## 📌 Versiones archivadas

Zenodo trata cada tag como una versión distinta bajo el mismo DOI de concepto. El tag Git `v1.0.0`
se conserva intacto en el repositorio precisamente porque esta primera versión ya lo archivó — no se
mueve un tag que un DOI publicado ya usa como referencia.

| Versión | Tag Git | DOI de la versión | Publicado | Notas |
|---|---|---|---|---|
| **v1.0.1** (actual) | `v1.0.1` | [10.5281/zenodo.22445216](https://doi.org/10.5281/zenodo.22445216) | 6 sep 2026 | Cierre real de la Entrega Final: correcciones de las Entregas 1A/1B/3 aplicadas (cobertura 63,17 %, CSP endurecida, catálogo de SP completo, evidencia OWASP real) — ver `docs/observaciones/OBSERVACIONES.md`. |
| v1.0.0 | `v1.0.0` | [10.5281/zenodo.21988564](https://doi.org/10.5281/zenodo.21988564) | 18 ago 2026 | Release Final original, archivado vía la integración Zenodo↔GitHub sobre el release de esa fecha. |

- **Enlace permanente a la última versión:** [`https://doi.org/10.5281/zenodo.21988563`](https://doi.org/10.5281/zenodo.21988563) (DOI de concepto — usar este enlace cuando se quiera citar "el software" en general, no una corrida específica).
- **Registro de la versión actual:** `https://zenodo.org/records/22445216`
- **Badge oficial (cita siempre la última versión):**
  [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.21988563.svg)](https://doi.org/10.5281/zenodo.21988563)

---

## 🏷️ Metadatos del Registro y Cita Académica

- **Título del Registro:** Sistema de Gestión de Pre-Sustentaciones de Titulación UTEQ
- **Licencia:** MIT Open Source License
- **Repositorio GitHub:** `https://github.com/carla22072004/PFC-Presustentaciones-2026`
- **Autores CRediT:** Jean Pierre Alava Alvarado ([ORCID: 0009-0001-2878-2919](https://orcid.org/0009-0001-2878-2919)), Xavier Alejandro Moncayo Loor, Carla Esthefania Zamora Arias, Heider Dominick Barreto Rosado.
- **Verificación externa real (2026-09-06):** `curl https://zenodo.org/api/records/22445216` (API pública
  de Zenodo, no solo el badge) devuelve `version: v1.0.1`, `conceptdoi: 10.5281/zenodo.21988563`,
  licencia MIT, los mismos 4 autores en el mismo orden, y el tamaño de archivo (14.415.965 bytes)
  coincide exactamente con el `git archive` generado localmente sobre el tag `v1.0.1` — no solo con lo
  que muestra la interfaz de Zenodo. La verificación anterior (2026-08-30) sobre `v1.0.0` sigue siendo
  válida para esa versión.
- **Formato de Cita BibTeX (versión actual):**

```bibtex
@software{alava_alvarado_2026_presustentaciones,
  author       = {Alava Alvarado, Jean Pierre and Moncayo Loor, Xavier Alejandro and Zamora Arias, Carla Esthefania and Barreto Rosado, Heider Dominick},
  title        = {Sistema de Gestión de Pre-Sustentaciones de Titulación UTEQ},
  year         = 2026,
  publisher    = {Zenodo},
  version      = {v1.0.1},
  doi          = {10.5281/zenodo.22445216},
  url          = {https://doi.org/10.5281/zenodo.22445216}
}
```
