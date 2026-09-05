# Cobertura de pruebas (JaCoCo) — datos reales

**Cómo se generó:** `cd backend && ./mvnw clean verify` (JaCoCo corre en la fase `test` vía `jacoco-maven-plugin`, ver `backend/pom.xml`).
**Reporte crudo archivado (HTML + XML + CSV):** [`docs/mediciones/jacoco/2026-09-05/`](2026-09-05/) — cifra de cierre vigente, corrida sobre Postgres/Redis reales en Docker (228 tests, 29 clases de prueba, 0 fallos/errores); `2026-08-30/`, `2026-08-29/` y `2026-08-17/` se conservan como snapshots históricos. El reporte también se regenera y publica como artefacto en el job `backend` de [`.github/workflows/ci.yml`](../../../.github/workflows/ci.yml) en cada push.
**Última actualización:** 2026-09-05 — corrida de cierre para unificar la cifra de cobertura, que hasta esta fecha aparecía contradicha dentro del propio informe académico (37,1% líneas en 5 lugares del documento, 38,88% en otros 3; ninguna de las dos ya era la vigente). Esta es ahora **la única cifra válida**, reemplaza a las dos anteriores en todo el documento académico y en cualquier otro lugar del repositorio que las cite.

## ⚠️ Corrección de cifra (2026-09-05): la que estaba en el informe académico ya no es la vigente

Antes de esta fecha, `ChatbotController`, `ChatbotService` y `ReporteServiceImpl` se habían agregado al backend **después** de que se generara el snapshot `2026-08-30/`, así que JaCoCo nunca los había medido — la cifra publicada (35,10% instrucciones / 38,88% líneas / 23,06% ramas) estaba desactualizada respecto al código real incluso antes de que el informe la citara. La corrida de hoy (`2026-09-05/`) sí los incluye:

| Métrica | 2026-08-30 (obsoleta) | **2026-09-05 (vigente)** |
|---|---|---|
| Instrucciones | 35.10% (5,699 / 16,237) | **44.39%** (9,303 / 20,957) |
| Líneas | 38.88% (1,173 / 3,017) | **48.03%** (1,866 / 3,885) |
| Ramas | 23.06% (255 / 1,106) | **32.07%** (506 / 1,578) |
| Controladores (líneas / ramas) | 8.75% / 0.00% | **21.18% / 12.35%** |
| Tests / archivos | 109 / 15 | **228 / 29** |

La cobertura global subió porque se agregaron 119 tests nuevos reales en 14 clases (`RubricaEvaluacionServiceImplTest`, `EvaluacionJuradoServiceTest`, `EvaluacionServiceImplTest`, `UsuarioServiceImplTest`, `ReporteServiceImplTest` y otras — 228 tests / 29 archivos en total hoy, frente a 109/15 el 30-08), no por un cambio de denominador favorable. **`ChatbotController` y `ChatbotService` siguen en 0%: no existe ningún archivo de test para ninguno de los dos** (verificado en esta corrida — cero `*Chatbot*` en `backend/target/surefire-reports/`), así que siguen bajando el promedio del paquete de controladores. La cobertura de controladores, aunque más que duplicada, **sigue muy por debajo del 70% que exige la guía** — es el hueco más grande de la rúbrica (ver `docs/basedatos/CATALOGO-SP.md` para qué controladores exponen los procedimientos almacenados, que son la prioridad).

Una versión anterior de este documento (y el badge de `README.md`) afirmaba `>60%` de cobertura sin que existiera ni una sola clase de prueba en el repositorio. Esa cifra era falsa. La cobertura real ha fluctuado a medida que se agregan tests reales *y* código nuevo (el denominador también crece):

