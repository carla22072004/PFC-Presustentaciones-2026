# VOLUMEN-DATOS.md — Volumen de datos (requisito: mínimo 1,000,000 de registros)

## Estado (verificado 2026-08-23, contra el Postgres de desarrollo real, contenedor `amz-postgres` / BD `BdPresustentaciones`)

**Total: 1,003,344 registros** en el esquema `presus`, distribuidos entre 41 tablas.

Generado por [`scripts/generar-volumen-datos.sql`](../../scripts/generar-volumen-datos.sql) — ver ese archivo para la
lógica de distribución (respeta las FKs y el embudo real del dominio: `solicitud` → `tutores` → `anteproyectos` →
`cronograma` → `miembros_tribunal`/`evaluadores` → evaluaciones → `actas`). El volumen anterior (1,010,242 registros,
17-21 ago 2026) se perdió el 22 ago 2026 al recrear el volumen de Docker por un problema de contraseña de Postgres
(ver `docs/observaciones/INFORME-ERRORES-2026-08-22.md`) y **no tenía script de generación versionado** — no era
reproducible, así que esta cifra reemplaza a la anterior en vez de intentar igualarla exactamente.

Conteo exacto por tabla (no estimado — `SELECT count(*)` real sobre cada tabla):

| Tabla | Filas | Tabla | Filas |
|---|---:|---|---:|
| tutoria_mensajes | 172,489 | evaluadores | 30,803 |
| notificaciones | 145,000 | tutores | 30,801 |
| disponibilidad_sala | 95,040 | anteproyectos | 26,401 |
| evaluaciones_criterio | 92,409 | cronograma | 24,201 |
| historial_estados_solicitud | 52,806 | evaluaciones_finales | 15,401 |
| usuarios | 51,435 | evaluaciones | 15,400 |
| tutoria_fases | 49,283 | actas | 10,780 |
| miembros_tribunal | 46,203 | docente | 9,807 |
| evaluaciones_jurado | 46,203 | historial_cronograma | 3,630 |
| solicitud | 44,002 | criterios_rubrica | 120 |
| estudiante | 41,001 | (resto: catálogos — rubricas, sala, convocatorias, bloques_horarios, estados_solicitud, periodos_academicos, roles_jurado, etc.) | ~200 |

**Nota sobre la tabla `jurados`:** la versión anterior de este documento la incluía (46,200 filas) como remanente de
un esquema previo a la migración a `miembros_tribunal`. Esa tabla **ya no existe** en `V1__schema_inicial.sql` — se
eliminó en una limpieza de esquema posterior. El volumen que le correspondía se compensó aumentando `notificaciones`
(105,131 → 145,000) en la regeneración actual.

## Coherencia de la distribución

El volumen no está concentrado en una sola tabla "de relleno": sigue la
jerarquía real del dominio (`usuarios` → `estudiante`/`docente` → `solicitud`
→ `anteproyectos`/`cronograma`/`tutores`/`miembros_tribunal`/`evaluadores` →
`tutoria_fases` → `tutoria_mensajes`, `evaluaciones_criterio`, etc.), con
proporciones plausibles para un sistema de titulación real (p. ej. ~4 mensajes
de tutoría por fase, ~2 criterios evaluados por evaluador y solicitud).

## Integridad verificada

Se comprobaron 0 filas huérfanas en las relaciones más profundas de la
jerarquía:

```
estudiante → usuarios              : 0 huérfanos
solicitud → estudiante             : 0 huérfanos
tutoria_mensajes → tutoria_fases   : 0 huérfanos
evaluaciones_criterio → solicitud  : 0 huérfanos
```

## Consulta para reproducir el conteo en vivo durante la sustentación

```sql
SELECT r.tablename,
  (xpath('/row/c/text()', query_to_xml(
     format('SELECT count(*) AS c FROM presus.%I', r.tablename), false, true, ''
  )))[1]::text::bigint AS filas
FROM pg_tables r WHERE r.schemaname = 'presus'
ORDER BY filas DESC;
```
