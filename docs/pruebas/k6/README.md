# Pruebas de carga k6 — datos reales

Una versión anterior de `run1-summary.json`, `run2-summary.json` y `run3-summary.json` contenía números inventados (incluso internamente inconsistentes: `http_req_failed.fails: 2900` junto con `value: 0.0`, matemáticamente imposible). Se reemplazaron por **2 corridas reales**, ejecutadas de verdad contra el backend corriendo en `localhost:8080` con datos reales de la base (51,426 usuarios). No hay una tercera corrida — se documentan solo las que realmente se ejecutaron.

## Cómo se ejecutaron

```bash
docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6:latest \
  run --quiet -e BASE_URL=http://host.docker.internal:8080/api - < load-test.js
```

`load-test.js` se parametrizó con `__ENV.BASE_URL` para poder apuntar al backend del host desde el contenedor de k6 (antes tenía `http://localhost:8080` hardcodeado, que dentro del contenedor apunta al propio contenedor, no al host).

## Resultados reales (2 corridas, ~2 minutos cada una: 30s ramp-up a 20 VUs, 1min en 50 VUs, 30s ramp-down)

| Métrica | Run 1 | Run 2 |
|---|---|---|
| Requests totales | 5,876 | 5,874 |
| Iteraciones | 2,938 | 2,937 |
| `http_req_duration` p95 | 80.28 ms | 80.91 ms |
| `http_req_duration` avg | 38.35 ms | 38.33 ms |
| VUs máximos | 50 | 50 |
| Umbral `p(95)<500ms` | ✅ Cumplido | ✅ Cumplido |
| Umbral `http_req_failed rate<0.01` | ❌ No cumplido (50%) | ❌ No cumplido (50%) |
| Check `login status is 200` | ✅ 100% (2,938/2,938) | ✅ 100% (2,937/2,937) |
| Check `status is 200 or 401` en `/catalogos/carreras` | ❌ 0% | ❌ 0% |

## Por qué falla el umbral `http_req_failed<1%` (hallazgo real, no error de la prueba)

El endpoint `/api/catalogos/carreras` sin token responde **403 Forbidden**, no 401 como asumía el check original del script. k6 clasifica cualquier respuesta fuera del rango 2xx/3xx como "fallida" por defecto, así que la mitad de las peticiones (las de catálogo, sin autenticar) se cuentan como fallidas — no es un error del servidor ni de la prueba, es que Spring Security no tiene un `AuthenticationEntryPoint` personalizado y por eso devuelve 403 en vez de 401 para peticiones anónimas a rutas protegidas. El login (con credenciales reales del admin sembrado) funcionó al 100% en ambas corridas. La latencia (p95 ~80ms con 50 usuarios concurrentes) es sólida.
