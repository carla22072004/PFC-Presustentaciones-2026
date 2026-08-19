-- =============================================================================
-- V2__stored_procedures.sql
-- Procedimientos Almacenados y Funciones para Complejidad de Negocio (PFC-UTEQ)
-- =============================================================================

-- 1. Función para calcular la nota final ponderada de una pre-sustentación
CREATE OR REPLACE FUNCTION sp_calcular_promedio_evaluacion(p_solicitud_id BIGINT)
RETURNS TABLE (
    solicitud_id BIGINT,
    nota_final DOUBLE PRECISION,
    estado_resultado VARCHAR
) AS $$
DECLARE
    v_nota_instructor DOUBLE PRECISION;
    v_nota_jurado DOUBLE PRECISION;
    v_nota_final DOUBLE PRECISION;
    v_estado VARCHAR(30);
BEGIN
    -- Obtener nota promedio de jurados
    SELECT COALESCE(AVG(nota_obtenida), 0.0)
    INTO v_nota_jurado
    FROM evaluaciones_criterio
    WHERE solicitud_id = p_solicitud_id;

    -- Obtener nota de instructor / tutor
    SELECT COALESCE(e.nota_instructor, 7.0)
    INTO v_nota_instructor
    FROM evaluaciones e
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

    -- Actualizar tabla de evaluaciones
    UPDATE evaluaciones
    SET nota_jurado = v_nota_jurado,
        nota_final = v_nota_final,
        estado = v_estado,
        actualizado_en = NOW()
    WHERE solicitud_id = p_solicitud_id;

    RETURN QUERY SELECT p_solicitud_id, v_nota_final, v_estado;
END;
$$ LANGUAGE plpgsql;

-- 2. Función para generar reporte consolidado de defensas por carrera y estado
CREATE OR REPLACE FUNCTION sp_generar_reporte_defensas(p_carrera VARCHAR)
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
        s.estado AS estado_solicitud,
        c.fecha_defensa,
        sa.nombre AS sala_nombre,
        COALESCE(ev.nota_final, 0.0) AS nota_final
    FROM solicitudes s
    INNER JOIN estudiantes e ON s.estudiante_id = e.id
    INNER JOIN usuarios u ON e.usuario_id = u.id
    LEFT JOIN cronogramas c ON c.solicitud_id = s.id
    LEFT JOIN salas sa ON c.sala_id = sa.id
    LEFT JOIN evaluaciones ev ON ev.solicitud_id = s.id
    WHERE e.carrera ILIKE '%' || p_carrera || '%'
    ORDER BY s.fecha_registro DESC;
END;
$$ LANGUAGE plpgsql;

-- 3. Procedimiento para asignación segura de un jurado (invocado una vez por par
--    solicitud/docente desde Java dentro de un límite @Transactional para lograr
--    semántica de "asignación masiva": todo el lote se confirma o se revierte junto).
--    NOTA DE CORRECCIÓN: la versión original apuntaba a una tabla "jurados" con
--    columna "rol" VARCHAR que no es la que usa la capa JPA en producción; la
--    entidad real (Jurado.java) mapea a "miembros_tribunal", con rol_jurado_id
--    como FK a roles_jurado. Se corrige para escribir en la tabla real.
CREATE OR REPLACE PROCEDURE sp_asignar_jurado_masivo(
    p_solicitud_id BIGINT,
    p_docente_id BIGINT,
    p_rol_codigo VARCHAR
)
AS $$
DECLARE
    v_rol_jurado_id SMALLINT;
BEGIN
    SELECT id INTO v_rol_jurado_id FROM roles_jurado WHERE codigo = p_rol_codigo;
    IF v_rol_jurado_id IS NULL THEN
        RAISE EXCEPTION 'Rol de jurado invalido: %', p_rol_codigo;
    END IF;

    INSERT INTO miembros_tribunal (solicitud_id, docente_id, rol_jurado_id, confirmado, asignado_en)
    VALUES (p_solicitud_id, p_docente_id, v_rol_jurado_id, true, NOW())
    ON CONFLICT (solicitud_id, docente_id)
    DO UPDATE SET rol_jurado_id = EXCLUDED.rol_jurado_id, asignado_en = NOW();
END;
$$ LANGUAGE plpgsql;

-- 4. Procedimiento para firma digital multi-actor de actas
CREATE OR REPLACE PROCEDURE sp_firmar_acta_digital(
    p_acta_id BIGINT,
    p_rol VARCHAR,
    p_observacion TEXT
)
AS $$
BEGIN
    IF p_rol = 'PRESIDENTE' THEN
        UPDATE actas
        SET firmada_presidente = true,
            fecha_firma_presidente = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[PRESIDENTE]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'VOCAL1' THEN
        UPDATE actas
        SET firmada_vocal1 = true,
            fecha_firma_vocal1 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL1]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'VOCAL2' THEN
        UPDATE actas
        SET firmada_vocal2 = true,
            fecha_firma_vocal2 = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[VOCAL2]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSIF p_rol = 'TUTOR' THEN
        UPDATE actas
        SET firmada_tutor = true,
            fecha_firma_tutor = NOW(),
            observaciones_acta = COALESCE(observaciones_acta, '') || E'\n[TUTOR]: ' || COALESCE(p_observacion, 'Firma registrada')
        WHERE id = p_acta_id;
    ELSE
        RAISE EXCEPTION 'Rol invalido para firma de acta: %', p_rol;
    END IF;
END;
$$ LANGUAGE plpgsql;
