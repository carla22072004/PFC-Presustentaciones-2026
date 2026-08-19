-- =============================================================================
-- V3__stored_procedures_validacion_y_codigos.sql
-- Completa las 5 categorías funcionales de procedimientos almacenados exigidas por la
-- guía (Fase 3 / Bloque A.2): V2 ya cubre consultas multi-tabla
-- (sp_generar_reporte_defensas), cálculos agregados (sp_calcular_promedio_evaluacion) y
-- actualizaciones masivas (sp_asignar_jurado_masivo, sp_firmar_acta_digital). Faltaban
-- validaciones cruzadas y generación de códigos secuenciales -- se agregan aquí.
--
-- Nota sobre PROCEDURE + INOUT en vez de FUNCTION: probando la conexión real desde JPA
-- (Fase 3) se encontró que Hibernate invoca los métodos @Procedure/@NamedStoredProcedureQuery
-- con la sintaxis JDBC "{call proc(?, ?)}" (CALL literal), y Postgres rechaza esa sintaxis
-- para FUNCTION ("... is not a procedure. Hint: To call a function, use SELECT"), sin
-- importar cuántos parámetros IN/OUT se declaren. Por eso estos dos, a diferencia de
-- sp_calcular_promedio_evaluacion y sp_generar_reporte_defensas (que SÍ son FUNCTION porque
-- retornan un conjunto de filas, no un escalar, y se leen con @NamedStoredProcedureQuery +
-- @SqlResultSetMapping en vez de CALL), se implementan como PROCEDURE con parámetro INOUT
-- para el valor de retorno escalar -- coincide exactamente con la sintaxis CALL que Hibernate
-- genera y con lo que Postgres acepta.
-- =============================================================================

-- 5. Procedimiento de VALIDACIÓN CRUZADA: verifica que un docente no quede asignado como
-- jurado en dos defensas cuyos horarios se solapen (cruza miembros_tribunal con
-- cronograma, dos tablas distintas, antes de confirmar una asignación).
CREATE OR REPLACE PROCEDURE presus.sp_validar_conflicto_jurado(
    IN p_solicitud_id BIGINT,
    IN p_docente_id BIGINT,
    IN p_fecha_inicio TIMESTAMP,
    IN p_duracion_min INTEGER,
    INOUT p_disponible BOOLEAN DEFAULT NULL
)
AS $$
DECLARE
    v_conflictos INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_conflictos
    FROM presus.miembros_tribunal mt
    INNER JOIN presus.cronograma c ON c.solicitud_id = mt.solicitud_id
    WHERE mt.docente_id = p_docente_id
      AND mt.solicitud_id <> p_solicitud_id
      AND c.fecha_inicio < (p_fecha_inicio + (p_duracion_min || ' minutes')::INTERVAL)
      AND (c.fecha_inicio + (c.duracion_min || ' minutes')::INTERVAL) > p_fecha_inicio;

    p_disponible := (v_conflictos = 0); -- TRUE = sin conflicto, el docente está disponible
END;
$$ LANGUAGE plpgsql;

-- 6. Procedimiento de GENERACIÓN DE CÓDIGO SECUENCIAL: código de expediente de un
-- estudiante (formato EXP-<año>-NNNNN), usando nextval() sobre una secuencia dedicada --
-- atómico a nivel de motor de base de datos, así que dos transacciones concurrentes creando
-- un estudiante al mismo tiempo nunca reciben el mismo número (evita la condición de carrera
-- que tendría, por ejemplo, calcular MAX(id)+1 en la capa de aplicación).
CREATE SEQUENCE IF NOT EXISTS presus.expediente_codigo_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE PROCEDURE presus.sp_generar_codigo_expediente(
    IN p_anio INTEGER,
    INOUT p_codigo VARCHAR DEFAULT NULL
)
AS $$
DECLARE
    v_anio INTEGER;
    v_siguiente BIGINT;
BEGIN
    v_anio := COALESCE(p_anio, EXTRACT(YEAR FROM NOW())::INTEGER);
    v_siguiente := nextval('presus.expediente_codigo_seq');
    p_codigo := 'EXP-' || v_anio || '-' || LPAD(v_siguiente::TEXT, 5, '0');
END;
$$ LANGUAGE plpgsql;