| Fecha | Instrucciones | Líneas | Ramas | Nº de tests |
|---|---|---|---|---|
| histórico (0 tests) | 0% | 0% | — | 0 |
| 2026-08-12 | 1.65% | 2.83% | — | 3 archivos |
| 2026-08-17 (Fase 5) | 18.13% (2,417 / 13,334) | 22.70% (559 / 2,463) | — | 46 tests / 9 archivos |
| 2026-08-17 (Fase 3) | 23.33% (3,169 / 13,581) | 28.45% (716 / 2,517) | 15.00% (129 / 860) | 61 tests / 11 archivos |
| 2026-08-29 (tests reparados, 0 nuevos) | 20.26% (3,290 / 16,237) | 24.63% (743 / 3,017) | 12.39% (137 / 1,106) | 61 tests / 11 archivos |
| 2026-08-29 (+ 4 clases de test nuevas) | 29.69% (4,820 / 16,237) | 33.51% (1,011 / 3,017) | 18.90% (209 / 1,106) | 92 tests / 15 archivos |
| 2026-08-29 (+ RF-06 `generarActa`) | 33.18% (5,388 / 16,237) | 37.06% (1,118 / 3,017) | 23.06% (255 / 1,106) | 109 tests / 15 archivos |
| 2026-08-30 (`@SpringBootTest` real) | 35.10% (5,699 / 16,237) | 38.88% (1,173 / 3,017) | 23.06% (255 / 1,106) | 109 tests / 15 archivos |
| **2026-09-05 (cierre, `mvn clean verify` sobre Docker limpio, actual)** | **44.39%** (9,303 / 20,957) | **48.03%** (1,866 / 3,885) | **32.07%** (506 / 1,578) | **228 tests / 29 archivos** |

**El porcentaje bajó del 17-08 al 29-08 (fila intermedia) aunque el número absoluto de instrucciones/líneas cubiertas subió** (3,169→3,290 instrucciones, 716→743 líneas): entre esas dos fechas se agregó código de producción real (nuevos módulos/controladores) sin tests proporcionales, así que el denominador creció más rápido que la cobertura. Después se agregaron 31 tests reales nuevos (`PermisoServiceTest`, `NotificacionServiceImplTest`, `EvaluacionJuradoServiceTest`, `ActaServiceImplTest`) cubriendo 4 clases de servicio que tenían 0% — subiendo la cobertura de líneas 8.9 puntos porcentuales de una vez. El salto del 29-08 al 30-08 (37.06%→38.88% líneas) **no** viene de tests nuevos (el número de tests/archivos no cambió) sino de que `PreSustentacionesApplicationTests` dejó de ser un `assertTrue(true)` y ahora levanta el contexto real de Spring (ver Fase 20/README), lo que ejecuta código de inicialización de beans que antes nunca corría bajo test. La cobertura de ramas (23.06%) no cambió — el contexto de Spring no ejerce ramas condicionales de lógica de negocio, solo construcción de objetos. **No se alcanza el objetivo de ≥60/70% declarado en la guía** — sigue habiendo controllers y varias clases de servicio con 0% (`EstudianteService`, `EvaluacionServiceImpl`, `TutorServiceImpl`, etc.); esta ronda priorizó agregar cobertura real y útil sobre inflar la cifra, y la brecha restante queda declarada explícitamente en vez de maquillada. No se ocultó ninguna caída — es la cifra real de `./mvnw test`, verificable en [`2026-08-30/jacoco.csv`](2026-08-30/jacoco.csv).

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

`UsuarioController` (7%), `GlobalExceptionHandler` (31%), `AuthController` (32%), y la mayoría de los 29 controladores REST no tienen tests dedicados — la suite actual se concentra en `services/` y `security/`, que es donde vive la lógica de negocio y la superficie de riesgo de seguridad. Los controladores están cubiertos indirectamente por `AuthControllerIntegrationTest` (`@WebMvcTest`), pero no exhaustivamente. (Actualizado 2026-08-29: `EvaluacionServiceImpl.calcularPromedioSp` y `ActaServiceImpl` ya tienen test unitario dedicado — ver `docs/basedatos/CATALOGO-SP.md`.)

El umbral objetivo declarado en la autoevaluación de Unidad IV era ≥60% — sigue sin alcanzarse, pero la trayectoria real (0% → 2.83% → 22.70% en instrucciones) documenta progreso genuino en vez de una cifra estática inventada.
