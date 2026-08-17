# Declaración de Uso de Inteligencia Artificial (AI Disclosure)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Última actualización:** 2026-08-17

## Declaración

Este proyecto usó asistentes de IA generativa (incluyendo Claude, de Anthropic, a través de Claude Code) como herramienta de apoyo durante el desarrollo, en las siguientes actividades:

- Generación e implementación de código (backend Spring Boot, frontend Angular, configuración de infraestructura como Docker/nginx/CI).
- Redacción y actualización de documentación técnica (ADRs, SRS, reportes de mediciones, este mismo documento).
- Diagnóstico y corrección de errores (bugs de reproducibilidad, configuración, seguridad).
- Ejecución y análisis de pruebas automatizadas (k6, Lighthouse, JaCoCo, OWASP ZAP, SpotBugs/find-sec-bugs) y del análisis estadístico derivado (percentiles, intervalos de confianza, test de Wilcoxon).
- Reestructuración del árbol de documentación del repositorio (Fase 6).

## Qué NO hizo la IA sin supervisión

- Ninguna cifra o resultado empírico se publicó sin haberse verificado ejecutando realmente la herramienta correspondiente en esta sesión (ver [`../mediciones/DATA-PROVENANCE.md`](../mediciones/DATA-PROVENANCE.md) para la trazabilidad de cada dato a su comando de origen).
- Las decisiones de arquitectura documentadas en `docs/adr/` reflejan decisiones tomadas y validadas por el equipo, no solo generadas automáticamente.
- No se usó IA para generar datos de participantes humanos (evaluaciones SUS, consentimientos informados) — de hecho, una parte del trabajo de este mismo proceso fue **detectar y corregir** datos de ese tipo que una versión anterior del proyecto había fabricado (ver la nota de integridad en [`ETHICS.md`](ETHICS.md) sección 3 y en [`../mediciones/sus/SUS-RESULTS.md`](../mediciones/sus/SUS-RESULTS.md)).

## Nota de transparencia sobre el historial del proyecto

Varios documentos de este repositorio (`docs/mediciones/sus/SUS-RESULTS.md`, el historial de `docs/mediciones/perf/lighthouse/LIGHTHOUSE-REPORT.md`, `k6/README.md`) documentan explícitamente que versiones anteriores del proyecto contenían **datos fabricados** (puntajes SUS, métricas Lighthouse, corridas de k6 inventadas) que nunca se ejecutaron realmente. Esto es consistente con un patrón conocido de mal uso de IA generativa: pedirle a un asistente que "genere" evidencia empírica sin ejecutar las herramientas reales, en vez de usarlo para ejecutar, analizar e interpretar resultados reales. El equipo identificó y corrigió esos casos reemplazando cada cifra fabricada por una corrida real y documentando el comando exacto usado — esa es la política de uso de IA que rige el resto del proyecto de aquí en adelante: **la IA puede ejecutar herramientas, escribir código y redactar documentación, pero nunca debe inventar resultados de mediciones que no se corrieron.**

## Responsabilidad

La responsabilidad final por la corrección, seguridad y honestidad del contenido de este repositorio recae en el equipo humano de desarrollo, independientemente de qué porción del trabajo se haya producido con asistencia de IA. El uso de IA no exime de revisión humana antes de una entrega o defensa.
