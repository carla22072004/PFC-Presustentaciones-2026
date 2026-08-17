# Checklist — PRISMA 2020

**Referencia:** Page, M.J. et al. (2021). *The PRISMA 2020 statement: an updated guideline for reporting systematic reviews*. BMJ 372:n71.
**Alcance:** aplicado a la Sección "Trabajos Relacionados" de [`Informe-Final/secciones/05-trabajos-relacionados.tex`](../../Informe-Final/secciones/05-trabajos-relacionados.tex).

## Actualización de este checklist (Fase 10)

Una versión anterior de este archivo declaraba que PRISMA "no aplica a este proyecto" porque no existía
ninguna revisión de literatura. Eso dejó de ser cierto: la Sección 5 del informe final ahora documenta
explícitamente un procedimiento de búsqueda (24 consultas dirigidas, técnica de *snowballing* de Wohlin
(2014), un diagrama de flujo de 4 etapas adaptado de PRISMA, y una tabla de 13 trabajos
comparados) — es decir, el capítulo **sí sigue un procedimiento explícito de búsqueda**, la condición
exacta que la guía de Fase 10 usa para exigir este checklist. Este archivo se reescribe para evaluar ese
procedimiento real contra los ítems de PRISMA 2020, en vez de declarar la no aplicabilidad.

## Nota de honestidad metodológica (heredada del propio informe)

El informe mismo declara, antes de presentar resultados, que esto **no es una revisión sistemática de
literatura (SLR) completa** en el sentido de Kitchenham y Charters: no hubo acceso a bases de datos
académicas indexadas (Scopus, IEEE Xplore, ACM DL) con conteos exportables por cadena de búsqueda, no hay
protocolo pre-registrado, y no hubo doble revisor independiente. Es una **búsqueda dirigida y honesta,
reportada con la disciplina de PRISMA en la medida de lo posible**. Este checklist evalúa exactamente eso:
qué tan bien la Sección 5 cumple la disciplina de reporte de PRISMA dado ese alcance limitado — no certifica
que el proyecto contenga una SLR formal, porque no la contiene y el propio informe lo dice primero.

## Evaluación por sección de PRISMA 2020

| Ítem PRISMA 2020 | Cumple | Evidencia / justificación |
|---|---|---|
| **Título** — identificar el reporte como revisión | 🔴 No | El capítulo se titula "Trabajos Relacionados", no "revisión sistemática" — consistente con no reclamar el estatus de SLR formal |
| **Resumen estructurado** | 🔴 No aplica | No hay un abstract independiente de la sección; el resumen ejecutivo del informe (Sección 1) no desglosa metodología de búsqueda por separado |
| **Justificación (Introduction)** | ✅ Sí | La subsección "Por qué no hay trabajos previos sobre 'sistemas de gestión de pre-sustentaciones'" explica el vacío de literatura y por qué la comparación se reorienta a técnicas (JWT, CI, dependencias vulnerables, revisión de código, trazabilidad) en vez del dominio de aplicación |
| **Objetivos (formato PICO o equivalente)** | 🟡 Parcial | No hay una pregunta PICO formal, pero el objetivo implícito es explícito en prosa: comparar las técnicas de ingeniería usadas en este proyecto contra evidencia empírica publicada sobre esas mismas técnicas |
| **Criterios de elegibilidad** | 🟡 Parcial | Declarados de forma narrativa ("verificación cruzada con 2+ fuentes independientes por cita", cita completa verificable con autores/año/venue/DOI cuando disponible) — no como una tabla PICOS formal separada |
| **Fuentes de información** | 🟡 Parcial | Declara explícitamente que **no** se usaron bases de datos académicas indexadas directamente — la búsqueda fue vía motor de búsqueda web general + snowballing. Es una limitación reconocida, no omitida |
| **Estrategia de búsqueda completa y reproducible** | 🔴 No | Se reporta el número de consultas (24) pero no las cadenas de búsqueda exactas ni las fechas de ejecución — no es reproducible palabra por palabra, a diferencia de lo que exige PRISMA para una SLR formal |
| **Proceso de selección (screening)** | ✅ Sí | Diagrama de flujo de 4 etapas: identificación (24 consultas, ~200 resultados) → cribado (32 trabajos con cita verificable) → elegibilidad (32 retenidos, 0 excluidos por irrelevancia) → incluidos en comparación directa (13) |
| **Proceso de extracción de datos** | 🟡 Parcial | La Tabla de comparación extrae de forma consistente: dominio, venue, y relación con el proyecto para cada uno de los 13 trabajos — pero no hay un formulario de extracción formal documentado ni doble extractor |
| **Métodos de síntesis** | ✅ Sí | Síntesis narrativa (no meta-análisis, correctamente — los 13 trabajos no son comparables cuantitativamente entre sí): cada fila de la tabla conecta el hallazgo empírico del trabajo citado con una decisión de diseño concreta de este proyecto |
| **Evaluación de riesgo de sesgo de los estudios incluidos** | 🔴 No | No se evaluó formalmente el riesgo de sesgo de los 13 trabajos citados (p. ej. con una herramienta como el ROBIS) — limitación no discutida en el informe más allá de la nota general de rigor |
| **Diagrama de flujo PRISMA** | ✅ Sí | Presente y explícitamente rotulado como "adaptado de PRISMA 2020 a las limitaciones reales de acceso a bases de datos de este equipo" (Figura del capítulo 5) |
| **Resultados de estudios individuales** | ✅ Sí | Cada fila de la tabla resume la relación específica del trabajo citado con este proyecto, no solo una lista de referencias |
| **Limitaciones** | ✅ Sí | Declaradas dos veces: en el propio capítulo 5 (nota de honestidad metodológica) y de nuevo en la Sección de Amenazas a la Validez (`12-amenazas-validez.tex`), evitando que la limitación quede enterrada en un solo lugar |
| **Financiamiento / conflictos de interés** | ✅ Sí (no aplica) | Proyecto académico de pregrado sin financiamiento externo; declarado en la sección de declaraciones del informe (`15-declaraciones.tex`) |
| **Registro y protocolo (pre-registro)** | 🔴 No | No hubo protocolo pre-registrado (p. ej. en PROSPERO) — esperado, dado que esto no se presenta como una SLR formal |

## Resumen

**6/16 ítems evaluados cumplidos, 4 parciales, 6 no cumplidos.** El patrón es consistente con lo que el
informe declara de sí mismo: fuerte en las partes que SÍ se hicieron con disciplina (diagrama de flujo de
selección, síntesis narrativa conectada a decisiones de diseño reales, declaración explícita y repetida de
limitaciones), débil exactamente en las partes que requieren infraestructura de investigación formal que
este equipo no tuvo (bases de datos indexadas, protocolo pre-registrado, doble revisor, evaluación de
riesgo de sesgo). No se recomienda reclamar cumplimiento total de PRISMA 2020 en ningún resumen ejecutivo
del proyecto — la clasificación correcta es "búsqueda dirigida con disciplina de reporte parcialmente
adaptada de PRISMA 2020", que es exactamente como el propio Capítulo 5 ya se autodescribe.
