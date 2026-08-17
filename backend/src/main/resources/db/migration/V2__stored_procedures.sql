-- =============================================================================
-- V2__stored_procedures.sql
-- Procedimientos Almacenados y Funciones para Complejidad de Negocio (PFC-UTEQ)
-- =============================================================================
-- Nota (corrección aplicada): la versión original de este archivo referenciaba nombres
-- de tabla que nunca existieron en V1__schema_inicial.sql ("estudiantes", "solicitudes",
-- "cronogramas", "salas", "jurados" -- los nombres reales son "estudiante", "solicitud",
-- "cronograma", "sala", "miembros_tribunal"), y "s.estado" como si fuera una columna
-- directa cuando en realidad es una FK "estado_id" a "estados_solicitud". Como estos
-- procedimientos nunca fueron invocados desde código Java (Fase 1/3), el error nunca se
-- manifestó. Se corrige aquí junto con la conexión real desde Java. Todas las referencias
-- se califican con el esquema "presus" explícitamente para no depender del search_path de
-- la sesión que invoque la función (ver también hibernate.default_schema en
-- application.properties, para el mismo tipo de problema con secuencias).
--
-- Nota (PROCEDURE + REFCURSOR en vez de FUNCTION...RETURNS TABLE): probando la conexión
-- real desde JPA (Fase 3) se encontró que Hibernate invoca @NamedStoredProcedureQuery vía la
-- sintaxis JDBC "{call proc(?)}" (CALL literal), y Postgres solo resuelve CALL contra el
-- catálogo de PROCEDURE, nunca contra FUNCTION ("... does not exist" aunque la función sí
-- exista) -- mismo problema ya documentado para los procedimientos con retorno escalar
-- (ver V3__stored_procedures_validacion_y_codigos.sql). Para un conjunto de filas (no un
-- escalar) el patrón estándar de JPA+Postgres es un PROCEDURE con un parámetro INOUT de
-- tipo refcursor: el procedimiento abre el cursor con la consulta, y Hibernate lo consume
-- como ParameterMode.REF_CURSOR.

-- 1. Procedimiento para calcular la nota final ponderada de una pre-sustentación
CREATE OR REPLACE PROCEDURE presus.sp_calcular_promedio_evaluacion(
    IN p_solicitud_id BIGINT,
    INOUT p_resultado refcursor DEFAULT 'promedio_evaluacion_cursor'
)
AS $$
DECLARE
    v_nota_instructor DOUBLE PRECISION;
    v_nota_jurado DOUBLE PRECISION;
    v_nota_final DOUBLE PRECISION;
    v_estado VARCHAR(30);
BEGIN
    -- Obtener nota promedio de jurados
    SELECT COALESCE(AVG(nota_obtenida), 0.0)
    INTO v_nota_jurado
    FROM presus.evaluaciones_criterio
    WHERE solicitud_id = p_solicitud_id;

    -- Obtener nota de instructor / tutor
    SELECT COALESCE(e.nota_instructor, 7.0)
    INTO v_nota_instructor
    FROM presus.evaluaciones e
    WHERE e.solicitud_id = p_solicitud_id
    LIMIT 1;

    -- Ponderación: 60% instructor + 40% jurados
    v_nota_final := ROUND((v_nota_instructor * 0.60 + v_nota_jurado * 0.40)::numeric, 2);

    IF v_nota_final >= 7.0 THEN
        v_estado := 'APROBADO';
    ELSIF v_nota_final >= 5.0 THEN
        v_estado := 'CON_OBSERVACIONES';
    ELSE
        v_estado := 'REPROBADO';
    END IF;

    -- Actualizar tabla de evaluaciones (evaluaciones no tiene columna actualizado_en)
    UPDATE presus.evaluaciones
    SET nota_jurado = v_nota_jurado,
        nota_final = v_nota_final,
        resultado = v_estado
    WHERE solicitud_id = p_solicitud_id;

    OPEN p_resultado FOR
        SELECT p_solicitud_id AS solicitud_id, v_nota_final AS nota_final, v_estado AS estado_resultado;
