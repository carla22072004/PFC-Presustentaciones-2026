-- =============================================================================
-- V3__actualizar_procedimientos_fase3.sql
-- Corrección y Ampliación de Procedimientos Almacenados (PFC-UTEQ)
-- =============================================================================

-- 1. Función para calcular la nota final ponderada de una pre-sustentación (evaluaciones_finales)
CREATE OR REPLACE FUNCTION presus.sp_calcular_promedio_evaluacion(p_solicitud_id BIGINT)
RETURNS TABLE (
    solicitud_id BIGINT,
    nota_final DOUBLE PRECISION,
    estado_resultado VARCHAR
) AS $$
DECLARE
    v_nota_instructor DOUBLE PRECISION;
    v_nota_jurado DOUBLE PRECISION;
    v_nota_final DOUBLE PRECISION;
    v_peso_instructor DOUBLE PRECISION;
    v_peso_jurado DOUBLE PRECISION;
    v_estado_id SMALLINT;
    v_estado_codigo VARCHAR(30);
BEGIN
    -- Obtener nota promedio de jurados
    SELECT COALESCE(AVG(nota_obtenida), 0.0)
    INTO v_nota_jurado
    FROM presus.evaluaciones_criterio
    WHERE solicitud_id = p_solicitud_id;

    -- Obtener nota de instructor y pesos
    SELECT COALESCE(e.nota_instructor, 7.0), COALESCE(e.peso_instructor, 0.6), COALESCE(e.peso_jurado, 0.4)
    INTO v_nota_instructor, v_peso_instructor, v_peso_jurado
    FROM presus.evaluaciones_finales e
    WHERE e.solicitud_id = p_solicitud_id
    LIMIT 1;

    -- Si no existe la evaluación final, usar pesos por defecto (60% instructor, 40% jurado)
    IF v_peso_instructor IS NULL THEN
        v_peso_instructor := 0.6;
        v_peso_jurado := 0.4;
    END IF;

    -- Ponderación de nota
    v_nota_final := ROUND((v_nota_instructor * v_peso_instructor + v_nota_jurado * v_peso_jurado)::numeric, 2);

    IF v_nota_final >= 7.0 THEN
        v_estado_codigo := 'APROBADO';
    ELSE
        v_estado_codigo := 'REPROBADO';
    END IF;

    -- Obtener o crear el resultado en el catálogo
    SELECT id INTO v_estado_id
    FROM presus.resultados_evaluacion
    WHERE codigo = v_estado_codigo;

    IF v_estado_id IS NULL THEN
        INSERT INTO presus.resultados_evaluacion (id, codigo, nombre)
        VALUES (CAST(COALESCE((SELECT MAX(id) FROM presus.resultados_evaluacion), 0) + 1 AS SMALLINT), v_estado_codigo, INITCAP(v_estado_codigo))
        RETURNING id INTO v_estado_id;
    END IF;

    -- Actualizar o registrar la evaluación final consolidada
    UPDATE presus.evaluaciones_finales
    SET nota_jurado_promedio = v_nota_jurado,
        nota_final = v_nota_final,
        resultado_id = v_estado_id,
        fecha_calculo = NOW()
    WHERE solicitud_id = p_solicitud_id;

    IF NOT FOUND THEN
        INSERT INTO presus.evaluaciones_finales (solicitud_id, nota_instructor, nota_jurado_promedio, nota_final, peso_instructor, peso_jurado, resultado_id, fecha_calculo)
        VALUES (p_solicitud_id, v_nota_instructor, v_nota_jurado, v_nota_final, v_peso_instructor, v_peso_jurado, v_estado_id, NOW());
    END IF;

    RETURN QUERY SELECT p_solicitud_id, v_nota_final, CAST(v_estado_codigo AS VARCHAR);
END;
$$ LANGUAGE plpgsql;

