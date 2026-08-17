# Pruebas de carga k6 — datos reales

## Historial

Una versión anterior de `run1-summary.json`, `run2-summary.json` y `run3-summary.json` contenía números inventados. Se reemplazaron por 2 corridas reales (`run1`, `run2`) contra `/api` (sin versionar, antes de implementar el versionado dinámico) y luego, tras cerrar la brecha de reproducibilidad/CI de la Unidad IV (Fase 5), se agregaron **3 corridas adicionales** (`run3`, `run4`, `run5`) para llegar a las 5 corridas independientes que exige la guía, más un análisis estadístico de caché frío vs caliente.

## Cambio de metodología entre run1/run2 y run3/run4/run5

Entre las corridas 1-2 y las corridas 3-5 el backend cambió de forma que rompe el script original:

1. **Versionado dinámico (`/api/v1/`)**: `/api/auth/login` sin versión ahora devuelve 403. `BASE_URL` se actualizó a `http://localhost:8080/api/v1`.
2. **Rate limiting en login** (6 intentos/60s → 429): el script original hacía login en **cada iteración**. Con 50 VUs concurrentes eso agota el límite casi de inmediato — el primer intento de corrida 3 con el script viejo dio **99.8% de fallos**, todos por rate limiting, no por falta de capacidad. Esa corrida se conserva como evidencia de que el rate limiter funciona: [`rate-limiter-evidence-run.json`](rate-limiter-evidence-run.json). El script se rediseñó con un `setup()` de k6 que hace **un solo login** y reutiliza el token; las iteraciones prueban el flujo real de un usuario ya autenticado.
3. **Endpoint inexistente**: el script original apuntaba a `/api/catalogos/carreras`, que **nunca existió** en el backend (`CatalogoController` solo expone `/modalidades`, `/convocatorias` y `/convocatoria-activa`). Como la petición iba sin token, siempre devolvía 403 antes de llegar al enrutador, por lo que el 404 real nunca se notó. Se corrigió a `/catalogos/modalidades` (endpoint real) y se agregó `/universidades` (el endpoint con caché Redis, Requisito E2) para ejercitar más superficie de la API.

## Cómo se ejecutaron (runs 3-5)

```bash
# k6 nativo (winget install GrafanaLabs.k6), backend real en localhost:8080
# (postgres + redis en Docker, backend con mvnw spring-boot:run)
k6 run --quiet --summary-export=runN-summary.json load-test.js
```

## Resultados reales (5 corridas, ~2 minutos cada una: 30s ramp-up a 20 VUs, 1min en 50 VUs, 30s ramp-down)

| Métrica | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 |
|---|---|---|---|---|---|
| Metodología | login por iteración, `/api` sin versión | login por iteración, `/api` sin versión | login único (`setup()`), `/api/v1` | login único (`setup()`), `/api/v1` | login único (`setup()`), `/api/v1` |
| Requests totales | 5,876 | 5,874 | 6,231 | 6,237 | 6,241 |
| `http_req_duration` p95 | 80.28 ms | 80.91 ms | 7.70 ms | 7.39 ms | 7.47 ms |
| `http_req_duration` avg | 38.35 ms | 38.33 ms | 7.48 ms | 6.26 ms | 6.30 ms |
| `http_req_failed` | 50%* | 50%* | 0% | 0% | 0% |
| Umbral `p(95)<500ms` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Umbral `http_req_failed<1%` | ❌* | ❌* | ✅ | ✅ | ✅ |

\* En run1/run2 la mitad de las peticiones eran a `/catalogos/carreras` sin token (403 Forbidden, contado como fallo por k6) — comportamiento documentado, no error del servidor. Runs 3-5 usan un endpoint real con token válido y no reproducen ese fallo.

La latencia bajó notablemente entre run1/2 y run3-5 (p95 de ~80ms a ~7.5ms) principalmente porque las corridas nuevas no repiten `POST /auth/login` (con hashing BCrypt, el paso más costoso) en cada iteración — es la comparación esperada entre "login en cada request" vs "sesión ya autenticada", no una mejora de infraestructura entre corridas.

## Análisis estadístico: caché fría vs caché caliente (`GET /api/v1/universidades`)

Requisito: comparar los escenarios de caché fría (sin entrada en Redis, dispara la llamada real a la API externa de Hipo Labs) y caché caliente (respuesta servida desde Redis, TTL 10 min) con percentiles, media, IC 95% y un test no paramétrico (Wilcoxon/Mann-Whitney) con tamaño de efecto.

**Método:** 30 muestras independientes por escenario. Frío: `redis-cli DEL universidades::ecuador` inmediatamente antes de cada petición (fuerza una llamada real a la API externa en cada muestra). Caliente: una petición de precalentamiento, luego 30 peticiones consecutivas dentro del TTL de 10 minutos. Mismo token JWT, mismo proceso backend, misma máquina. Datos crudos: [`cache-cold-samples.txt`](cache-cold-samples.txt), [`cache-warm-samples.txt`](cache-warm-samples.txt).

| Métrica | Caché fría (n=30) | Caché caliente (n=30) |
|---|---|---|
| Media | 109.62 ms | 7.86 ms |
| Desv. estándar | 18.88 ms | 0.75 ms |
| IC 95% de la media | [102.57, 116.67] ms | [7.58, 8.14] ms |
| p50 (mediana) | 106.02 ms | 7.60 ms |
| p90 | 107.04 ms | 8.42 ms |
| p95 | 108.07 ms | 8.89 ms |
| p99 | 180.31 ms | 10.50 ms |
| Mínimo / Máximo | 104.96 / 209.51 ms | 7.27 / 11.15 ms |

**Test de Wilcoxon rank-sum (Mann-Whitney U), dos muestras independientes:**

- U = 0 (de un máximo posible de 900 = 30×30) — **separación perfecta**, ninguna muestra fría fue más rápida que ninguna muestra caliente.
- z = −6.65, **p < 0.0001** (dos colas) — la diferencia es estadísticamente significativa muy por debajo de α=0.05.
- Correlación biserial de rangos (tamaño de efecto) r = **1.00** — efecto máximo posible según las convenciones de Cohen para r (>0.5 ya se considera grande).

**Conclusión:** la caché Redis reduce la latencia de este endpoint en ~14× (109.6 ms → 7.9 ms de media), con una diferencia estadísticamente significativa y de tamaño de efecto máximo. Esto valida cuantitativamente el Requisito E2 (caché Redis activa con TTL).

### Cómo se reprodujo

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@uteq.edu.ec","password":"admin123"}' \
  | node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>console.log(JSON.parse(d).data.auth.token))")

# Frio: flush + request, 30 veces
for i in $(seq 1 30); do
  docker exec <contenedor-redis> redis-cli DEL "universidades::ecuador" > /dev/null
  curl -s -o /dev/null -w "%{time_total}\n" http://localhost:8080/api/v1/universidades \
    -H "Authorization: Bearer $TOKEN"
done > cache-cold-samples.txt

# Caliente: precalentar + 30 requests consecutivos
curl -s -o /dev/null http://localhost:8080/api/v1/universidades -H "Authorization: Bearer $TOKEN"
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{time_total}\n" http://localhost:8080/api/v1/universidades \
    -H "Authorization: Bearer $TOKEN"
done > cache-warm-samples.txt
```

Percentiles, IC 95% (aproximación t, df=29) y el test de Mann-Whitney/Wilcoxon (con corrección por empates y aproximación normal para el p-valor) se calcularon con un script de Node sin dependencias externas.
