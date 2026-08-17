# Cobertura de pruebas (JaCoCo) — datos reales

**Cómo se generó:** `cd backend && ./mvnw test` (JaCoCo corre en la fase `test` vía `jacoco-maven-plugin`, ver `backend/pom.xml`), luego `backend/target/site/jacoco/jacoco.csv` (reporte generado localmente, no versionado — se regenera en cada corrida; también se genera y publica como artefacto en el job `backend` de [`.github/workflows/ci.yml`](../../../.github/workflows/ci.yml)).
**Última actualización:** 2026-08-17, tras la Fase 5 (46 tests reales, incluye `Phase4FeaturesTest`).

Una versión anterior de este documento (y el badge de `README.md`) afirmaba `>60%` de cobertura sin que existiera ni una sola clase de prueba en el repositorio. Esa cifra era falsa. La cobertura real ha ido subiendo a medida que se agregan tests reales:

| Fecha | Instrucciones | Líneas | Nº de tests |
|---|---|---|---|
| histórico (0 tests) | 0% | 0% | 0 |
| 2026-08-12 | 1.65% | 2.83% | 3 archivos |
| **2026-08-17 (actual)** | **18.13%** (2,417 / 13,334) | **22.70%** (559 / 2,463) | 46 tests / 9 archivos |

## Clases con mejor cobertura real

| Clase | Líneas cubiertas | % |
|---|---|---|
| `security/dto/LoginRequest` | 3/3 | 100% |
| `security/dto/LoginResponse` | 9/11 | 82% |
| `security/RateLimiterService` | 9/11 | 82% |
| `security/jwt/JwtAuthenticationFilter` | 18/23 | 78% |
| `services/AnteproyectoServiceImpl` | 69/96 | 72% |
| `services/TutoriaServiceImpl` | 171/243 | 70% |
| `services/ExternalApiServiceImpl` | 32/47 | 68% |
| `services/UsuarioServiceImpl` | 34/50 | 68% |
| `security/jwt/JwtTokenProvider` | 57/87 | 66% |
| `security/RateLimitingFilter` | 11/18 | 61% |
| `services/JuradoServiceImpl` | 101/191 | 53% |

## Clases sin cobertura real o con cobertura baja (candidatas para próxima iteración)

`UsuarioController` (7%), `GlobalExceptionHandler` (31%), `AuthController` (32%), y la mayoría de los 21 controladores REST no tienen tests dedicados — la suite actual se concentra en `services/` y `security/`, que es donde vive la lógica de negocio y la superficie de riesgo de seguridad. Los controladores están cubiertos indirectamente por `AuthControllerIntegrationTest` (`@WebMvcTest`), pero no exhaustivamente.

El umbral objetivo declarado en la autoevaluación de Unidad IV era ≥60% — sigue sin alcanzarse, pero la trayectoria real (0% → 2.83% → 22.70% en instrucciones) documenta progreso genuino en vez de una cifra estática inventada.
