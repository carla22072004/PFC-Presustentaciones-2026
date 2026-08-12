# Cobertura de pruebas (JaCoCo) — datos reales

**Cómo se generó:** `cd backend && ./mvnw test`, luego `backend/target/site/jacoco/jacoco.csv` (reporte generado localmente, no versionado — se regenera en cada corrida).

Una versión anterior de este documento (y el badge de `README.md`) afirmaba `>60%` de cobertura sin que existiera ni una sola clase de prueba en el repositorio. Esa cifra era falsa. Estos son los números reales tras ejecutar la suite de pruebas:

| Métrica | Cobertura real |
|---|---|
| Instrucciones | 1.65% (421 / 25,503) |
| Líneas | 2.83% (86 / 3,036) |

## Clases con cobertura real

| Clase | Líneas cubiertas |
|---|---|
| `security.jwt.JwtTokenProvider` | 27/27 (100%) |
| `services.UsuarioServiceImpl` | 34/50 (68%) |
| `entities.Usuario` | 12/18 |
| `services.RubricaEvaluacionServiceImpl` (`calcularNotaTribunal`) | 9/199 |

## Suite actual (15 pruebas, todas en verde)

- `PreSustentacionesApplicationTests` — smoke test de arranque del contexto Spring.
- `JwtTokenProviderTest` — generación/validación de JWT.
- `UsuarioServiceImplTest` — CRUD de usuarios, encriptado de contraseña con BCrypt, asignación de `rolUsuario`, rechazo de email duplicado.
- `RubricaEvaluacionServiceImplTest` — cálculo real del promedio de notas del tribunal (`calcularNotaTribunal`), incluyendo redondeo a 2 decimales y caso sin evaluaciones.

## Por qué es baja y qué falta

El backend tiene 21 controladores y ~20 servicios; solo 2 servicios tienen pruebas unitarias reales. La cobertura baja es honesta, no un fallo de ejecución: refleja que la mayoría de la lógica (controladores REST, generación de PDF, procedimientos almacenados, flujo de tutorías/actas) todavía no tiene pruebas automatizadas. Ampliar esta suite (especialmente `SolicitudServiceImpl`, con las reglas de transición de estados) es el trabajo pendiente más valioso para la siguiente entrega.
