-- =============================================================================
-- V25__sincronizar_secuencias_catalogos.sql
-- Mismo bug que V14 (roles_usuario) y V17 (facultades/carreras/modalidades):
-- estas 8 tablas de catálogo se siembran con id explícito por SQL crudo (V1),
-- sin pasar por Hibernate, así que sus secuencias nunca avanzaron y quedaron en
-- el arranque (last_value=1, is_called=false). Confirmado contra la base real
-- (2026-09-04): las 8 secuencias siguen en el valor inicial mientras MAX(id) va
-- de 2 a 9 en cada tabla. El código Java las usa con el patrón lazy-create
-- findByCodigo(...).orElseGet(() -> repo.save(nuevo)) en SolicitudServiceImpl,
-- JuradoServiceImpl, EvaluacionServiceImpl, TutorServiceImpl y ActaServiceImpl:
-- hoy no falla porque todos los códigos que el código busca ya están sembrados,
-- pero el primer código no sembrado que se cree en producción dispara
-- nextval() = 1 y choca con la fila id=1 existente ("duplicate key value
-- violates unique constraint"), igual que el error que forzó V14.
--
-- A diferencia de una columna serial/IDENTITY normal, estas secuencias se
-- crearon con CREATE SEQUENCE suelto en V1 y nunca quedaron con OWNED BY su
-- columna (confirmado vía pg_depend: deptype='n', no 'a') -- por eso
-- pg_get_serial_sequence() devuelve NULL para estas 8 tablas hoy. Se establece
-- la relación de propiedad primero (metadato puro, no toca datos ni IDs) para
-- poder resolver el nombre de la secuencia dinámicamente en vez de asumirlo,
-- y para que herramientas estándar de Postgres (pg_dump, pg_get_serial_sequence)
-- la reconozcan correctamente de aquí en adelante.
--
-- COALESCE(MAX(id), 0) + 1 con is_called=false (mismo patrón de V14) entrega
-- exactamente el próximo id libre en el primer nextval(), y maneja una tabla
-- vacía sin casos especiales: MAX(id) sería NULL, COALESCE la vuelve 0, y el
-- resultado es 1 -- el mismo valor con el que arrancaría una secuencia nueva.
-- =============================================================================

ALTER SEQUENCE presus.estados_solicitud_seq OWNED BY presus.estados_solicitud.id;
ALTER SEQUENCE presus.estados_proceso_seq OWNED BY presus.estados_proceso.id;
ALTER SEQUENCE presus.estados_cronograma_seq OWNED BY presus.estados_cronograma.id;
ALTER SEQUENCE presus.jornadas_seq OWNED BY presus.jornadas.id;
ALTER SEQUENCE presus.tipos_evaluador_seq OWNED BY presus.tipos_evaluador.id;
ALTER SEQUENCE presus.tipos_mensaje_seq OWNED BY presus.tipos_mensaje.id;
ALTER SEQUENCE presus.roles_jurado_seq OWNED BY presus.roles_jurado.id;
ALTER SEQUENCE presus.resultados_evaluacion_seq OWNED BY presus.resultados_evaluacion.id;

SELECT setval(pg_get_serial_sequence('presus.estados_solicitud', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.estados_solicitud), false);
SELECT setval(pg_get_serial_sequence('presus.estados_proceso', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.estados_proceso), false);
SELECT setval(pg_get_serial_sequence('presus.estados_cronograma', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.estados_cronograma), false);
SELECT setval(pg_get_serial_sequence('presus.jornadas', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.jornadas), false);
SELECT setval(pg_get_serial_sequence('presus.tipos_evaluador', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.tipos_evaluador), false);
SELECT setval(pg_get_serial_sequence('presus.tipos_mensaje', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.tipos_mensaje), false);
SELECT setval(pg_get_serial_sequence('presus.roles_jurado', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.roles_jurado), false);
SELECT setval(pg_get_serial_sequence('presus.resultados_evaluacion', 'id'),
              (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.resultados_evaluacion), false);
