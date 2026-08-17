# CHANGELOG-REQ.md — Bitácora de cambios de requisitos

## Por qué este changelog empieza en v0.9.0-rc, no en "Entrega 1A"

El criterio pide la bitácora de cambios "entre la Entrega 1A y la Entrega Final". Se investigó el
historial real de Git (`git log --follow -- docs/srs/SRS.md`, y antes de eso, `git log --oneline --all`
completo): **no existió ningún documento SRS versionado antes de la Entrega 3** (commit `015fd6d`,
"feat(entrega-3): integración final de requisitos ISO 29148..."). Los requisitos anteriores a ese punto
existieron como código y como observaciones docentes ([`../observaciones/OBSERVACIONES.md`](../observaciones/OBSERVACIONES.md),
etiquetas `v0.7.0`/`v0.7.1` para 1A/1B), no como un artefacto SRS diffable. Por eso esta bitácora
declara honestamente que **no puede reconstruir un diff de requisitos para Entrega 1A** — no hay nada
real contra qué compararlo — y en su lugar documenta el único cambio de requisitos real y verificable
disponible: **de SRS v0.9.0-rc (Entrega 3) a SRS v1.0.0 (Fase 7, esta entrega)**.

## v1.0.0 (2026-08-17, Fase 7) — desde v0.9.0-rc (2026-07-30)

### Requisitos formalizados por primera vez (existían operativamente, sin HU/CU escrita)

`matriz.csv` ya referenciaba RF-04 hasta RF-12 desde la Entrega 3, pero el SRS en prosa
(`docs/srs/SRS.md`) solo tenía HU-01 a HU-05, con una numeración además inconsistente entre ambos
documentos (el HU-04/HU-05 del SRS no correspondía al HU-04/HU-05 de la matriz). Se formalizaron por
primera vez, en formato Connextra + INVEST + Gherkin:

- RF-04 Programación de cronograma de defensa (antes solo en `matriz.csv`, sin HU en prosa)
- RF-06 Generación de actas (antes numerado como parte de "HU-05" ambigua en el SRS viejo)
- RF-07 Firma digital de actas (idem)
- RF-08 Notificaciones (antes solo en `matriz.csv`)
- RF-09 Reportes de gestión (antes solo en `matriz.csv`)
- RF-10 Gestión de salas (antes solo en `matriz.csv`)
- RF-11 Gestión de usuarios (antes solo en `matriz.csv`)

**7 de 12 requisitos son nuevos en el sentido de "formalizados por primera vez como HU"**, aunque su
funcionalidad ya existía y estaba implementada (no es código nuevo, es documentación que llega a
alcanzar al código).

### Requisitos que se mantuvieron sin cambios de fondo (renumerados o reescritos en formato, no en contenido)

- RF-01 Autenticación — mismo alcance, reescrito a Connextra+Gherkin
- RF-02 Registro de solicitud — mismo alcance, reescrito a Connextra+Gherkin
- RF-03 Asignación de jurados — mismo alcance, reescrito a Connextra+Gherkin
- RF-05 Evaluación por rúbrica — mismo alcance (antes numerado "HU-04" en el SRS viejo), reescrito
- RF-12 Anteproyectos — mismo alcance (antes numerado "HU-05" combinado con actas en el SRS viejo, separado aquí)

### Correcciones de evidencia (no son cambios de requisito, son correcciones de citas incorrectas en `matriz.csv`)

- RF-05: la cita "k6 Load Test Run 1 JSON" era incorrecta — el script k6 real nunca ejercita `/evaluaciones/**`.
- RF-09: la cita "k6 Load Test Run 2 JSON" era incorrecta — el script k6 real nunca ejercita `/reportes/**`.
- RF-10: la cita "SUS Usability Score > 80" era un *non sequitur* — un puntaje de usabilidad general del
  sistema no es evidencia de que el CRUD de salas específicamente funcione.
- RF-12: la cita "Lighthouse Accessibility > 90" era igualmente un *non sequitur* por la misma razón.
- 5 de 12 filas citaban archivos de test (`*ServiceImplTest.java`) que **no existen** en el repositorio
  — confirmado con [`../../scripts/validate-traceability.sh`](../../scripts/validate-traceability.sh).

Ver `docs/trazabilidad/matriz.csv` v1.0.0 para el detalle corregido completo, y
`docs/requisitos/historico/matriz-v0.9.0-rc.csv` para la versión original con las citas incorrectas
(conservada para trazabilidad histórica, no para que se siga usando).

## Cálculo de la tasa de estabilidad de requisitos

**Fórmula:** `1 - (requisitos modificados / requisitos totales)`

Definiendo "modificado" de forma estricta como *cambio de alcance o intención* (no solo cambio de
formato/redacción):

- Requisitos totales: 12
- Requisitos con cambio real de alcance entre v0.9.0-rc y v1.0.0: **0** (los 7 "nuevos" no cambiaron de
  alcance — pasaron de estar implícitos en `matriz.csv`/el código a estar documentados explícitamente;
  la funcionalidad no cambió)
- Requisitos que cambiaron de **redacción/formato** (Connextra+Gherkin) sin cambiar de alcance: 12/12

**Tasa de estabilidad de alcance: 1 - (0/12) = 1.00 (100%)**
**Tasa de estabilidad de documentación/redacción: 1 - (12/12) = 0.00 (0%)** — se reescribió el 100% del
texto de los requisitos en esta fase, pero por un cambio deliberado de formato (Fase 7), no porque el
negocio haya cambiado de opinión sobre lo que el sistema debe hacer.

Se reportan ambas cifras porque una sola serían engañosa: reportar solo "100% estable" ocultaría que se
reescribió todo el texto; reportar solo "0% estable" sugeriría erróneamente que el alcance del sistema
cambió, cuando en realidad no cambió — la funcionalidad de los 12 RF ya existía y funcionaba antes de
esta fase, lo que cambió es que ahora está documentada como debía estarlo desde la Entrega 3.
