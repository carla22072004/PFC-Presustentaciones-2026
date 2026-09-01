# Checklist — Ralph 2021, Estándar complementario "Engineering Research"

**Referencia:** ACM SIGSOFT Empirical Standards — "Engineering Research" (evalúa artefactos de ingeniería: se construyó algo, ¿el diseño y la evaluación de ese algo son rigurosos?).
**Por qué aplica:** el Sistema de Gestión de Pre-Sustentaciones UTEQ es en sí mismo el artefacto de ingeniería producido por el proyecto.

| Criterio | Cumple | Evidencia / justificación |
|---|---|---|
| El problema/necesidad que motiva el artefacto está claramente definido | ✅ Sí | `docs/requisitos/SRS-v1.0.0.tex` sección 1.1 (Propósito) y 2.1 (Perspectiva del Producto) |
| Los requisitos del artefacto están documentados de forma verificable | 🟡 Parcial | 12 HU/RF y 4 RNF documentados en el SRS v1.0.0, con criterios de aceptación en Gherkin; ver [`INCOSE-REQUIREMENTS.md`](INCOSE-REQUIREMENTS.md) para hallazgos sobre la calidad de la redacción y [`../trazabilidad/matriz.csv`](../trazabilidad/matriz.csv) para qué porcentaje tiene evidencia automatizada real (100% de los Must, actualizado 2026-08-29; 75% general — RF-08/09/10 de prioridad Could/Should siguen sin test) |
| El artefacto se evaluó contra los requisitos declarados, no solo se construyó | ✅ Sí | `docs/trazabilidad/matriz.csv` conecta Requisito → HU → Módulo → Endpoint → Test → Evidencia empírica |
| Se documentan las decisiones de diseño y sus alternativas consideradas | ✅ Sí | 7 ADRs en `docs/adr/` (arquitectura general, JWT, estrategia de BD, frontend, seguridad, despliegue, permisos dinámicos) |
| Las decisiones de diseño se justifican con trade-offs explícitos, no solo se afirman | ✅ Sí | Cada ADR tiene sección "Consecuencias" con positivas y negativas explícitas (ver p. ej. `docs/adr/ADR-006-estrategia-hibrida-bd-sp.md`) |
| El artefacto fue evaluado por alguien más allá de quien lo construyó | ✅ Sí | `Informe-UNIDAD-4-PRESUS/AUTOEVALUACION-PRESUS.md` es una evaluación cruzada de otro equipo (Equipo E), no autoevaluación del mismo equipo que construyó el sistema |
| Las limitaciones conocidas del artefacto se declaran explícitamente, no se ocultan | ✅ Sí | Ejemplos reales y verificables: cobertura de tests real de 37.1% de líneas (no inflada), 14 vulnerabilidades de dependencias de build sin corregir (la crítica de código servido a producción, `@angular/core`, sí se corrigió), SUS sin aplicar a usuarios reales — todos declarados abiertamente en sus respectivos documentos |
| El artefacto es reproducible por un tercero a partir del repositorio | 🟡 Parcial | `docker compose up -d` reproduce el entorno completo (validado en la Fase 5: se encontraron y corrigieron 2 bugs reales que impedían un despliegue desde cero — migración duplicada y catálogo de roles sin sembrar). Sigue pendiente: `docs/despliegue/RUNBOOK.md` con el procedimiento operativo completo (diferido a Fase 8) |

## Resumen

6/8 criterios cumplidos, 2 parciales. El artefacto tiene evaluación externa real (no solo autoevaluación) y decisiones de diseño documentadas con trade-offs — poco común en proyectos estudiantiles, es un punto fuerte genuino. La brecha más clara es la ausencia de un runbook operativo formal (previsto para Fase 8) que complete la historia de reproducibilidad más allá del `docker compose up`.
