# Evidencia de técnicas de elicitación de requisitos

**Método de esta reconstrucción:** se revisó el repositorio completo (historial de Git, documentos)
buscando evidencia real de cada una de las 5 técnicas que pide la guía (entrevistas, observación,
prototipado, workshops, análisis de documentos). **No se fabricó ninguna transcripción, acta de reunión
ni nota de observación que no existiera ya en el repositorio** — donde no hay evidencia real, se declara
explícitamente en vez de inventarla, siguiendo la misma política que ya se aplicó al corregir la encuesta
SUS fabricada (ver [`../../mediciones/sus/SUS-RESULTS.md`](../../mediciones/sus/SUS-RESULTS.md)) y las
citas de custodia de consentimientos falsas en [`../../etica/ETHICS.md`](../../etica/ETHICS.md).

## ✅ Análisis de documentos — evidencia real y sustancial

La técnica de elicitación con evidencia más fuerte y verificable en este proyecto es el **análisis de
observaciones docentes**, documentado en [`../../observaciones/OBSERVACIONES.md`](../../observaciones/OBSERVACIONES.md):
7 observaciones reales (OBS-01 a OBS-07) de las Entregas 1A y 1B, cada una con:
- El criterio/observación original del docente-director.
- La decisión técnica tomada en respuesta.
- El componente de código afectado.
- El **hash de commit real** donde se aplicó la corrección.

Esto constituye elicitación real por retroalimentación de stakeholder (el docente actuando como
representante de los requisitos de calidad institucionales), capturada y trazada de forma verificable.

Un segundo documento real de esta categoría es [`../../../Informe-UNIDAD-4-PRESUS/AUTOEVALUACION-PRESUS.md`](../../../Informe-UNIDAD-4-PRESUS/AUTOEVALUACION-PRESUS.md):
una evaluación cruzada de otro equipo (Equipo E) que identificó 10 hallazgos (E1-E10) sobre brechas
funcionales, la mayoría de los cuales se cerraron en commits posteriores rastreables (ver el historial
de `backend/`). Es evidencia real de elicitación por revisión de pares.

## 🟡 Prototipado — evidencia parcial

El propio historial de Git es evidencia de desarrollo iterativo con validación progresiva: 40+ commits
que evolucionan el sistema en respuesta a hallazgos (fabricación de SUS/Lighthouse detectada y corregida,
migración UUID→Long, endurecimiento de seguridad en varias rondas). No existe, sin embargo, un registro
formal de sesiones de prototipado con usuarios reales (mockups mostrados a estudiantes/docentes con
retroalimentación capturada) — el "prototipado" real fue el sistema funcional mismo, iterado y
corregido, no maquetas de baja fidelidad validadas con usuarios antes de construir.

## ❌ Entrevistas — sin evidencia documentada

No se encontró ninguna transcripción, grabación, o resumen de entrevista con estudiantes, docentes o
coordinadores en el repositorio. Si el equipo realizó conversaciones informales con stakeholders (p. ej.
consultando a un coordinador real sobre el flujo de asignación de jurados), **no quedó registro
documental de ello**. Se declara como técnica no evidenciada, no como técnica no aplicada — es posible
que haya ocurrido informalmente sin dejar rastro, pero este documento no puede afirmar eso sin evidencia.

## ❌ Observación directa — sin evidencia documentada

No existe ningún registro de observación directa del proceso real de pre-sustentación en la UTEQ (p. ej.
notas de campo de presenciar una defensa real para entender el flujo). El conocimiento del dominio
reflejado en el SRS (roles, flujo de actas, rúbricas ponderadas) es consistente con conocimiento
institucional de los integrantes del equipo como estudiantes de la propia carrera, pero eso no equivale
a una sesión de observación formal documentada.

## ❌ Workshops — sin evidencia documentada

No se encontró ningún acta, agenda o lista de asistentes de un taller de elicitación de requisitos.

## Resumen

| Técnica | Evidencia real encontrada |
|---|---|
| Análisis de documentos | ✅ Fuerte — `OBSERVACIONES.md` (7 hallazgos, trazados a commits) + `AUTOEVALUACION-PRESUS.md` (10 hallazgos, evaluación de pares) |
| Prototipado | 🟡 Parcial — desarrollo iterativo real vía Git, sin sesiones formales de validación de mockups |
| Entrevistas | ❌ Sin evidencia documentada |
| Observación directa | ❌ Sin evidencia documentada |
| Workshops | ❌ Sin evidencia documentada |

**2 de 5 técnicas tienen evidencia real verificable; 1 es parcial; 2 no tienen evidencia documentada.**
Esto no significa necesariamente que esas técnicas no se hayan usado — significa que este repositorio,
que es la fuente de verdad declarada por la guía, no contiene evidencia de ellas. Recomendación honesta
para el equipo: si en efecto hubo entrevistas u observación informal no documentada, reconstruir un
resumen breve ahora (con fecha aproximada y participantes) es preferible a dejarlo sin registro, pero
eso requiere que el equipo humano aporte esa información real — no se puede reconstruir desde el código.