-- 2. Función para generar reporte consolidado de defensas por carrera y estado (tablas físicas del esquema)
CREATE OR REPLACE FUNCTION presus.sp_generar_reporte_defensas(p_carrera VARCHAR)
RETURNS TABLE (
    solicitud_id BIGINT,
    estudiante_nombre TEXT,
    expediente VARCHAR,
    titulo_tema VARCHAR,
    estado_solicitud VARCHAR,
    fecha_defensa TIMESTAMP,
    sala_nombre VARCHAR,
    nota_final DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        s.id AS solicitud_id,
        (u.nombre || ' ' || u.apellido)::TEXT AS estudiante_nombre,
        e.expediente_codigo AS expediente,
        s.titulo_tema,
        CAST(es.nombre AS VARCHAR) AS estado_solicitud,
        c.fecha_inicio AS fecha_defensa,
        sa.nombre AS sala_nombre,
        COALESCE(ev.nota_final, 0.0) AS nota_final
    FROM presus.solicitud s
    INNER JOIN presus.estudiante e ON s.estudiante_id = e.id
    INNER JOIN presus.usuarios u ON e.usuario_id = u.id
    INNER JOIN presus.estados_solicitud es ON s.estado_id = es.id
    LEFT JOIN presus.cronograma c ON c.solicitud_id = s.id
    LEFT JOIN presus.sala sa ON c.sala_id = sa.id
    LEFT JOIN presus.evaluaciones_finales ev ON ev.solicitud_id = s.id
    WHERE e.carrera ILIKE '%' || p_carrera || '%'
    ORDER BY s.fecha_registro DESC;
END;
$$ LANGUAGE plpgsql;

-- 3. Procedimiento para asignación masiva de jurados (miembros_tribunal)
CREATE OR REPLACE PROCEDURE presus.sp_asignar_jurado_masivo(
    p_solicitud_ids BIGINT[],
    p_docente_ids BIGINT[],
    p_rol VARCHAR
) AS $$
DECLARE
    v_solicitud_id BIGINT;
    v_docente_id BIGINT;
    v_rol_id SMALLINT;
    i INT;
BEGIN
    IF array_length(p_solicitud_ids, 1) IS NULL OR array_length(p_docente_ids, 1) IS NULL THEN
        RAISE EXCEPTION 'Los arreglos de solicitudes y docentes no pueden ser nulos o vacíos';
    END IF;

    IF array_length(p_solicitud_ids, 1) != array_length(p_docente_ids, 1) THEN
        RAISE EXCEPTION 'Los arreglos de solicitudes y docentes deben tener la misma longitud';
    END IF;

    -- Obtener id del rol del jurado
    SELECT id INTO v_rol_id
    FROM presus.roles_jurado
    WHERE codigo = UPPER(p_rol);

    IF v_rol_id IS NULL THEN
        INSERT INTO presus.roles_jurado (id, codigo, nombre)
        VALUES (CAST(COALESCE((SELECT MAX(id) FROM presus.roles_jurado), 0) + 1 AS SMALLINT), UPPER(p_rol), INITCAP(p_rol))
        RETURNING id INTO v_rol_id;
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

-- 4. Procedimiento para firma digital de actas (consolidación del estado firmada)
CREATE OR REPLACE PROCEDURE presus.sp_firmar_acta_digital(
    p_acta_id BIGINT,
    p_rol VARCHAR,
    p_observacion TEXT
) AS $$
BEGIN
    IF UPPER(p_rol) = 'PRESIDENTE' THEN
        UPDATE presus.actas
        SET firmada_presidente = true,
            fecha_firma_presidente = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[PRESIDENTE]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF UPPER(p_rol) = 'VOCAL_1' OR UPPER(p_rol) = 'VOCAL1' THEN
        UPDATE presus.actas
        SET firmada_vocal1 = true,
            fecha_firma_vocal1 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL_1]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF UPPER(p_rol) = 'VOCAL_2' OR UPPER(p_rol) = 'VOCAL2' THEN
        UPDATE presus.actas
        SET firmada_vocal2 = true,
            fecha_firma_vocal2 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL_2]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF UPPER(p_rol) = 'TUTOR' THEN
        UPDATE presus.actas
        SET firmada_tutor = true,
            fecha_firma_tutor = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[TUTOR]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSE
        RAISE EXCEPTION 'Rol inválido para firma de acta: %', p_rol;
    END IF;

    -- Consolidar el estado general de firmas
    UPDATE presus.actas
    SET firmada = (firmada_presidente AND firmada_vocal1 AND firmada_vocal2 AND firmada_tutor)
    WHERE id = p_acta_id;
