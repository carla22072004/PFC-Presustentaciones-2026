# 📖 DICCIONARIO DE DATOS DE MEDICIONES Y VARIABLES DE PRUEBA

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Documento:** `docs/mediciones/DATA-DICTIONARY.md`  
**Propósito:** Explicar exhaustivamente la semántica, unidades de medida, tipo de dato y rangos aceptables de todas las variables recolectadas durante las pruebas empíricas (k6, JaCoCo, SUS, OWASP y Lighthouse).  

---

## 📌 1. Variables de Pruebas de Carga (k6)

| Variable | Tipo de Dato | Unidad de Medida | Descripción | Rango Aceptable |
|---|---|---|---|---|
| `http_req_duration` | Double | Milisegundos (ms) | Tiempo total desde la emisión de la solicitud HTTP hasta la recepción completa de la respuesta. | `< 500 ms` (p95) |
| `http_req_failed` | Float / Rate | Porcentaje (%) | Proporción de solicitudes HTTP que resultaron en código de estado de error (4xx o 5xx). | `< 1.0 %` |
| `http_reqs` | Integer | Reqs / Segundo | Tasa total de peticiones procesadas exitosamente por segundo (*Throughput*). | `>= 20.0 req/s` |
| `iterations` | Integer | Conteo | Cantidad de iteraciones completas del script de simulación ejecutadas por los usuarios virtuales. | N/A |
| `vus` | Integer | Usuarios | Cantidad de usuarios virtuales (*Virtual Users*) concurrentes simulados durante la prueba. | `1 - 50 VUs` |

---

## 📌 2. Variables de Cobertura de Código (JaCoCo)

| Variable | Tipo de Dato | Unidad de Medida | Descripción | Rango Aceptable |
|---|---|---|---|---|
| `LINE_COVERAGE` | Float | Porcentaje (%) | Porcentaje de líneas de código ejecutable cubiertas por pruebas unitarias automatizadas. | `>= 60.0 %` |
| `BRANCH_COVERAGE` | Float | Porcentaje (%) | Porcentaje de bifurcaciones condicionales (`if`, `switch`) evaluadas en ambos sentidos. | `>= 50.0 %` |
| `INSTRUCTION_COVERAGE` | Float | Porcentaje (%) | Porcentaje de instrucciones de bytecode Java (JVM) ejecutadas durante los tests. | `>= 65.0 %` |
| `COMPLEXITY` | Integer | Conteo Cyclomatic | Complejidad ciclomática total acumulada por método o clase. | `< 15 por método` |

---

## 📌 3. Variables de Usabilidad (Cuestionario SUS)

| Variable | Tipo de Dato | Unidad de Medida | Descripción | Rango Aceptable |
|---|---|---|---|---|
| `SUS_ITEM_SCORE` | Integer | Puntos Likert (1 - 5) | Calificación individual otorgada a cada uno de los 10 ítems de la escala SUS. | 1 a 5 |
| `SUS_GLOBAL_SCORE` | Double | Puntos (0 - 100) | Calificación consolidada calculada mediante la fórmula normalizada de Brooke (1996). | `>= 75.0 / 100` |
| `USABILITY_GRADE` | String | Categoría | Clasificación cualitativa correspondiente al rango de nota SUS (A+, A, B, C, D, F). | `Grado A / A+` |

---

## 📌 4. Variables de Métricas de Frontend (Lighthouse Core Web Vitals)

| Variable | Tipo de Dato | Unidad de Medida | Descripción | Rango Aceptable |
|---|---|---|---|---|
| `LIGHTHOUSE_PERFORMANCE` | Integer | Puntos (0 - 100) | Calificación sintética de velocidad y rendimiento de carga. | `>= 80 / 100` |
| `LIGHTHOUSE_ACCESSIBILITY` | Integer | Puntos (0 - 100) | Calificación de cumplimiento de estándares de accesibilidad web (WCAG 2.1 AA). | `>= 90 / 100` |
| `LIGHTHOUSE_BEST_PRACTICES` | Integer | Puntos (0 - 100) | Calificación de adopción de prácticas de desarrollo web modernas y seguras. | `>= 90 / 100` |
| `FCP (First Contentful Paint)` | Double | Segundos (s) | Tiempo en que el navegador renderiza el primer bloque de contenido DOM. | `< 1.8 s` |
| `LCP (Largest Contentful Paint)`| Double | Segundos (s) | Tiempo en que se renderiza el elemento visual de mayor tamaño en pantalla. | `< 2.5 s` |
| `CLS (Cumulative Layout Shift)` | Double | Índice sin unidad | Grado de desplazamiento inesperado de elementos de diseño durante la carga. | `< 0.1` |
