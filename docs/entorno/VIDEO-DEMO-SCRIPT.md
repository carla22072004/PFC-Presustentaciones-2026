# Guion para el video de reproducibilidad (`make all`, 5-7 min)

**Estado:** ⏳ Pendiente de grabar — este documento es el guion/checklist para grabarlo, no el
video en sí. Una vez grabado y subido, enlazar el video desde `README.md` (sección "Video de
Reproducibilidad") y actualizar el estado de este archivo.

**Por qué existe este documento en vez de solo el video:** graba una persona del equipo con su
propia voz y pantalla — no es algo que se pueda generar automáticamente. Este guion deja el
procedimiento exacto listo para que grabarlo tome ~20-30 minutos (incluyendo el tiempo real de
`make all`, que corre en paralelo mientras se explica cada paso), en vez de tener que improvisar
qué mostrar.

## Preparación antes de grabar (una sola vez)

- [ ] Verificar que no hay contenedores previos del proyecto corriendo: `docker compose down -v`
- [ ] Verificar que `.env` **no** existe todavía (o borrarlo) — el video debe mostrar el paso de crearlo desde `.env.example`, no asumirlo
- [ ] Cerrar aplicaciones que puedan interferir con el rendimiento (afecta los números de k6/Lighthouse — ver la nota de amenazas a la validez en `Informe-Final/secciones/12-amenazas-validez.tex`, sé consistente con esa honestidad también en el video: se puede mencionar en voz que la máquina no está aislada)
- [ ] Aumentar el tamaño de fuente de la terminal (legible en pantalla completa a 1080p)
- [ ] Tener a mano: el software de grabación (OBS Studio, o el grabador de pantalla nativo de Windows `Win+Alt+R`), y confirmar que graba audio del micrófono si se va a narrar en voz

## Guion minuto a minuto

| Tiempo | Qué mostrar | Qué decir (guion sugerido) |
|---|---|---|
| **0:00–0:30** | Terminal vacía, `git clone` del repositorio (o simular con `git log` mostrando que es el mismo commit que está en GitHub) | "Este es un clon limpio del repositorio, sin `.env`, sin contenedores previos. Voy a correr `make all`, el objetivo de reproducibilidad de la Fase 10." |
| **0:30–1:00** | `cp .env.example .env`, editar `.env` con un JWT_SECRET generado con `openssl rand -hex 32` | "Primer paso manual documentado en el README: generar el secreto JWT. Esto es lo único que `make all` no automatiza, porque un secreto real no debe quedar hardcodeado en el repositorio." |
| **1:00–1:15** | `make all` — mostrar el comando y el primer output (`make build`) | "`make all` compila el backend, construye el frontend de producción, levanta los 4 contenedores, espera a que el backend esté healthy, corre los tests con JaCoCo, los benchmarks de k6, la auditoría de seguridad, regenera las figuras y la matriz de trazabilidad, y compila el PDF final. Todo en una sola invocación." |
| **1:15–2:00** | *(puede acelerarse/cortarse en edición)* `docker compose up -d --build` construyendo las imágenes | "Mientras construye, adelanto el video — esto toma unos minutos la primera vez." |
| **2:00–2:30** | Salida de `wait-backend`: los reintentos contra `/actuator/health` hasta ver `Backend UP` | "Este paso confirma que las migraciones Flyway se aplicaron — no hay un paso separado de 'aplicar migraciones', Spring Boot las corre automáticamente al arrancar." |
| **2:30–3:15** | *(puede acelerarse)* Salida de `make test` — resultado final con el conteo de tests | "Corre la suite completa de JUnit, JaCoCo se genera en esta misma fase." |
| **3:15–4:00** | *(puede acelerarse)* Salida de `make bench` — las 5 corridas de k6 (o una corrida corta representativa si se decide no correr las 5 completas en el video) | "Benchmarks de carga contra el backend real que acabamos de levantar." |
| **4:00–4:30** | Salida de `make audit` (SpotBugs + npm audit) | "Análisis estático de seguridad, incluye la regla de SQL dinámico." |
| **4:30–5:00** | Salida de `make docs` (figuras + validación de trazabilidad) | "Regenera las figuras de `docs/mediciones/perf/figuras/` a partir de los datos crudos, y valida que la matriz de trazabilidad no mienta sobre qué está verificado." |
| **5:00–5:30** | Salida de `make pdf` — mensaje final "PDF generado" | "Compila el informe final completo a PDF con LaTeX." |
| **5:30–6:15** | Mensaje final de `make all` (código de salida 0), luego abrir en el explorador de archivos: `docs/mediciones/perf/figuras/`, `docs/mediciones/jacoco/`, `k6/run5-summary.json`, `Informe-Final/informe-final.pdf` | "Y aquí está la evidencia generada: figuras, cobertura, resultados de carga, y el PDF final — todo reproducible desde cero con un solo comando." |
| **6:15–7:00** | Abrir el navegador en `http://localhost` (frontend) y `http://localhost:8080/actuator/health` (backend) | "El sistema completo también queda arriba y funcional, no solo los reportes." |

## Después de grabar

- [ ] Editar solo para acelerar/cortar las esperas largas de build (nunca cortar para ocultar un error real — si algo falla, hay que arreglarlo y regrabar, no editarlo fuera)
- [ ] Subir el video (YouTube no listado, o el mecanismo que el equipo prefiera)
- [ ] Enlazar la URL real desde `README.md`
- [ ] Actualizar el "Estado" al inicio de este archivo a "✅ Grabado — ver [enlace]"
