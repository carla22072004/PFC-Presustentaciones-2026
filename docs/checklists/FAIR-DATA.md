# Checklist — Principios FAIR (Findable, Accessible, Interoperable, Reusable)

**Referencia:** Wilkinson, M.D. et al. (2016). *The FAIR Guiding Principles for scientific data management and stewardship*. Scientific Data 3, 160018.
**Alcance:** aplicado a la evidencia empírica generada por el proyecto (resultados de k6, Lighthouse, OWASP ZAP, JaCoCo, find-sec-bugs, npm audit) — no al software en sí, sino a los *datos* que documentan su comportamiento.

## Findable (Localizable)

| Criterio | Cumple | Evidencia |
|---|---|---|
| Los datos tienen una ubicación estable y documentada dentro del repositorio | ✅ | `docs/mediciones/` reorganizado en Fase 6 para coincidir con rutas declaradas exigidas por la guía; `docs/mediciones/DATA-PROVENANCE.md` indexa cada dato con su ubicación exacta |
| Existe metadata que describe qué mide cada dato | ✅ | `docs/mediciones/DATA-DICTIONARY.md` documenta unidades, tipo de dato y rangos aceptables para cada variable |
| Un identificador único y persistente (DOI, hash de commit) referencia cada versión de los datos | 🟡 Parcial | El repositorio tiene `CITATION.cff` y está preparado para Zenodo (`docs/ZENODO.md`), lo que da DOI a nivel de repositorio completo, pero no hay un identificador granular por archivo de datos individual (p. ej. no hay un DOI específico para `k6/run3-summary.json`) |

## Accessible (Accesible)

| Criterio | Cumple | Evidencia |
|---|---|---|
| Los datos son accesibles sin restricciones ni credenciales especiales | ✅ | Repositorio público en GitHub, licencia MIT (`LICENSE`) |
| Los datos se recuperan con protocolos estándar (HTTP/Git), no herramientas propietarias | ✅ | Todo es texto plano (JSON, CSV, XML, Markdown) versionado en Git |
| Los metadatos permanecen accesibles incluso si el dato crudo se elimina algún día | 🟡 Parcial | `DATA-PROVENANCE.md` y `DATA-DICTIONARY.md` seguirían existiendo, pero no hay un mecanismo de archivo (p. ej. snapshot en Zenodo) que garantice esto si el repo de GitHub se borrara |

## Interoperable (Interoperable)

| Criterio | Cumple | Evidencia |
|---|---|---|
| Los datos usan formatos estándar, no propietarios | ✅ | JSON (k6, Lighthouse, ZAP), CSV (JaCoCo, matriz de trazabilidad), XML (SpotBugs) — todos formatos abiertos y ampliamente soportados |
| El vocabulario/nombres de campo son estándar del dominio, no ad-hoc | ✅ | Los campos vienen directamente de las herramientas originales (k6, Lighthouse, SpotBugs) sin renombrar — cualquiera familiarizado con esas herramientas puede leerlos sin traducción |
| Los datos se referencian entre sí de forma consistente | ✅ | `DATA-PROVENANCE.md` conecta explícitamente cada cifra citada con su archivo de origen |

## Reusable (Reutilizable)

| Criterio | Cumple | Evidencia |
|---|---|---|
| Los datos tienen una licencia clara de uso | ✅ | MIT License a nivel de repositorio completo |
| Existe documentación suficiente para reutilizar los datos sin contactar a los autores originales | ✅ | Cada reporte (`k6/README.md`, `LIGHTHOUSE-REPORT.md`, `OWASP-AUDIT.md`) documenta metodología completa y comandos exactos de reproducción |
| Los datos cumplen estándares de la comunidad/dominio relevante (formato de salida nativo de la herramienta) | ✅ | No se post-procesaron ni reformatearon los JSON/XML crudos — se conservan tal como los emite cada herramienta, y el análisis (percentiles, IC 95%, Wilcoxon) se documenta por separado como derivado |
| Se declara la procedencia (provenance) del dato: qué proceso lo generó | ✅ | `DATA-PROVENANCE.md`, creado específicamente para este propósito en la Fase 6 |

## Resumen

11/13 criterios cumplidos completamente, 2 parciales — ambos relacionados con identificadores persistentes/DOI granulares y garantías de archivo a largo plazo, que dependen de publicar el repositorio en Zenodo (ya preparado en `docs/ZENODO.md`, pero no confirmado que se haya ejecutado el depósito real). Es el marco donde el proyecto muestra el cumplimiento más alto, consistente con la política ya existente en el repositorio de mantener evidencia cruda versionada y trazable en vez de solo cifras finales en prosa.
