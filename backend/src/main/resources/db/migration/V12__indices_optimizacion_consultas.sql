-- Índices agregados a partir del análisis de consultas costosas con
-- EXPLAIN (ANALYZE, BUFFERS, TIMING, SUMMARY) sobre la base de datos real
-- (1,010,242 registros). Ver docs/mediciones/perf/OPTIMIZACION-CONSULTAS.xlsx
-- para las mediciones antes/después de cada uno.

-- tutoria_mensajes (172,489 filas) no tenía ningún índice sobre fase_id:
-- listar los mensajes de una fase de tutoría forzaba un Seq Scan completo
-- de la tabla más grande del sistema. ~13.8 ms -> ~0.23 ms (60x) tras el índice.
CREATE INDEX IF NOT EXISTS ix_tutoria_mensajes_fase
    ON presus.tutoria_mensajes (fase_id);

-- evaluaciones_jurado (46,200 filas) no tenía índice sobre jurado_id:
-- consultar "mis evaluaciones registradas" (patrón de acceso frecuente de
-- un docente-jurado) forzaba un Seq Scan. ~5.1 ms -> ~0.22 ms (23x) tras el índice.
CREATE INDEX IF NOT EXISTS ix_evaluaciones_jurado_jurado
    ON presus.evaluaciones_jurado (jurado_id);

-- notificaciones (105,131 filas): listar las no leídas ordenadas por fecha
-- generaba Seq Scan + Sort (con desborde a disco: "external merge"). Este
-- índice compuesto solo da una mejora modesta (~5%) porque el filtro
-- leida=false tiene baja selectividad (~33% de la tabla) y Postgres prefiere
-- Bitmap Scan + Sort en vez de un recorrido ordenado por índice. Se deja
-- documentado como caso honesto de "el índice no siempre elimina el costo"
-- -- ver docs/mediciones/perf/OPTIMIZACION-CONSULTAS.xlsx, hoja
-- notificaciones_no_leidas, para el detalle y la interpretación.
CREATE INDEX IF NOT EXISTS ix_notificaciones_leida_fecha
    ON presus.notificaciones (leida, fecha DESC);

-- Resto de columnas de filtro frecuentes sin índice, detectadas con el mismo
-- método (EXPLAIN sin índice -> Seq Scan -> CREATE INDEX -> Index/Bitmap Scan).
-- Detalle de las 5 ejecuciones antes/después de cada una en
-- docs/mediciones/perf/OPTIMIZACION-CONSULTAS.xlsx (Consultas 4-10).
CREATE INDEX IF NOT EXISTS ix_usuarios_rol
    ON presus.usuarios (rol_id);
CREATE INDEX IF NOT EXISTS ix_estudiante_carrera
    ON presus.estudiante (carrera_id);
CREATE INDEX IF NOT EXISTS ix_docente_facultad
    ON presus.docente (facultad_id);
CREATE INDEX IF NOT EXISTS ix_evaluadores_docente
    ON presus.evaluadores (docente_id);
CREATE INDEX IF NOT EXISTS ix_miembros_tribunal_docente
    ON presus.miembros_tribunal (docente_id);
CREATE INDEX IF NOT EXISTS ix_tutores_docente
    ON presus.tutores (docente_id);
CREATE INDEX IF NOT EXISTS ix_anteproyectos_estado
    ON presus.anteproyectos (estado);
