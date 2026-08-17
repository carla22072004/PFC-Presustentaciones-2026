# Checklist — PRISMA 2020

**Referencia:** Page, M.J. et al. (2021). *The PRISMA 2020 statement: an updated guideline for reporting systematic reviews*. BMJ 372:n71.

## No aplica a este proyecto — declaración explícita

PRISMA 2020 (*Preferred Reporting Items for Systematic Reviews and Meta-Analyses*) es un estándar de reporte para **revisiones sistemáticas de literatura y meta-análisis**: exige, entre otras cosas, una pregunta de investigación formal (formato PICO), una estrategia de búsqueda documentada en bases de datos bibliográficas, criterios de inclusión/exclusión de estudios, un diagrama de flujo de selección de estudios, y evaluación de riesgo de sesgo de los estudios incluidos.

Este proyecto es un **sistema de software construido y evaluado empíricamente** (Sistema de Gestión de Pre-Sustentaciones UTEQ), no una revisión de literatura. No existe en el repositorio:

- Una pregunta de investigación en formato PICO o equivalente.
- Una búsqueda sistemática en bases de datos académicas (IEEE Xplore, ACM DL, Scopus, etc.).
- Un diagrama de flujo PRISMA de selección de estudios.
- Una síntesis de hallazgos de múltiples estudios primarios.

Se verificó explícitamente que no existe contenido de revisión de literatura sistemática en `docs/requisitos/historico/SRS-v0.9.0-rc.md` (histórico) y `docs/requisitos/SRS-v1.0.0.pdf` (vigente) ni en las secciones del informe (`Informe-UNIDAD-4-PRESUS/secciones/`) que pudiera calificar parcialmente bajo este marco.

## Qué SÍ existe y podría confundirse con esto (aclaración)

El SRS y los ADRs citan estándares y tecnologías (ISO/IEC/IEEE 29148:2018, OWASP Top 10, RFC 7519) como referencias normativas de diseño, **no como una revisión de literatura**: son citas puntuales para justificar decisiones de ingeniería, no una síntesis sistemática de trabajos previos sobre un tema de investigación. Esa distinción es la razón por la que este checklist se documenta como "no aplica" en vez de omitirse silenciosamente — para que quede claro que la ausencia fue evaluada conscientemente y no pasada por alto.

## Si en una futura iteración se agregara una revisión de literatura

Si el equipo decidiera en el futuro justificar alguna decisión de arquitectura (p. ej. la elección de JWT + refresh token, o la estrategia híbrida JPA + procedimientos almacenados) contrastándola sistemáticamente contra literatura académica, en ese momento correspondería aplicar PRISMA 2020 (o, más realista para un alcance pequeño, una revisión de alcance/*scoping review* con un subconjunto de los ítems PRISMA) y documentarlo en este mismo archivo.
