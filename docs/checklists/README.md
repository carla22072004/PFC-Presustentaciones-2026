# Checklists metodológicos — índice

Esta carpeta aplica cuatro marcos metodológicos públicos y reconocidos contra el estado real del proyecto, como autoevaluación honesta (no como un listado de casillas marcadas para cumplir un requisito administrativo). Cada checklist declara explícitamente qué se cumple, qué no, y por qué — siguiendo la misma política de este repositorio de no reportar cifras o cumplimientos que no se puedan verificar en el código o la evidencia.

| Archivo | Marco | Por qué aplica (o no) a este proyecto |
|---|---|---|
| [`RALPH-2021-GENERAL.md`](RALPH-2021-GENERAL.md) | ACM SIGSOFT Empirical Standards (Ralph et al., 2021) — Estándar General | Marco base para evaluar el rigor de cualquier afirmación empírica hecha en el informe (p. ej. "el rendimiento mejoró", "la caché reduce la latencia"). |
| [`RALPH-2021-ENGINEERING-RESEARCH.md`](RALPH-2021-ENGINEERING-RESEARCH.md) | Ralph 2021 — Estándar complementario "Engineering Research" | El sistema de Pre-Sustentaciones **es** el artefacto de ingeniería construido; este estándar evalúa si su diseño y validación siguen el rigor esperado de investigación en ingeniería de software. |
| [`RALPH-2021-BENCHMARKING.md`](RALPH-2021-BENCHMARKING.md) | Ralph 2021 — Estándar complementario "Benchmarking" | Las corridas de k6, Lighthouse y OWASP ZAP son, en esencia, *benchmarks* del sistema; este estándar evalúa si esas mediciones son metodológicamente sólidas (muestreo, reproducibilidad, amenazas a la validez). |
| [`PRISMA-2020.md`](PRISMA-2020.md) | PRISMA 2020 (revisiones sistemáticas de literatura) | **No aplica directamente** — el proyecto no incluye una revisión sistemática de literatura. Se documenta explícitamente por qué, en vez de omitirlo silenciosamente. |
| [`INCOSE-REQUIREMENTS.md`](INCOSE-REQUIREMENTS.md) | INCOSE Guide for Writing Requirements v4 | Aplicado a los 12 RF completos del SRS v1.0.0 ([`../requisitos/SRS-v1.0.0.pdf`](../requisitos/SRS-v1.0.0.pdf)): 9 características individuales + 6 de conjunto. |
| [`FAIR-DATA.md`](FAIR-DATA.md) | Principios FAIR (Findable, Accessible, Interoperable, Reusable) | Se aplica a la evidencia empírica generada (k6, Lighthouse, ZAP, JaCoCo, find-sec-bugs) para evaluar qué tan reutilizable y verificable es esa evidencia por un tercero. |

## Nota sobre la interpretación de "estándares complementarios según el tipo de estudio"

La guía pide, además de Ralph 2021, PRISMA 2020, INCOSE y FAIR, "los estándares complementarios según el tipo de estudio". Ralph et al. (2021) publican el Estándar General más una familia de estándares específicos por tipo de estudio (Engineering Research, Case Study Research, Benchmarking, Repository Mining, Sampling Studies, Data Science, etc.); no todos aplican a todo proyecto. Este proyecto contiene dos tipos de estudio reales: **construir un artefacto de ingeniería** (el sistema en sí) y **medir su comportamiento empíricamente** (k6/Lighthouse/ZAP). Por eso los dos complementarios elegidos son `Engineering Research` y `Benchmarking` — los dos que corresponden a lo que el equipo efectivamente hizo, no una lista genérica de los siete estándares de Ralph aplicados a ciegas.
