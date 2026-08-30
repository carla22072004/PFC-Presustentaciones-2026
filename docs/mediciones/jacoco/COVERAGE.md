# Cobertura de pruebas (JaCoCo) — datos reales

**Cómo se generó:** `cd backend && ./mvnw test` (JaCoCo corre en la fase `test` vía `jacoco-maven-plugin`, ver `backend/pom.xml`).
**Reporte crudo archivado (HTML + XML + CSV):** [`docs/mediciones/jacoco/2026-08-29/`](2026-08-29/) — snapshot real más reciente; [`2026-08-17/`](2026-08-17/) se conserva como snapshot histórico. El reporte también se regenera y publica como artefacto en el job `backend` de [`.github/workflows/ci.yml`](../../../.github/workflows/ci.yml) en cada push.
**Última actualización:** 2026-08-29 (auditoría de reproducibilidad — se repararon 17 tests que fallaban por mocks faltantes tras cambios de código posteriores al 17-08; ningún test nuevo se añadió).

Una versión anterior de este documento (y el badge de `README.md`) afirmaba `>60%` de cobertura sin que existiera ni una sola clase de prueba en el repositorio. Esa cifra era falsa. La cobertura real ha fluctuado a medida que se agregan tests reales *y* código nuevo (el denominador también crece):

| Fecha | Instrucciones | Líneas | Ramas | Nº de tests |
|---|---|---|---|---|
| histórico (0 tests) | 0% | 0% | — | 0 |
| 2026-08-12 | 1.65% | 2.83% | — | 3 archivos |
| 2026-08-17 (Fase 5) | 18.13% (2,417 / 13,334) | 22.70% (559 / 2,463) | — | 46 tests / 9 archivos |
| 2026-08-17 (Fase 3) | 23.33% (3,169 / 13,581) | 28.45% (716 / 2,517) | 15.00% (129 / 860) | 61 tests / 11 archivos |
| **2026-08-29 (actual)** | **20.26%** (3,290 / 16,237) | **24.63%** (743 / 3,017) | **12.39%** (137 / 1,106) | **61 tests / 11 archivos** |

**El porcentaje bajó del 17-08 al 29-08 aunque el número absoluto de instrucciones/líneas cubiertas subió** (3,169→3,290 instrucciones, 716→743 líneas): entre esas dos fechas se agregó código de producción real (nuevos módulos/controladores) sin tests proporcionales, así que el denominador creció más rápido que la cobertura. No se ocultó esta caída — es la cifra real de `./mvnw test` corrida el 2026-08-29, verificable en [`2026-08-29/jacoco.csv`](2026-08-29/jacoco.csv).

## Clases nuevas cubiertas por los tests agregados en la Fase 3

| Clase | Qué cubre | Motivo |
|---|---|---|
| `services/SolicitudServiceImplTest` (11 tests) | `crearSolicitud`, `crearSolicitudPorUsuario` (incluye la llamada real a `sp_generar_codigo_expediente`), reglas de transición `CREADA→ENVIADA→APROBADA/RECHAZADA`, y las reglas de `suspenderSolicitud` | Identificada como prioridad #1 en la versión anterior de este documento — era la clase de reglas de negocio más importante sin ninguna prueba |
| `services/CronogramaServiceImplTest` (4 tests) | Prerrequisitos (tribunal completo, tutoría completada) y la validación cruzada `sp_validar_conflicto_jurado` recién conectada (Fase 3) — incluye el caso de conflicto real (docente ya asignado en horario solapado) | No existía ninguna prueba de este servicio; además es el único punto del código que invoca el procedimiento de validación cruzada, así que sin este test esa conexión quedaba sin cubrir |

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
| `services/SolicitudServiceImpl` | 108/166 | 65% |
| `security/RateLimitingFilter` | 11/18 | 61% |
| `services/JuradoServiceImpl` | 101/191 | 53% |
| `services/CronogramaServiceImpl` | 49/102 | 48% |

## Clases sin cobertura real o con cobertura baja (candidatas para próxima iteración)

`UsuarioController` (7%), `GlobalExceptionHandler` (31%), `AuthController` (32%), `EvaluacionServiceImpl` (el nuevo método `calcularPromedioSp` no tiene test unitario, solo se verificó manualmente contra Docker — ver `docs/basedatos/CATALOGO-SP.md`), `ActaServiceImpl`, y la mayoría de los 21 controladores REST no tienen tests dedicados — la suite actual se concentra en `services/` y `security/`, que es donde vive la lógica de negocio y la superficie de riesgo de seguridad. Los controladores están cubiertos indirectamente por `AuthControllerIntegrationTest` (`@WebMvcTest`), pero no exhaustivamente.

El umbral objetivo declarado en la autoevaluación de Unidad IV era ≥60% — sigue sin alcanzarse, pero la trayectoria real (0% → 2.83% → 22.70% en instrucciones) documenta progreso genuino en vez de una cifra estática inventada.
