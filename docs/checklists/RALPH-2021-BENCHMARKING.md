# Checklist — Ralph 2021, Estándar complementario "Benchmarking"

**Referencia:** ACM SIGSOFT Empirical Standards — "Benchmarking" (evalúa mediciones de rendimiento/comparación: ¿el benchmark mide lo que dice medir, de forma justa y reproducible?).
**Por qué aplica:** las corridas de k6 (carga), Lighthouse (rendimiento web) y OWASP ZAP (seguridad dinámica) son, metodológicamente, benchmarks del sistema.

| Criterio | Cumple | Evidencia / justificación |
|---|---|---|
| El benchmark mide algo bien definido, no una mezcla ambigua de factores | ✅ Sí | El análisis de caché fría/caliente aísla una sola variable (estado de Redis) manteniendo todo lo demás constante — ver `k6/README.md` |
| Se ejecutan múltiples corridas, no una sola muestra | ✅ Sí | 5 corridas de k6, 6 de Lighthouse, 30 muestras por escenario en el análisis de caché |
| Se reporta variabilidad (no solo un promedio) | ✅ Sí | Desviación estándar, IC 95% y percentiles p50/p90/p95/p99 reportados en el análisis de caché; p95 reportado en cada corrida de k6 |
| El entorno de medición está documentado (hardware, SO, versiones) | ✅ Sí | `docs/entorno/versions.txt`, y notas puntuales sobre la máquina de pruebas en `LIGHTHOUSE-REPORT.md` |
| El baseline/comparación es justo (no se compara peras con manzanas) | 🟡 Parcial | El análisis de caché fría/caliente es una comparación justa (mismo endpoint, mismo token, misma máquina, única variable el caché). Las corridas k6 run1-2 vs run3-5 **no** son directamente comparables entre sí (cambiaron endpoint, versión de API y metodología de login) — el propio reporte lo declara explícitamente para no inducir una lectura errónea |
| Se usa una prueba estadística apropiada para la comparación, no solo diferencia visual de promedios | ✅ Sí | Wilcoxon rank-sum (Mann-Whitney U) con tamaño de efecto (correlación biserial de rangos), apropiado para dos muestras independientes no necesariamente normales — ver `k6/README.md` |
| El benchmark evita medir el "camino feliz" únicamente | 🟡 Parcial | El escaneo ZAP y find-sec-bugs sí buscan activamente fallos (no solo el camino exitoso). k6 mide principalmente el camino exitoso (200 OK); no se diseñaron escenarios de estrés hasta el punto de falla (breaking point) ni pruebas de caos |
| Los resultados del benchmark son reproducibles con los comandos documentados | ✅ Sí | Todos los comandos exactos están en `k6/README.md`, `LIGHTHOUSE-REPORT.md` y `OWASP-AUDIT.md` |

## Resumen

6/8 criterios cumplidos, 2 parciales. El punto más fuerte es el análisis estadístico de caché (variable única, prueba no paramétrica apropiada, tamaño de efecto) — cumple el estándar de benchmarking con rigor real. La brecha más clara: no se probó el sistema hasta su punto de quiebre (breaking point testing), solo carga moderada (50 VUs) dentro de un rango donde el sistema ya se sabe que responde bien.
