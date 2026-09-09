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

---

## v1.0.0 (2026-09-09) — primera versión de la especificación vigente

Especificación redactada y verificada contra el código real de la etiqueta `v1.0.1` (commit
`9d7abbd`), contrastada con la rama principal en el commit `73c8902`. El documento vigente es
[`SRS-v1.0.0.tex`](SRS-v1.0.0.tex) / [`.pdf`](SRS-v1.0.0.pdf), con copia editable en
[`SRS-v1.0.0.docx`](SRS-v1.0.0.docx).

**Sustituye a los dos borradores anteriores**, que pasan a `historico/` y no deben citarse como
especificación vigente:

- `historico/SRS-v0.9.0-rc.md` (2026-07-30): 5 historias de usuario, sin casos de uso.
- `historico/SRS-v1.0.0-2026-08-17.tex` / `.pdf`: 12 historias, 12 casos de uso y un resumen de 12
  requisitos funcionales y 4 no funcionales. Se archiva con la fecha en el nombre para que su
  identificador antiguo no se confunda con esta especificación.

### Motivo

El borrador del 2026-08-17 describía **16 requisitos** (12 RF + 4 RNF) de un sistema con **31 controladores
y 207 rutas**. Un SRS que describe menos de una décima parte del sistema deja de servir como
contrato y como base de verificación. Además arrastraba tres defectos de trazabilidad que el
validador no detectaba.

### Corpus

| | Borrador 2026-08-17 | Esta especificación |
|---|---|---|
| Requisitos funcionales | 12 | **64** |
| Requisitos no funcionales | 4 | **26** |
| Total | 16 | **90** |
| Filas de la matriz | 15 | **90** |
| Must verificados | 8/8 declarado (100 %) | **36/50 (72 %)** real |

**De los 74 requisitos añadidos, 69 documentan capacidades ya construidas** y sin requisito que
las especificara; solo 5 describen algo que aún no existe o no puede verificarse (RF-05, RF-06,
RNF-11, RNF-19, RNF-22). No es alcance nuevo: es la especificación alcanzando al código.

**Estabilidad del corpus heredado: 93,8 %.** De los 16 requisitos del borrador, 15 se conservan con
el mismo alcance (renumerados y completados con la plantilla) y 1 cambia de prioridad con
justificación. Ninguno se retira.

### Defectos de trazabilidad corregidos

1. **10 de los 15 endpoints de la matriz no resolvían.** Todos llevaban el prefijo `/api/v1/` que
   su controlador no tiene (RF-02, RF-03, RF-04, RF-05, RF-06, RF-09, RF-10, RF-11, RF-12 y RF-13
   de la numeración del borrador). Causa raíz: solo 8 de los 31 controladores usan ese prefijo
   (RNF-26).
2. **3 historias de usuario citadas no existían:** HU-13, HU-14 y HU-15. El directorio contiene
   HU-01 a HU-12.
3. **El SRS y la matriz declaraban conjuntos distintos** en la misma versión: 12 requisitos
   frente a 15 filas.
4. **El validador no detectaba ninguno de los tres.** Comprobaba el endpoint quedándose con el
   *primer segmento* de la ruta tras descartar el prefijo, de modo que una ruta versionada
   inexistente coincidía con el controlador sin versionar. Sobre la matriz del borrador informaba
   «15/15 filas consistentes» y «8/8 Must verificados (100 %)».

### Cambios de estado por corrección de cifra

- **Cobertura de pruebas (RNF-04 del borrador):** El borrador declaraba 38,88 % de líneas y «no cumplido». La
  medición vigente al cierre es de **81,03 % de líneas y 64,39 % de ramas** con 559 pruebas
  (2026-09-06). Aquí es **RNF-21**, con umbral elevado al 70 % de líneas y de ramas, en estado
  Verificado, y con la fecha de medición dentro del propio requisito.
- **Firma del acta (RF-07 del borrador → RF-37):** sube de *Should* a **Must**. Es la única transición que
  lleva la solicitud a `COMPLETADA`; un requisito del que depende el cierre del proceso no puede
  ser opcional.
- **Usabilidad (RNF-03 del borrador):** el umbral pasa de «SUS > 75» a **SUS ≥ 68** (media poblacional de
  referencia del instrumento) y se declara explícitamente la muestra exigida. Aquí es **RNF-22**,
  en estado Planificado: el instrumento sigue sin aplicarse a personas reales.

### Requisitos nuevos con estado No cumplido (deuda declarada, no oculta)

`RNF-02` rendimiento del cliente (68/61 frente a umbral 80), `RNF-04` degradación ante caída de
Redis (hoy *fail-open* en la revocación de tokens), `RNF-06` política de contraseñas (hoy 6
caracteres y solo en el alta), `RNF-15` cuentas de demostración con contraseñas literales en el
código, `RNF-25` siembra no determinista de los estados del dominio, `RNF-26` versionado parcial
de la API.

### Secciones estructurales incorporadas

Interfaces externas (§4), catálogo de estados del dominio y transiciones (§5), matriz de permisos
rol × operación (§6), correspondencia con el Anexo C de la norma (§11) y cláusula de verificación
(§9.1).

### Validador

`scripts/validate-traceability.sh` pasa de 2 comprobaciones a **8** (V1–V8, especificadas en §9.1 del SRS): número de columnas, vocabulario cerrado de estado, correspondencia SRS ↔ matriz en
ambos sentidos, resolución del endpoint por **ruta completa**, existencia de la clase de prueba,
existencia de la historia o caso de uso citado, evidencia obligatoria para *Verificado*, y
coherencia de *Planificado*. Las seis comprobaciones nuevas habrían detectado, antes de la
entrega, los cuatro defectos listados arriba.