END;
$$ LANGUAGE plpgsql;

-- 2. Procedimiento para generar reporte consolidado de defensas por carrera y estado
CREATE OR REPLACE PROCEDURE presus.sp_generar_reporte_defensas(
    IN p_carrera VARCHAR,
    INOUT p_resultado refcursor DEFAULT 'reporte_defensas_cursor'
)
AS $$
BEGIN
    OPEN p_resultado FOR
        SELECT
            s.id AS solicitud_id,
            (u.nombre || ' ' || u.apellido)::TEXT AS estudiante_nombre,
            e.expediente_codigo AS expediente,
            s.titulo_tema,
            est.codigo AS estado_solicitud,
            c.fecha_inicio AS fecha_defensa,
            sa.nombre AS sala_nombre,
            COALESCE(ev.nota_final, 0.0) AS nota_final
        FROM presus.solicitud s
        INNER JOIN presus.estudiante e ON s.estudiante_id = e.id
        INNER JOIN presus.usuarios u ON e.usuario_id = u.id
        INNER JOIN presus.estados_solicitud est ON est.id = s.estado_id
        LEFT JOIN presus.cronograma c ON c.solicitud_id = s.id
        LEFT JOIN presus.sala sa ON c.sala_id = sa.id
        LEFT JOIN presus.evaluaciones ev ON ev.solicitud_id = s.id
        WHERE e.carrera ILIKE '%' || p_carrera || '%'
        ORDER BY s.fecha_registro DESC;
END;
$$ LANGUAGE plpgsql;

-- 3. Procedimiento para asignación masiva segura de jurados
CREATE OR REPLACE PROCEDURE presus.sp_asignar_jurado_masivo(
    p_solicitud_ids BIGINT[],
    p_docente_ids BIGINT[],
    p_rol VARCHAR
)
AS $$
DECLARE
    v_solicitud_id BIGINT;
    v_docente_id BIGINT;
    v_rol_id SMALLINT;
    i INT;
BEGIN
    IF array_length(p_solicitud_ids, 1) != array_length(p_docente_ids, 1) THEN
        RAISE EXCEPTION 'Los arreglos de solicitudes y docentes deben tener la misma longitud';
    END IF;

    SELECT id INTO v_rol_id FROM presus.roles_jurado WHERE codigo = p_rol;
    IF v_rol_id IS NULL THEN
        RAISE EXCEPTION 'Rol de jurado no reconocido: %', p_rol;
    END IF;

    FOR i IN 1..array_length(p_solicitud_ids, 1) LOOP
        v_solicitud_id := p_solicitud_ids[i];
        v_docente_id := p_docente_ids[i];

        INSERT INTO presus.miembros_tribunal (docente_id, solicitud_id, rol_jurado_id, confirmado, asignado_en)
        VALUES (v_docente_id, v_solicitud_id, v_rol_id, true, NOW())
        ON CONFLICT (solicitud_id, docente_id)
        DO UPDATE SET rol_jurado_id = EXCLUDED.rol_jurado_id, asignado_en = NOW();
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 4. Procedimiento para firma digital multi-actor de actas
CREATE OR REPLACE PROCEDURE presus.sp_firmar_acta_digital(
    p_acta_id BIGINT,
    p_rol VARCHAR,
    p_observacion TEXT
)
AS $$
BEGIN
    IF p_rol = 'PRESIDENTE' THEN
        UPDATE presus.actas
        SET firmada_presidente = true,
            fecha_firma_presidente = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[PRESIDENTE]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'VOCAL_1' THEN
        UPDATE presus.actas
        SET firmada_vocal1 = true,
            fecha_firma_vocal1 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL_1]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'VOCAL_2' THEN
        UPDATE presus.actas
        SET firmada_vocal2 = true,
            fecha_firma_vocal2 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL_2]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'TUTOR' THEN
        UPDATE presus.actas
        SET firmada_tutor = true,
            fecha_firma_tutor = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[TUTOR]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSE
        RAISE EXCEPTION 'Rol invalido para firma de acta: %', p_rol;
    END IF;
END;
$$ LANGUAGE plpgsql;
