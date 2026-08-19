# Checklist — Runeson & Höst (2009), Guidelines for Case Study Research

**Referencia:** Runeson, P. & Höst, M. (2009). *Guidelines for conducting and reporting case study research in software engineering*. Empirical Software Engineering, 14(2), 131-164.

## No aplica a este proyecto — declaración explícita

La guía de Fase 10 pide este checklist **"si el trabajo se documenta como estudio de caso"**. Este
proyecto explícitamente **no** se documenta como estudio de caso. [`Informe-Final/secciones/07-materiales-metodos.tex`](../../Informe-Final/secciones/07-materiales-metodos.tex)
declara con todas sus letras el marco metodológico real usado:

- **Design Science Research (DSR)**, modelo de proceso de seis actividades de Peffers et al. (2007) —
  instanciado actividad por actividad en la Tabla 7.1 del informe (identificar problema → definir
  objetivos → diseño y desarrollo → demostración → evaluación → comunicación).
- **Goal-Question-Metric (GQM)** de Basili (1994) para la meta de rendimiento del sistema de caché Redis.

Ninguno de los dos es un estudio de caso en el sentido de Runeson & Höst. La diferencia no es cosmética:

| Estudio de caso (Runeson & Höst) | Lo que este proyecto realmente hace |
|---|---|
| Estudia un fenómeno **ya existente** en su contexto real (una organización, un equipo, un proceso en producción) sin intervenir en su diseño | El equipo **construye** el artefacto de software desde cero — no hay un fenómeno preexistente que se esté observando pasivamente |
| El investigador es externo u observador del objeto de estudio | El equipo es simultáneamente el constructor y el evaluador del artefacto (ver [`RALPH-2021-ENGINEERING-RESEARCH.md`](RALPH-2021-ENGINEERING-RESEARCH.md) para la discusión honesta de esa tensión) |
| Preguntas de investigación tipo "¿cómo/por qué ocurre X en este contexto?" | Preguntas de ingeniería tipo "¿el sistema que construimos cumple el RNF-01?" (ver el esquema GQM, Tabla 7.2 del informe) |
| Protocolo de recolección con múltiples fuentes de evidencia triangulada de un caso real (entrevistas, observación directa, documentos de la organización) | Evidencia empírica generada por el propio equipo corriendo herramientas (k6, Lighthouse, ZAP, JaCoCo) contra el sistema que ellos mismos construyeron |

## Por qué no se fuerza el checklist de todas formas

Podría objetarse que "construir y evaluar un sistema para UTEQ" es, en sentido amplio, un caso (la UTEQ
como organización). Se decidió **no** forzar esa lectura por una razón concreta y verificable: el sistema
**no está desplegado en producción en UTEQ** (ver [`../despliegue/DEPLOYMENT.md`](../despliegue/DEPLOYMENT.md)) y
ningún dato de uso real de la institución se recolectó — no hay caso real que observar todavía, solo un
artefacto construido y evaluado en el entorno de desarrollo del equipo. Aplicar Runeson & Höst aquí sería
maquillar de "estudio de caso" lo que en realidad es ingeniería + medición, exactamente el tipo de
sobre-etiquetado que este repositorio evita en cualquier otro documento (ver la nota equivalente en
[`RALPH-2021-GENERAL.md`](RALPH-2021-GENERAL.md) y [`PRISMA-2020.md`](PRISMA-2020.md)).

## Si en el futuro el sistema se despliega y se usa realmente en UTEQ

Si el sistema llega a operar en producción con usuarios reales de UTEQ (coordinadores, docentes,
estudiantes) y el equipo decide estudiar cómo se usa o qué efecto tiene sobre el proceso real de
pre-sustentación, en ese momento un estudio de caso post-despliegue sí sería el diseño correcto, y
correspondería aplicar las guías de Runeson & Höst (definición del caso, unidades de análisis, fuentes de
evidencia trianguladas, protocolo de estudio) y completar este archivo con esa evaluación real.