END;
$$ LANGUAGE plpgsql;

-- 5. Procedimiento Adicional 5 (Función): Obtener estadísticas consolidadas del desempeño de tutores
CREATE OR REPLACE FUNCTION presus.sp_obtener_estadisticas_tutores()
RETURNS TABLE (
    tutor_docente_id BIGINT,
    tutor_nombre TEXT,
    tutorias_activas BIGINT,
    tutorias_completadas BIGINT,
    total_fases_aprobadas BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        d.id AS tutor_docente_id,
        (u.nombre || ' ' || u.apellido)::TEXT AS tutor_nombre,
        COUNT(CASE WHEN t.estado = 'ACTIVO' THEN 1 END) AS tutorias_activas,
        COUNT(CASE WHEN t.estado = 'COMPLETADA' THEN 1 END) AS tutorias_completadas,
        COUNT(tf.id) AS total_fases_aprobadas
    FROM presus.docente d
    INNER JOIN presus.usuarios u ON d.usuario_id = u.id
    LEFT JOIN presus.tutores t ON t.docente_id = d.id
    LEFT JOIN presus.tutoria_fases tf ON tf.tutor_id = t.id AND tf.estado = 'APROBADA'
    GROUP BY d.id, u.nombre, u.apellido
    ORDER BY tutorias_activas DESC, tutorias_completadas DESC;
END;
$$ LANGUAGE plpgsql;

-- 6. Procedimiento Adicional 6: Validar y registrar avance de fase de tutoría
CREATE OR REPLACE PROCEDURE presus.sp_registrar_tutoria_avance(
    p_tutor_id BIGINT,
    p_numero_fase INT,
    p_archivo_pdf VARCHAR,
    p_tamano_bytes BIGINT,
    p_sha256 VARCHAR
) AS $$
DECLARE
    v_fases_previas_aprobadas BIGINT;
    v_tutoria_estado VARCHAR(20);
    v_fase_id BIGINT;
BEGIN
    -- Verificar el estado de la tutoría
    SELECT estado INTO v_tutoria_estado
    FROM presus.tutores
    WHERE id = p_tutor_id;

    IF v_tutoria_estado = 'COMPLETADA' THEN
        RAISE EXCEPTION 'La tutoría ya está completada, no se pueden registrar más fases';
    END IF;

    -- Si es fase > 1, verificar que la fase inmediatamente anterior esté aprobada
    IF p_numero_fase > 1 THEN
        SELECT COUNT(*) INTO v_fases_previas_aprobadas
        FROM presus.tutoria_fases
        WHERE tutor_id = p_tutor_id 
          AND numero_fase = p_numero_fase - 1 
          AND estado = 'APROBADA';

        IF v_fases_previas_aprobadas = 0 THEN
            RAISE EXCEPTION 'No se puede registrar la fase %, la fase % debe estar APROBADA', p_numero_fase, p_numero_fase - 1;
        END IF;
    END IF;

    -- Registrar o actualizar la fase
    SELECT id INTO v_fase_id
    FROM presus.tutoria_fases
    WHERE tutor_id = p_tutor_id AND numero_fase = p_numero_fase;

    IF v_fase_id IS NOT NULL THEN
        UPDATE presus.tutoria_fases
        SET archivo_pdf_estudiante = p_archivo_pdf,
            tamano_pdf_bytes = p_tamano_bytes,
            sha256_pdf = p_sha256,
            estado = 'INICIADA'
        WHERE id = v_fase_id;
    ELSE
        INSERT INTO presus.tutoria_fases (tutor_id, numero_fase, archivo_pdf_estudiante, tamano_pdf_bytes, sha256_pdf, estado, fecha_inicio)
        VALUES (p_tutor_id, p_numero_fase, p_archivo_pdf, p_tamano_bytes, p_sha256, 'INICIADA', NOW());
    END IF;
END;
$$ LANGUAGE plpgsql;
