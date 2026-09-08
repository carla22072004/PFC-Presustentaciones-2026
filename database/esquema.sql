-- =============================================================================
--  esquema.sql  —  Sistema de Gestión de Pre-Sustentaciones (UTEQ)
-- =============================================================================
--  Crea la base de datos completa: esquema `presus`, las 53 tablas con sus
--  claves primarias y foráneas (86 FK), secuencias, índices, funciones y los
--  disparadores de auditoría.
--
--  Es el estado consolidado del esquema tras aplicar las migraciones Flyway
--  V1..V30 (backend/src/main/resources/db/migration). Generado con:
--    pg_dump --schema-only --no-owner --no-privileges -n presus
--
--  Uso:
--    createdb BdPresustentaciones
--    psql -d BdPresustentaciones -f database/esquema.sql
--    psql -d BdPresustentaciones -f database/datos_masivos.sql   (opcional: ~1M filas)
--
--  PostgreSQL 15+
-- =============================================================================

--
-- PostgreSQL database dump
--



SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: presus; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA presus;


--
-- Name: fn_auditoria_generica(); Type: FUNCTION; Schema: presus; Owner: -
--

CREATE FUNCTION presus.fn_auditoria_generica() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_usuario_id     BIGINT;
    v_usuario_nombre VARCHAR(200);
    v_accion         VARCHAR(20);
    v_registro_id    BIGINT;
    v_nuevo          JSONB;
    v_anterior       JSONB;
BEGIN
    v_usuario_id := NULLIF(current_setting('presus.usuario_actual', true), '')::BIGINT;
    IF v_usuario_id IS NOT NULL THEN
        SELECT (nombre || ' ' || apellido) INTO v_usuario_nombre FROM presus.usuarios WHERE id = v_usuario_id;
    END IF;

    IF TG_OP = 'INSERT' THEN
        v_nuevo := to_jsonb(NEW);
        -- Nunca se guarda el hash de contrasena en la auditoria.
        IF TG_TABLE_NAME = 'usuarios' THEN v_nuevo := v_nuevo - 'password'; END IF;
        v_accion := 'CREAR';
        v_registro_id := (v_nuevo->>'id')::BIGINT;
        INSERT INTO presus.auditoria (tabla, registro_id, accion, usuario_id, usuario_nombre, datos_nuevos)
        VALUES (TG_TABLE_NAME, v_registro_id, v_accion, v_usuario_id, v_usuario_nombre, v_nuevo);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        v_anterior := to_jsonb(OLD);
        v_nuevo := to_jsonb(NEW);
        IF TG_TABLE_NAME = 'usuarios' THEN
            v_anterior := v_anterior - 'password';
            v_nuevo := v_nuevo - 'password';
        END IF;
        v_accion := 'MODIFICAR';
        v_registro_id := (v_nuevo->>'id')::BIGINT;
        INSERT INTO presus.auditoria (tabla, registro_id, accion, usuario_id, usuario_nombre, datos_anteriores, datos_nuevos)
        VALUES (TG_TABLE_NAME, v_registro_id, v_accion, v_usuario_id, v_usuario_nombre, v_anterior, v_nuevo);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        v_anterior := to_jsonb(OLD);
        IF TG_TABLE_NAME = 'usuarios' THEN v_anterior := v_anterior - 'password'; END IF;
        v_accion := 'ELIMINAR';
        v_registro_id := (v_anterior->>'id')::BIGINT;
        INSERT INTO presus.auditoria (tabla, registro_id, accion, usuario_id, usuario_nombre, datos_anteriores)
        VALUES (TG_TABLE_NAME, v_registro_id, v_accion, v_usuario_id, v_usuario_nombre, v_anterior);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;


--
-- Name: fn_auditoria_rol_permisos(); Type: FUNCTION; Schema: presus; Owner: -
--

CREATE FUNCTION presus.fn_auditoria_rol_permisos() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_usuario_id     BIGINT;
    v_usuario_nombre VARCHAR(200);
BEGIN
    v_usuario_id := NULLIF(current_setting('presus.usuario_actual', true), '')::BIGINT;
    IF v_usuario_id IS NOT NULL THEN
        SELECT (nombre || ' ' || apellido) INTO v_usuario_nombre FROM presus.usuarios WHERE id = v_usuario_id;
    END IF;

    IF TG_OP = 'INSERT' THEN
        INSERT INTO presus.auditoria (tabla, registro_id, accion, usuario_id, usuario_nombre, datos_nuevos)
        VALUES ('rol_permisos', NEW.rol_id, 'ASIGNAR_PERMISO', v_usuario_id, v_usuario_nombre, to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO presus.auditoria (tabla, registro_id, accion, usuario_id, usuario_nombre, datos_anteriores)
        VALUES ('rol_permisos', OLD.rol_id, 'QUITAR_PERMISO', v_usuario_id, v_usuario_nombre, to_jsonb(OLD));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;


--
-- Name: sp_asignar_jurado_masivo(bigint[], bigint[], character varying); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_asignar_jurado_masivo(IN p_solicitud_ids bigint[], IN p_docente_ids bigint[], IN p_rol character varying)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_asignar_jurado_masivo(bigint, bigint, character varying); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_asignar_jurado_masivo(IN p_solicitud_id bigint, IN p_docente_id bigint, IN p_rol_codigo character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_rol_jurado_id SMALLINT;
BEGIN
    SELECT id INTO v_rol_jurado_id FROM presus.roles_jurado WHERE codigo = p_rol_codigo;
    IF v_rol_jurado_id IS NULL THEN
        RAISE EXCEPTION 'Rol de jurado invalido: %', p_rol_codigo;
    END IF;

    INSERT INTO presus.miembros_tribunal (solicitud_id, docente_id, rol_jurado_id, confirmado, asignado_en)
    VALUES (p_solicitud_id, p_docente_id, v_rol_jurado_id, true, NOW())
    ON CONFLICT (solicitud_id, docente_id)
    DO UPDATE SET rol_jurado_id = EXCLUDED.rol_jurado_id, asignado_en = NOW();
END;
$$;


--
-- Name: sp_calcular_promedio_evaluacion(bigint); Type: FUNCTION; Schema: presus; Owner: -
--

CREATE FUNCTION presus.sp_calcular_promedio_evaluacion(p_solicitud_id bigint) RETURNS TABLE(solicitud_id bigint, nota_final double precision, estado_resultado character varying)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_calcular_promedio_evaluacion(bigint, refcursor); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_calcular_promedio_evaluacion(IN p_solicitud_id bigint, INOUT p_resultado refcursor DEFAULT 'promedio_evaluacion_cursor'::refcursor)
    LANGUAGE plpgsql
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
$$;


--
-- Name: sp_firmar_acta_digital(bigint, character varying, text); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_firmar_acta_digital(IN p_acta_id bigint, IN p_rol character varying, IN p_observacion text)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_generar_codigo_expediente(integer, character varying); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_generar_codigo_expediente(IN p_anio integer, INOUT p_codigo character varying DEFAULT NULL::character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_anio INTEGER;
    v_siguiente BIGINT;
BEGIN
    v_anio := COALESCE(p_anio, EXTRACT(YEAR FROM NOW())::INTEGER);
    v_siguiente := nextval('presus.expediente_codigo_seq');
    p_codigo := 'EXP-' || v_anio || '-' || LPAD(v_siguiente::TEXT, 5, '0');
END;
$$;


--
-- Name: sp_generar_reporte_defensas(character varying); Type: FUNCTION; Schema: presus; Owner: -
--

CREATE FUNCTION presus.sp_generar_reporte_defensas(p_carrera character varying) RETURNS TABLE(solicitud_id bigint, estudiante_nombre text, expediente character varying, titulo_tema character varying, estado_solicitud character varying, fecha_defensa timestamp without time zone, sala_nombre character varying, nota_final double precision)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_generar_reporte_defensas(character varying, refcursor); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_generar_reporte_defensas(IN p_carrera character varying, INOUT p_resultado refcursor DEFAULT 'reporte_defensas_cursor'::refcursor)
    LANGUAGE plpgsql
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
$$;


--
-- Name: sp_obtener_estadisticas_tutores(); Type: FUNCTION; Schema: presus; Owner: -
--

CREATE FUNCTION presus.sp_obtener_estadisticas_tutores() RETURNS TABLE(tutor_docente_id bigint, tutor_nombre text, tutorias_activas bigint, tutorias_completadas bigint, total_fases_aprobadas bigint)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_registrar_tutoria_avance(bigint, integer, character varying, bigint, character varying); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_registrar_tutoria_avance(IN p_tutor_id bigint, IN p_numero_fase integer, IN p_archivo_pdf character varying, IN p_tamano_bytes bigint, IN p_sha256 character varying)
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: sp_validar_conflicto_jurado(bigint, bigint, timestamp without time zone, integer, boolean); Type: PROCEDURE; Schema: presus; Owner: -
--

CREATE PROCEDURE presus.sp_validar_conflicto_jurado(IN p_solicitud_id bigint, IN p_docente_id bigint, IN p_fecha_inicio timestamp without time zone, IN p_duracion_min integer, INOUT p_disponible boolean DEFAULT NULL::boolean)
    LANGUAGE plpgsql
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
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: actas; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.actas (
    fecha_generacion date NOT NULL,
    firmada boolean NOT NULL,
    firmada_presidente boolean NOT NULL,
    firmada_tutor boolean NOT NULL,
    firmada_vocal1 boolean NOT NULL,
    firmada_vocal2 boolean NOT NULL,
    fecha_firma_presidente timestamp(6) without time zone,
    fecha_firma_tutor timestamp(6) without time zone,
    fecha_firma_vocal1 timestamp(6) without time zone,
    fecha_firma_vocal2 timestamp(6) without time zone,
    id bigint NOT NULL,
    solicitud_id bigint NOT NULL,
    archivo_pdf character varying(255),
    observaciones_acta text,
    estado_id smallint DEFAULT 1 NOT NULL
);


--
-- Name: actas_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.actas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.actas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: anteproyectos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.anteproyectos (
    fecha_envio date,
    id bigint NOT NULL,
    solicitud_id bigint,
    tamano_bytes bigint,
    estado character varying(30),
    sha256_hash character varying(64),
    archivo_pdf character varying(255),
    observaciones text,
    estado_id smallint DEFAULT 1 NOT NULL
);


--
-- Name: anteproyectos_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.anteproyectos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.anteproyectos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: areas_tematicas; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.areas_tematicas (
    id integer NOT NULL,
    linea_investigacion_id integer NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text
);


--
-- Name: areas_tematicas_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.areas_tematicas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.areas_tematicas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.auditoria (
    id bigint NOT NULL,
    tabla character varying(60) NOT NULL,
    registro_id bigint,
    accion character varying(20) NOT NULL,
    usuario_id bigint,
    usuario_nombre character varying(200),
    fecha timestamp(6) without time zone DEFAULT now() NOT NULL,
    datos_anteriores jsonb,
    datos_nuevos jsonb
);


--
-- Name: auditoria_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.auditoria ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.auditoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: bloques_horarios; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.bloques_horarios (
    hora_fin time(6) without time zone NOT NULL,
    hora_inicio time(6) without time zone NOT NULL,
    id integer NOT NULL,
    jornada_id smallint NOT NULL,
    nombre character varying(60) NOT NULL
);


--
-- Name: bloques_horarios_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.bloques_horarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.bloques_horarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: carrera_linea_investigacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.carrera_linea_investigacion (
    carrera_id integer NOT NULL,
    linea_investigacion_id integer NOT NULL
);


--
-- Name: carreras; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.carreras (
    facultad_id integer NOT NULL,
    id integer NOT NULL,
    codigo character varying(20) NOT NULL,
    modalidad_estudio character varying(40),
    nombre character varying(150) NOT NULL
);


--
-- Name: carreras_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.carreras ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.carreras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: convocatorias_titulacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.convocatorias_titulacion (
    activa boolean NOT NULL,
    fecha_fin date NOT NULL,
    fecha_inicio date NOT NULL,
    id integer NOT NULL,
    periodo_academico_id integer NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(150) NOT NULL
);


--
-- Name: convocatorias_titulacion_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.convocatorias_titulacion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.convocatorias_titulacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: criterios_rubrica; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.criterios_rubrica (
    orden integer NOT NULL,
    ponderacion double precision NOT NULL,
    id bigint NOT NULL,
    rubrica_id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion text
);


--
-- Name: criterios_rubrica_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.criterios_rubrica ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.criterios_rubrica_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: cronograma; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.cronograma (
    bloque_id integer,
    convocatoria_id integer NOT NULL,
    duracion_min integer NOT NULL,
    estado_id smallint NOT NULL,
    numero_intento smallint NOT NULL,
    creado_en timestamp(6) without time zone NOT NULL,
    fecha_inicio timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    sala_id bigint NOT NULL,
    solicitud_id bigint NOT NULL,
    estado character varying(30) NOT NULL
);


--
-- Name: cronograma_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.cronograma ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.cronograma_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: disponibilidad_sala; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.disponibilidad_sala (
    bloque_id integer NOT NULL,
    disponible boolean NOT NULL,
    fecha date NOT NULL,
    id bigint NOT NULL,
    sala_id bigint NOT NULL,
    motivo character varying(200)
);


--
-- Name: disponibilidad_sala_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.disponibilidad_sala ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.disponibilidad_sala_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: docente; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.docente (
    carga_horaria_semanal integer NOT NULL,
    disponible boolean NOT NULL,
    facultad_id integer,
    creado_en timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    area_especialidad character varying(180)
);


--
-- Name: docente_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.docente ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.docente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: estados_academicos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estados_academicos (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: estados_acta; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estados_acta (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL,
    orden smallint NOT NULL
);


--
-- Name: estados_cronograma; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estados_cronograma (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: estados_cronograma_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.estados_cronograma_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estados_cronograma_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.estados_cronograma_seq OWNED BY presus.estados_cronograma.id;


--
-- Name: estados_proceso; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estados_proceso (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: estados_proceso_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.estados_proceso_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estados_proceso_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.estados_proceso_seq OWNED BY presus.estados_proceso.id;


--
-- Name: estados_solicitud; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estados_solicitud (
    id smallint NOT NULL,
    orden smallint DEFAULT 0,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: estados_solicitud_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.estados_solicitud_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: estados_solicitud_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.estados_solicitud_seq OWNED BY presus.estados_solicitud.id;


--
-- Name: estudiante; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.estudiante (
    carrera_id integer NOT NULL,
    periodo_ingreso_id integer,
    semestre_actual smallint NOT NULL,
    creado_en timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    semestre character varying(30),
    telefono character varying(30),
    expediente_codigo character varying(60),
    carrera character varying(180) NOT NULL,
    estado_academico_id smallint DEFAULT 1 NOT NULL
);


--
-- Name: estudiante_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.estudiante ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.estudiante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluaciones; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.evaluaciones (
    nota_final double precision,
    nota_instructor double precision,
    nota_jurado double precision,
    peso_instructor double precision NOT NULL,
    peso_jurado double precision NOT NULL,
    id bigint NOT NULL,
    rubrica_id bigint,
    solicitud_id bigint,
    resultado character varying(20),
    comentario_preestablecido text,
    observaciones text
);


--
-- Name: evaluaciones_criterio; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.evaluaciones_criterio (
    escala integer NOT NULL,
    nota_obtenida double precision NOT NULL,
    criterio_id bigint NOT NULL,
    evaluador_id bigint NOT NULL,
    id bigint NOT NULL,
    registrado_en timestamp(6) without time zone NOT NULL,
    solicitud_id bigint NOT NULL,
    observacion_auto text,
    observacion_manual text,
    observaciones text,
    jurado_id bigint NOT NULL
);


--
-- Name: evaluaciones_criterio_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.evaluaciones_criterio ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.evaluaciones_criterio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluaciones_finales; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.evaluaciones_finales (
    nota_final double precision,
    nota_instructor double precision,
    nota_jurado_promedio double precision,
    peso_instructor double precision NOT NULL,
    peso_jurado double precision NOT NULL,
    resultado_id smallint,
    fecha_calculo timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    rubrica_id bigint,
    solicitud_id bigint NOT NULL,
    comentario_preestablecido text,
    observaciones text
);


--
-- Name: evaluaciones_finales_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.evaluaciones_finales ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.evaluaciones_finales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluaciones_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.evaluaciones ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.evaluaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluaciones_jurado; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.evaluaciones_jurado (
    nota_jurado double precision NOT NULL,
    fecha_registro timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    jurado_id bigint NOT NULL,
    solicitud_id bigint NOT NULL,
    resultado character varying(20),
    comentario_preestablecido text,
    observaciones text
);


--
-- Name: evaluaciones_jurado_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.evaluaciones_jurado ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.evaluaciones_jurado_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: evaluadores; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.evaluadores (
    peso double precision NOT NULL,
    tipo_evaluador_id smallint NOT NULL,
    docente_id bigint NOT NULL,
    fecha_asignacion timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    miembro_tribunal_id bigint,
    solicitud_id bigint NOT NULL
);


--
-- Name: evaluadores_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.evaluadores ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.evaluadores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: expediente_codigo_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.expediente_codigo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: facultades; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.facultades (
    id integer NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(150) NOT NULL
);


--
-- Name: facultades_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.facultades ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.facultades_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: historial_cronograma; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.historial_cronograma (
    cronograma_id bigint NOT NULL,
    fecha_anterior timestamp(6) without time zone NOT NULL,
    fecha_cambio timestamp(6) without time zone NOT NULL,
    fecha_nueva timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    sala_anterior_id bigint,
    sala_nueva_id bigint,
    usuario_id bigint NOT NULL,
    motivo text
);


--
-- Name: historial_cronograma_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.historial_cronograma ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.historial_cronograma_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: historial_estados_acta; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.historial_estados_acta (
    id bigint NOT NULL,
    acta_id bigint NOT NULL,
    estado_anterior_id smallint,
    estado_nuevo_id smallint NOT NULL,
    usuario_id bigint,
    rol_usuario character varying(30),
    accion character varying(30) NOT NULL,
    comentario text,
    fecha_cambio timestamp(6) without time zone DEFAULT now() NOT NULL
);


--
-- Name: historial_estados_acta_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.historial_estados_acta ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.historial_estados_acta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: historial_estados_solicitud; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.historial_estados_solicitud (
    estado_anterior_id smallint,
    estado_nuevo_id smallint NOT NULL,
    fecha_cambio timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    solicitud_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    comentario text
);


--
-- Name: historial_estados_solicitud_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.historial_estados_solicitud ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.historial_estados_solicitud_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: jornadas; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.jornadas (
    id smallint NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(60) NOT NULL
);


--
-- Name: jornadas_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.jornadas_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jornadas_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.jornadas_seq OWNED BY presus.jornadas.id;


--
-- Name: lineas_investigacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.lineas_investigacion (
    facultad_id integer,
    id integer NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text
);


--
-- Name: lineas_investigacion_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.lineas_investigacion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.lineas_investigacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: miembros_tribunal; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.miembros_tribunal (
    confirmado boolean NOT NULL,
    rol_jurado_id smallint NOT NULL,
    asignado_en timestamp(6) without time zone NOT NULL,
    docente_id bigint NOT NULL,
    id bigint NOT NULL,
    solicitud_id bigint NOT NULL
);


--
-- Name: miembros_tribunal_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.miembros_tribunal ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.miembros_tribunal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: modalidades_titulacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.modalidades_titulacion (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(120) NOT NULL
);


--
-- Name: modalidades_titulacion_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.modalidades_titulacion_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notificaciones; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.notificaciones (
    leida boolean NOT NULL,
    fecha timestamp(6) without time zone,
    id bigint NOT NULL,
    usuario_id bigint,
    mensaje text
);


--
-- Name: notificaciones_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.notificaciones ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.notificaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: periodos_academicos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.periodos_academicos (
    activo boolean NOT NULL,
    fecha_fin date NOT NULL,
    fecha_inicio date NOT NULL,
    id integer NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(100) NOT NULL
);


--
-- Name: periodos_academicos_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.periodos_academicos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.periodos_academicos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: permisos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.permisos (
    id smallint NOT NULL,
    codigo character varying(60) NOT NULL,
    nombre character varying(150) NOT NULL,
    categoria character varying(60) NOT NULL,
    descripcion text
);


--
-- Name: progreso_estudiante; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.progreso_estudiante (
    id integer NOT NULL,
    estudiante_id bigint NOT NULL,
    pasos_json jsonb DEFAULT '{}'::jsonb NOT NULL
);


--
-- Name: progreso_estudiante_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.progreso_estudiante ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.progreso_estudiante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: recursos_titulacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.recursos_titulacion (
    id integer NOT NULL,
    titulo character varying(255) NOT NULL,
    categoria character varying(100) NOT NULL,
    url_archivo character varying(500) NOT NULL,
    carrera_id integer
);


--
-- Name: recursos_titulacion_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.recursos_titulacion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.recursos_titulacion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: respaldo_config; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.respaldo_config (
    id smallint DEFAULT 1 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    cron character varying(120) DEFAULT '0 0 23 * * SUN'::character varying NOT NULL,
    retener_diarios smallint DEFAULT 7 NOT NULL,
    retener_semanales smallint DEFAULT 5 NOT NULL,
    retener_mensuales smallint DEFAULT 12 NOT NULL,
    actualizado_en timestamp(6) without time zone DEFAULT now() NOT NULL,
    actualizado_por character varying(200),
    retener_dias_wal smallint DEFAULT 14 NOT NULL,
    diferencial_activo boolean DEFAULT false NOT NULL,
    cron_diferencial character varying(120) DEFAULT '0 30 2 * * WED,FRI'::character varying NOT NULL,
    CONSTRAINT respaldo_config_retener_pos CHECK (((retener_diarios >= 0) AND (retener_semanales >= 0) AND (retener_mensuales >= 0))),
    CONSTRAINT respaldo_config_singleton CHECK ((id = 1)),
    CONSTRAINT respaldo_config_wal_pos CHECK ((retener_dias_wal >= 0))
);


--
-- Name: respaldo_prueba_restauracion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.respaldo_prueba_restauracion (
    id bigint NOT NULL,
    respaldo_nombre character varying(255) NOT NULL,
    fecha timestamp(6) without time zone DEFAULT now() NOT NULL,
    resultado character varying(20) NOT NULL,
    responsable character varying(200),
    notas text,
    CONSTRAINT respaldo_prueba_resultado CHECK (((resultado)::text = ANY (ARRAY[('EXITOSA'::character varying)::text, ('FALLIDA'::character varying)::text])))
);


--
-- Name: respaldo_prueba_restauracion_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.respaldo_prueba_restauracion ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.respaldo_prueba_restauracion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: resultados_evaluacion; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.resultados_evaluacion (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: resultados_evaluacion_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.resultados_evaluacion_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: resultados_evaluacion_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.resultados_evaluacion_seq OWNED BY presus.resultados_evaluacion.id;


--
-- Name: rol_permisos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.rol_permisos (
    rol_id smallint NOT NULL,
    permiso_id smallint NOT NULL
);


--
-- Name: roles_jurado; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.roles_jurado (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: roles_jurado_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.roles_jurado_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_jurado_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.roles_jurado_seq OWNED BY presus.roles_jurado.id;


--
-- Name: roles_usuario; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.roles_usuario (
    id smallint NOT NULL,
    codigo character varying(30) NOT NULL,
    nombre character varying(80) NOT NULL
);


--
-- Name: roles_usuario_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.roles_usuario_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rubricas; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.rubricas (
    puntaje_maximo double precision NOT NULL,
    id bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    descripcion text
);


--
-- Name: rubricas_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.rubricas ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.rubricas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sala; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.sala (
    capacidad integer NOT NULL,
    disponible boolean NOT NULL,
    id bigint NOT NULL,
    codigo character varying(40) NOT NULL,
    nombre character varying(120) NOT NULL
);


--
-- Name: sala_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.sala ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.sala_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: solicitud; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.solicitud (
    area_tematica_id integer,
    convocatoria_id integer NOT NULL,
    estado_id smallint NOT NULL,
    linea_investigacion_id integer,
    modalidad_titulacion_id smallint NOT NULL,
    actualizado_en timestamp(6) without time zone NOT NULL,
    actualizado_por bigint,
    creado_por bigint,
    estudiante_id bigint NOT NULL,
    fecha_registro timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    suspendido_en timestamp(6) without time zone,
    titulo_tema character varying(300) NOT NULL,
    motivo_suspension text,
    observaciones text,
    estado character varying(30) NOT NULL
);


--
-- Name: solicitud_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.solicitud ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.solicitud_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: temas_guardados; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.temas_guardados (
    id integer NOT NULL,
    estudiante_id bigint NOT NULL,
    tema_propuesto_id integer NOT NULL,
    fecha_guardado timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: temas_guardados_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.temas_guardados ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.temas_guardados_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: temas_propuestos; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.temas_propuestos (
    id integer NOT NULL,
    titulo character varying(500) NOT NULL,
    problema text,
    objetivo_general text,
    objetivos_especificos text,
    justificacion text,
    beneficiarios text,
    nivel_dificultad character varying(50),
    carrera_id integer,
    linea_investigacion_id integer,
    area_id integer
);


--
-- Name: temas_propuestos_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.temas_propuestos ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.temas_propuestos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tipos_evaluador; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.tipos_evaluador (
    id smallint NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(60) NOT NULL
);


--
-- Name: tipos_evaluador_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.tipos_evaluador_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tipos_evaluador_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.tipos_evaluador_seq OWNED BY presus.tipos_evaluador.id;


--
-- Name: tipos_mensaje; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.tipos_mensaje (
    id smallint NOT NULL,
    codigo character varying(20) NOT NULL,
    nombre character varying(60) NOT NULL
);


--
-- Name: tipos_mensaje_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

CREATE SEQUENCE presus.tipos_mensaje_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tipos_mensaje_seq; Type: SEQUENCE OWNED BY; Schema: presus; Owner: -
--

ALTER SEQUENCE presus.tipos_mensaje_seq OWNED BY presus.tipos_mensaje.id;


--
-- Name: tutores; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.tutores (
    docente_id bigint NOT NULL,
    fecha_asignacion timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    solicitud_id bigint NOT NULL,
    estado character varying(20) NOT NULL,
    observaciones text,
    estado_id smallint DEFAULT 1 NOT NULL
);


--
-- Name: tutores_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.tutores ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.tutores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tutoria_fases; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.tutoria_fases (
    numero_fase integer NOT NULL,
    fecha_aprobacion timestamp(6) without time zone,
    fecha_inicio timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    tamano_pdf_bytes bigint,
    tutor_id bigint NOT NULL,
    estado character varying(30) NOT NULL,
    sha256_pdf character varying(64),
    archivo_pdf_estudiante character varying(255),
    estado_id smallint DEFAULT 1 NOT NULL
);


--
-- Name: tutoria_fases_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.tutoria_fases ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.tutoria_fases_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tutoria_mensajes; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.tutoria_mensajes (
    leido boolean NOT NULL,
    fase_id bigint NOT NULL,
    fecha_envio timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    remitente_id bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    contenido text NOT NULL,
    tipo_mensaje_id smallint NOT NULL
);


--
-- Name: tutoria_mensajes_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.tutoria_mensajes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.tutoria_mensajes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: presus; Owner: -
--

CREATE TABLE presus.usuarios (
    activo boolean NOT NULL,
    rol_id smallint NOT NULL,
    creado_en timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    apellido character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    email_notificaciones character varying(255),
    nombre character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    rol character varying(255) NOT NULL,
    telefono character varying(255)
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: presus; Owner: -
--

ALTER TABLE presus.usuarios ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME presus.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: actas actas_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.actas
    ADD CONSTRAINT actas_pkey PRIMARY KEY (id);


--
-- Name: actas actas_solicitud_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.actas
    ADD CONSTRAINT actas_solicitud_id_key UNIQUE (solicitud_id);


--
-- Name: anteproyectos anteproyectos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.anteproyectos
    ADD CONSTRAINT anteproyectos_pkey PRIMARY KEY (id);


--
-- Name: anteproyectos anteproyectos_solicitud_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.anteproyectos
    ADD CONSTRAINT anteproyectos_solicitud_id_key UNIQUE (solicitud_id);


--
-- Name: areas_tematicas areas_tematicas_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.areas_tematicas
    ADD CONSTRAINT areas_tematicas_pkey PRIMARY KEY (id);


--
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (id);


--
-- Name: bloques_horarios bloques_horarios_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.bloques_horarios
    ADD CONSTRAINT bloques_horarios_pkey PRIMARY KEY (id);


--
-- Name: carrera_linea_investigacion carrera_linea_investigacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carrera_linea_investigacion
    ADD CONSTRAINT carrera_linea_investigacion_pkey PRIMARY KEY (carrera_id, linea_investigacion_id);


--
-- Name: carreras carreras_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carreras
    ADD CONSTRAINT carreras_codigo_key UNIQUE (codigo);


--
-- Name: carreras carreras_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carreras
    ADD CONSTRAINT carreras_pkey PRIMARY KEY (id);


--
-- Name: convocatorias_titulacion convocatorias_titulacion_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.convocatorias_titulacion
    ADD CONSTRAINT convocatorias_titulacion_codigo_key UNIQUE (codigo);


--
-- Name: convocatorias_titulacion convocatorias_titulacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.convocatorias_titulacion
    ADD CONSTRAINT convocatorias_titulacion_pkey PRIMARY KEY (id);


--
-- Name: criterios_rubrica criterios_rubrica_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.criterios_rubrica
    ADD CONSTRAINT criterios_rubrica_pkey PRIMARY KEY (id);


--
-- Name: cronograma cronograma_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT cronograma_pkey PRIMARY KEY (id);


--
-- Name: disponibilidad_sala disponibilidad_sala_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.disponibilidad_sala
    ADD CONSTRAINT disponibilidad_sala_pkey PRIMARY KEY (id);


--
-- Name: docente docente_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.docente
    ADD CONSTRAINT docente_pkey PRIMARY KEY (id);


--
-- Name: docente docente_usuario_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.docente
    ADD CONSTRAINT docente_usuario_id_key UNIQUE (usuario_id);


--
-- Name: estados_academicos estados_academicos_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_academicos
    ADD CONSTRAINT estados_academicos_codigo_key UNIQUE (codigo);


--
-- Name: estados_academicos estados_academicos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_academicos
    ADD CONSTRAINT estados_academicos_pkey PRIMARY KEY (id);


--
-- Name: estados_acta estados_acta_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_acta
    ADD CONSTRAINT estados_acta_codigo_key UNIQUE (codigo);


--
-- Name: estados_acta estados_acta_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_acta
    ADD CONSTRAINT estados_acta_pkey PRIMARY KEY (id);


--
-- Name: estados_cronograma estados_cronograma_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_cronograma
    ADD CONSTRAINT estados_cronograma_codigo_key UNIQUE (codigo);


--
-- Name: estados_cronograma estados_cronograma_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_cronograma
    ADD CONSTRAINT estados_cronograma_pkey PRIMARY KEY (id);


--
-- Name: estados_proceso estados_proceso_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_proceso
    ADD CONSTRAINT estados_proceso_codigo_key UNIQUE (codigo);


--
-- Name: estados_proceso estados_proceso_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_proceso
    ADD CONSTRAINT estados_proceso_pkey PRIMARY KEY (id);


--
-- Name: estados_solicitud estados_solicitud_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_solicitud
    ADD CONSTRAINT estados_solicitud_codigo_key UNIQUE (codigo);


--
-- Name: estados_solicitud estados_solicitud_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estados_solicitud
    ADD CONSTRAINT estados_solicitud_pkey PRIMARY KEY (id);


--
-- Name: estudiante estudiante_expediente_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT estudiante_expediente_codigo_key UNIQUE (expediente_codigo);


--
-- Name: estudiante estudiante_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT estudiante_pkey PRIMARY KEY (id);


--
-- Name: estudiante estudiante_usuario_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT estudiante_usuario_id_key UNIQUE (usuario_id);


--
-- Name: evaluaciones_criterio evaluaciones_criterio_evaluador_id_criterio_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT evaluaciones_criterio_evaluador_id_criterio_id_key UNIQUE (evaluador_id, criterio_id);


--
-- Name: evaluaciones_criterio evaluaciones_criterio_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT evaluaciones_criterio_pkey PRIMARY KEY (id);


--
-- Name: evaluaciones_finales evaluaciones_finales_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_finales
    ADD CONSTRAINT evaluaciones_finales_pkey PRIMARY KEY (id);


--
-- Name: evaluaciones_finales evaluaciones_finales_solicitud_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_finales
    ADD CONSTRAINT evaluaciones_finales_solicitud_id_key UNIQUE (solicitud_id);


--
-- Name: evaluaciones_jurado evaluaciones_jurado_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_jurado
    ADD CONSTRAINT evaluaciones_jurado_pkey PRIMARY KEY (id);


--
-- Name: evaluaciones evaluaciones_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones
    ADD CONSTRAINT evaluaciones_pkey PRIMARY KEY (id);


--
-- Name: evaluaciones evaluaciones_solicitud_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones
    ADD CONSTRAINT evaluaciones_solicitud_id_key UNIQUE (solicitud_id);


--
-- Name: evaluadores evaluadores_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluadores
    ADD CONSTRAINT evaluadores_pkey PRIMARY KEY (id);


--
-- Name: facultades facultades_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.facultades
    ADD CONSTRAINT facultades_codigo_key UNIQUE (codigo);


--
-- Name: facultades facultades_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.facultades
    ADD CONSTRAINT facultades_pkey PRIMARY KEY (id);


--
-- Name: historial_cronograma historial_cronograma_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_cronograma
    ADD CONSTRAINT historial_cronograma_pkey PRIMARY KEY (id);


--
-- Name: historial_estados_acta historial_estados_acta_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_acta
    ADD CONSTRAINT historial_estados_acta_pkey PRIMARY KEY (id);


--
-- Name: historial_estados_solicitud historial_estados_solicitud_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_solicitud
    ADD CONSTRAINT historial_estados_solicitud_pkey PRIMARY KEY (id);


--
-- Name: jornadas jornadas_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.jornadas
    ADD CONSTRAINT jornadas_codigo_key UNIQUE (codigo);


--
-- Name: jornadas jornadas_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.jornadas
    ADD CONSTRAINT jornadas_pkey PRIMARY KEY (id);


--
-- Name: lineas_investigacion lineas_investigacion_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.lineas_investigacion
    ADD CONSTRAINT lineas_investigacion_codigo_key UNIQUE (codigo);


--
-- Name: lineas_investigacion lineas_investigacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.lineas_investigacion
    ADD CONSTRAINT lineas_investigacion_pkey PRIMARY KEY (id);


--
-- Name: miembros_tribunal miembros_tribunal_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.miembros_tribunal
    ADD CONSTRAINT miembros_tribunal_pkey PRIMARY KEY (id);


--
-- Name: miembros_tribunal miembros_tribunal_solicitud_id_docente_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.miembros_tribunal
    ADD CONSTRAINT miembros_tribunal_solicitud_id_docente_id_key UNIQUE (solicitud_id, docente_id);


--
-- Name: modalidades_titulacion modalidades_titulacion_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.modalidades_titulacion
    ADD CONSTRAINT modalidades_titulacion_codigo_key UNIQUE (codigo);


--
-- Name: modalidades_titulacion modalidades_titulacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.modalidades_titulacion
    ADD CONSTRAINT modalidades_titulacion_pkey PRIMARY KEY (id);


--
-- Name: notificaciones notificaciones_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.notificaciones
    ADD CONSTRAINT notificaciones_pkey PRIMARY KEY (id);


--
-- Name: periodos_academicos periodos_academicos_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.periodos_academicos
    ADD CONSTRAINT periodos_academicos_codigo_key UNIQUE (codigo);


--
-- Name: periodos_academicos periodos_academicos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.periodos_academicos
    ADD CONSTRAINT periodos_academicos_pkey PRIMARY KEY (id);


--
-- Name: permisos permisos_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.permisos
    ADD CONSTRAINT permisos_codigo_key UNIQUE (codigo);


--
-- Name: permisos permisos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.permisos
    ADD CONSTRAINT permisos_pkey PRIMARY KEY (id);


--
-- Name: progreso_estudiante progreso_estudiante_estudiante_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.progreso_estudiante
    ADD CONSTRAINT progreso_estudiante_estudiante_id_key UNIQUE (estudiante_id);


--
-- Name: progreso_estudiante progreso_estudiante_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.progreso_estudiante
    ADD CONSTRAINT progreso_estudiante_pkey PRIMARY KEY (id);


--
-- Name: recursos_titulacion recursos_titulacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.recursos_titulacion
    ADD CONSTRAINT recursos_titulacion_pkey PRIMARY KEY (id);


--
-- Name: respaldo_config respaldo_config_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.respaldo_config
    ADD CONSTRAINT respaldo_config_pkey PRIMARY KEY (id);


--
-- Name: respaldo_prueba_restauracion respaldo_prueba_restauracion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.respaldo_prueba_restauracion
    ADD CONSTRAINT respaldo_prueba_restauracion_pkey PRIMARY KEY (id);


--
-- Name: resultados_evaluacion resultados_evaluacion_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.resultados_evaluacion
    ADD CONSTRAINT resultados_evaluacion_codigo_key UNIQUE (codigo);


--
-- Name: resultados_evaluacion resultados_evaluacion_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.resultados_evaluacion
    ADD CONSTRAINT resultados_evaluacion_pkey PRIMARY KEY (id);


--
-- Name: rol_permisos rol_permisos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.rol_permisos
    ADD CONSTRAINT rol_permisos_pkey PRIMARY KEY (rol_id, permiso_id);


--
-- Name: roles_jurado roles_jurado_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.roles_jurado
    ADD CONSTRAINT roles_jurado_codigo_key UNIQUE (codigo);


--
-- Name: roles_jurado roles_jurado_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.roles_jurado
    ADD CONSTRAINT roles_jurado_pkey PRIMARY KEY (id);


--
-- Name: roles_usuario roles_usuario_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.roles_usuario
    ADD CONSTRAINT roles_usuario_codigo_key UNIQUE (codigo);


--
-- Name: roles_usuario roles_usuario_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.roles_usuario
    ADD CONSTRAINT roles_usuario_pkey PRIMARY KEY (id);


--
-- Name: rubricas rubricas_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.rubricas
    ADD CONSTRAINT rubricas_pkey PRIMARY KEY (id);


--
-- Name: sala sala_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.sala
    ADD CONSTRAINT sala_codigo_key UNIQUE (codigo);


--
-- Name: sala sala_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.sala
    ADD CONSTRAINT sala_pkey PRIMARY KEY (id);


--
-- Name: solicitud solicitud_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT solicitud_pkey PRIMARY KEY (id);


--
-- Name: temas_guardados temas_guardados_estudiante_id_tema_propuesto_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_guardados
    ADD CONSTRAINT temas_guardados_estudiante_id_tema_propuesto_id_key UNIQUE (estudiante_id, tema_propuesto_id);


--
-- Name: temas_guardados temas_guardados_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_guardados
    ADD CONSTRAINT temas_guardados_pkey PRIMARY KEY (id);


--
-- Name: temas_propuestos temas_propuestos_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_propuestos
    ADD CONSTRAINT temas_propuestos_pkey PRIMARY KEY (id);


--
-- Name: tipos_evaluador tipos_evaluador_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tipos_evaluador
    ADD CONSTRAINT tipos_evaluador_codigo_key UNIQUE (codigo);


--
-- Name: tipos_evaluador tipos_evaluador_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tipos_evaluador
    ADD CONSTRAINT tipos_evaluador_pkey PRIMARY KEY (id);


--
-- Name: tipos_mensaje tipos_mensaje_codigo_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tipos_mensaje
    ADD CONSTRAINT tipos_mensaje_codigo_key UNIQUE (codigo);


--
-- Name: tipos_mensaje tipos_mensaje_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tipos_mensaje
    ADD CONSTRAINT tipos_mensaje_pkey PRIMARY KEY (id);


--
-- Name: tutores tutores_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT tutores_pkey PRIMARY KEY (id);


--
-- Name: tutores tutores_solicitud_id_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT tutores_solicitud_id_key UNIQUE (solicitud_id);


--
-- Name: tutoria_fases tutoria_fases_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_fases
    ADD CONSTRAINT tutoria_fases_pkey PRIMARY KEY (id);


--
-- Name: tutoria_mensajes tutoria_mensajes_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_mensajes
    ADD CONSTRAINT tutoria_mensajes_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (email);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: ix_actas_estado; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_actas_estado ON presus.actas USING btree (estado_id);


--
-- Name: ix_anteproyectos_estado; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_anteproyectos_estado ON presus.anteproyectos USING btree (estado);


--
-- Name: ix_auditoria_fecha; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_auditoria_fecha ON presus.auditoria USING btree (fecha DESC);


--
-- Name: ix_auditoria_tabla_fecha; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_auditoria_tabla_fecha ON presus.auditoria USING btree (tabla, fecha DESC);


--
-- Name: ix_auditoria_usuario; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_auditoria_usuario ON presus.auditoria USING btree (usuario_id);


--
-- Name: ix_cronograma_solicitud; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_cronograma_solicitud ON presus.cronograma USING btree (solicitud_id);


--
-- Name: ix_docente_facultad; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_docente_facultad ON presus.docente USING btree (facultad_id);


--
-- Name: ix_estudiante_carrera; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_estudiante_carrera ON presus.estudiante USING btree (carrera_id);


--
-- Name: ix_evaluaciones_criterio_jurado; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_evaluaciones_criterio_jurado ON presus.evaluaciones_criterio USING btree (jurado_id);


--
-- Name: ix_evaluaciones_criterio_solicitud; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_evaluaciones_criterio_solicitud ON presus.evaluaciones_criterio USING btree (solicitud_id);


--
-- Name: ix_evaluaciones_jurado_jurado; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_evaluaciones_jurado_jurado ON presus.evaluaciones_jurado USING btree (jurado_id);


--
-- Name: ix_evaluadores_docente; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_evaluadores_docente ON presus.evaluadores USING btree (docente_id);


--
-- Name: ix_historial_acta_acta_fecha; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_historial_acta_acta_fecha ON presus.historial_estados_acta USING btree (acta_id, fecha_cambio DESC);


--
-- Name: ix_historial_acta_usuario; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_historial_acta_usuario ON presus.historial_estados_acta USING btree (usuario_id);


--
-- Name: ix_historial_estados_solicitud_solicitud; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_historial_estados_solicitud_solicitud ON presus.historial_estados_solicitud USING btree (solicitud_id);


--
-- Name: ix_miembros_tribunal_docente; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_miembros_tribunal_docente ON presus.miembros_tribunal USING btree (docente_id);


--
-- Name: ix_notificaciones_leida_fecha; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_notificaciones_leida_fecha ON presus.notificaciones USING btree (leida, fecha DESC);


--
-- Name: ix_notificaciones_usuario; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_notificaciones_usuario ON presus.notificaciones USING btree (usuario_id);


--
-- Name: ix_respaldo_prueba_fecha; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_respaldo_prueba_fecha ON presus.respaldo_prueba_restauracion USING btree (fecha DESC);


--
-- Name: ix_solicitud_estudiante; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_solicitud_estudiante ON presus.solicitud USING btree (estudiante_id);


--
-- Name: ix_tutores_docente; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_tutores_docente ON presus.tutores USING btree (docente_id);


--
-- Name: ix_tutoria_fases_tutor; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_tutoria_fases_tutor ON presus.tutoria_fases USING btree (tutor_id);


--
-- Name: ix_tutoria_mensajes_fase; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_tutoria_mensajes_fase ON presus.tutoria_mensajes USING btree (fase_id);


--
-- Name: ix_usuarios_rol; Type: INDEX; Schema: presus; Owner: -
--

CREATE INDEX ix_usuarios_rol ON presus.usuarios USING btree (rol_id);


--
-- Name: actas trg_auditoria_actas; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_actas AFTER INSERT OR DELETE OR UPDATE ON presus.actas FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: carrera_linea_investigacion trg_auditoria_carrera_linea_investigacion; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_carrera_linea_investigacion AFTER INSERT OR DELETE OR UPDATE ON presus.carrera_linea_investigacion FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: carreras trg_auditoria_carreras; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_carreras AFTER INSERT OR DELETE OR UPDATE ON presus.carreras FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: estudiante trg_auditoria_estudiante; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_estudiante AFTER INSERT OR DELETE OR UPDATE ON presus.estudiante FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: evaluaciones_finales trg_auditoria_evaluaciones_finales; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_evaluaciones_finales AFTER INSERT OR DELETE OR UPDATE ON presus.evaluaciones_finales FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: facultades trg_auditoria_facultades; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_facultades AFTER INSERT OR DELETE OR UPDATE ON presus.facultades FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: modalidades_titulacion trg_auditoria_modalidades_titulacion; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_modalidades_titulacion AFTER INSERT OR DELETE OR UPDATE ON presus.modalidades_titulacion FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: periodos_academicos trg_auditoria_periodos_academicos; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_periodos_academicos AFTER INSERT OR DELETE OR UPDATE ON presus.periodos_academicos FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: permisos trg_auditoria_permisos; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_permisos AFTER INSERT OR DELETE OR UPDATE ON presus.permisos FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: progreso_estudiante trg_auditoria_progreso_estudiante; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_progreso_estudiante AFTER INSERT OR DELETE OR UPDATE ON presus.progreso_estudiante FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: recursos_titulacion trg_auditoria_recursos_titulacion; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_recursos_titulacion AFTER INSERT OR DELETE OR UPDATE ON presus.recursos_titulacion FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: respaldo_config trg_auditoria_respaldo_config; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_respaldo_config AFTER INSERT OR DELETE OR UPDATE ON presus.respaldo_config FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: rol_permisos trg_auditoria_rol_permisos; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_rol_permisos AFTER INSERT OR DELETE ON presus.rol_permisos FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_rol_permisos();


--
-- Name: roles_usuario trg_auditoria_roles_usuario; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_roles_usuario AFTER INSERT OR DELETE OR UPDATE ON presus.roles_usuario FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: solicitud trg_auditoria_solicitud; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_solicitud AFTER INSERT OR DELETE OR UPDATE ON presus.solicitud FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: temas_guardados trg_auditoria_temas_guardados; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_temas_guardados AFTER INSERT OR DELETE OR UPDATE ON presus.temas_guardados FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: temas_propuestos trg_auditoria_temas_propuestos; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_temas_propuestos AFTER INSERT OR DELETE OR UPDATE ON presus.temas_propuestos FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: usuarios trg_auditoria_usuarios; Type: TRIGGER; Schema: presus; Owner: -
--

CREATE TRIGGER trg_auditoria_usuarios AFTER INSERT OR DELETE OR UPDATE ON presus.usuarios FOR EACH ROW EXECUTE FUNCTION presus.fn_auditoria_generica();


--
-- Name: carrera_linea_investigacion carrera_linea_investigacion_carrera_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carrera_linea_investigacion
    ADD CONSTRAINT carrera_linea_investigacion_carrera_id_fkey FOREIGN KEY (carrera_id) REFERENCES presus.carreras(id) ON DELETE CASCADE;


--
-- Name: carrera_linea_investigacion carrera_linea_investigacion_linea_investigacion_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carrera_linea_investigacion
    ADD CONSTRAINT carrera_linea_investigacion_linea_investigacion_id_fkey FOREIGN KEY (linea_investigacion_id) REFERENCES presus.lineas_investigacion(id) ON DELETE CASCADE;


--
-- Name: evaluaciones_finales fk1656v7687m5a006ig2dnxdxlq; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_finales
    ADD CONSTRAINT fk1656v7687m5a006ig2dnxdxlq FOREIGN KEY (resultado_id) REFERENCES presus.resultados_evaluacion(id);


--
-- Name: solicitud fk1gfsheb54hb4313592ynmr5u4; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fk1gfsheb54hb4313592ynmr5u4 FOREIGN KEY (estudiante_id) REFERENCES presus.estudiante(id);


--
-- Name: notificaciones fk1mxbjb81ft61gwlh0kabubndc; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.notificaciones
    ADD CONSTRAINT fk1mxbjb81ft61gwlh0kabubndc FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: historial_estados_solicitud fk27v25jypoybt045dokcm47yul; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_solicitud
    ADD CONSTRAINT fk27v25jypoybt045dokcm47yul FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: historial_estados_solicitud fk28v16j834n0mxh44jrkouaard; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_solicitud
    ADD CONSTRAINT fk28v16j834n0mxh44jrkouaard FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: solicitud fk3kdfcybjca99ian0k5g1mhhoq; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fk3kdfcybjca99ian0k5g1mhhoq FOREIGN KEY (estado_id) REFERENCES presus.estados_solicitud(id);


--
-- Name: evaluaciones_jurado fk3m5avtbjkv8okijdpvnba6br8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_jurado
    ADD CONSTRAINT fk3m5avtbjkv8okijdpvnba6br8 FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: solicitud fk4o0t4min6fnjh6wtewgwetdsc; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fk4o0t4min6fnjh6wtewgwetdsc FOREIGN KEY (convocatoria_id) REFERENCES presus.convocatorias_titulacion(id);


--
-- Name: evaluaciones fk53hknbr7ctsod58a57sbk99j9; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones
    ADD CONSTRAINT fk53hknbr7ctsod58a57sbk99j9 FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: evaluaciones_jurado fk53wpgafopnaoqe0cge5mi09g8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_jurado
    ADD CONSTRAINT fk53wpgafopnaoqe0cge5mi09g8 FOREIGN KEY (jurado_id) REFERENCES presus.miembros_tribunal(id);


--
-- Name: solicitud fk5lihp07xom3g6aiv6aid1mqoy; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fk5lihp07xom3g6aiv6aid1mqoy FOREIGN KEY (actualizado_por) REFERENCES presus.usuarios(id);


--
-- Name: estudiante fk665t8l4wpufihhoes8s1938ht; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT fk665t8l4wpufihhoes8s1938ht FOREIGN KEY (periodo_ingreso_id) REFERENCES presus.periodos_academicos(id);


--
-- Name: evaluaciones_finales fk6j0y0jr90cysn9b2xomrn2907; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_finales
    ADD CONSTRAINT fk6j0y0jr90cysn9b2xomrn2907 FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: lineas_investigacion fk6mbdout54e5jy49tpaeqijaa8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.lineas_investigacion
    ADD CONSTRAINT fk6mbdout54e5jy49tpaeqijaa8 FOREIGN KEY (facultad_id) REFERENCES presus.facultades(id);


--
-- Name: estudiante fk6mibklpfbmshbwm5ly7t6quc1; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT fk6mibklpfbmshbwm5ly7t6quc1 FOREIGN KEY (carrera_id) REFERENCES presus.carreras(id);


--
-- Name: tutoria_mensajes fk6otp7q57xutoo6yhs7m5vkn19; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_mensajes
    ADD CONSTRAINT fk6otp7q57xutoo6yhs7m5vkn19 FOREIGN KEY (fase_id) REFERENCES presus.tutoria_fases(id);


--
-- Name: evaluaciones_criterio fk704dbpt9dfb9cc4q2frdk1e59; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT fk704dbpt9dfb9cc4q2frdk1e59 FOREIGN KEY (criterio_id) REFERENCES presus.criterios_rubrica(id);


--
-- Name: evaluadores fk7lfvwr8cap0mtqx82eypyrfkd; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluadores
    ADD CONSTRAINT fk7lfvwr8cap0mtqx82eypyrfkd FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: cronograma fk7wxxnuex9myk4vf3qbr8nm2qn; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT fk7wxxnuex9myk4vf3qbr8nm2qn FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: areas_tematicas fk89neyi3j42yt914y9dsepvx8g; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.areas_tematicas
    ADD CONSTRAINT fk89neyi3j42yt914y9dsepvx8g FOREIGN KEY (linea_investigacion_id) REFERENCES presus.lineas_investigacion(id);


--
-- Name: actas fk_actas_estado; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.actas
    ADD CONSTRAINT fk_actas_estado FOREIGN KEY (estado_id) REFERENCES presus.estados_acta(id);


--
-- Name: anteproyectos fk_anteproyectos_estado_id; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.anteproyectos
    ADD CONSTRAINT fk_anteproyectos_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: anteproyectos fk_anteproyectos_estado_proceso; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.anteproyectos
    ADD CONSTRAINT fk_anteproyectos_estado_proceso FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: estudiante fk_estudiante_estado_academico; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT fk_estudiante_estado_academico FOREIGN KEY (estado_academico_id) REFERENCES presus.estados_academicos(id);


--
-- Name: evaluaciones_criterio fk_evaluaciones_criterio_jurado; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT fk_evaluaciones_criterio_jurado FOREIGN KEY (jurado_id) REFERENCES presus.miembros_tribunal(id);


--
-- Name: historial_estados_acta fk_hist_acta_acta; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_acta
    ADD CONSTRAINT fk_hist_acta_acta FOREIGN KEY (acta_id) REFERENCES presus.actas(id) ON DELETE CASCADE;


--
-- Name: historial_estados_acta fk_hist_acta_est_ant; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_acta
    ADD CONSTRAINT fk_hist_acta_est_ant FOREIGN KEY (estado_anterior_id) REFERENCES presus.estados_acta(id);


--
-- Name: historial_estados_acta fk_hist_acta_est_nuevo; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_acta
    ADD CONSTRAINT fk_hist_acta_est_nuevo FOREIGN KEY (estado_nuevo_id) REFERENCES presus.estados_acta(id);


--
-- Name: historial_estados_acta fk_hist_acta_usuario; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_acta
    ADD CONSTRAINT fk_hist_acta_usuario FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: tutores fk_tutores_estado_id; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT fk_tutores_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: tutores fk_tutores_estado_proceso; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT fk_tutores_estado_proceso FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: tutoria_fases fk_tutoria_fases_estado_id; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_fases
    ADD CONSTRAINT fk_tutoria_fases_estado_id FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: tutoria_fases fk_tutoria_fases_estado_proceso; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_fases
    ADD CONSTRAINT fk_tutoria_fases_estado_proceso FOREIGN KEY (estado_id) REFERENCES presus.estados_proceso(id);


--
-- Name: tutoria_mensajes fk_tutoria_mensajes_tipo; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_mensajes
    ADD CONSTRAINT fk_tutoria_mensajes_tipo FOREIGN KEY (tipo_mensaje_id) REFERENCES presus.tipos_mensaje(id);


--
-- Name: tutoria_mensajes fk_tutoria_mensajes_tipo_mensaje; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_mensajes
    ADD CONSTRAINT fk_tutoria_mensajes_tipo_mensaje FOREIGN KEY (tipo_mensaje_id) REFERENCES presus.tipos_mensaje(id);


--
-- Name: historial_cronograma fkaou7dy35isiktfxl0wvrv24c5; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_cronograma
    ADD CONSTRAINT fkaou7dy35isiktfxl0wvrv24c5 FOREIGN KEY (sala_anterior_id) REFERENCES presus.sala(id);


--
-- Name: disponibilidad_sala fkaqxqt9i1aht1anwbwjtnxkjay; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.disponibilidad_sala
    ADD CONSTRAINT fkaqxqt9i1aht1anwbwjtnxkjay FOREIGN KEY (bloque_id) REFERENCES presus.bloques_horarios(id);


--
-- Name: solicitud fkaxmn3drihjstewcid9a1wjuw9; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fkaxmn3drihjstewcid9a1wjuw9 FOREIGN KEY (modalidad_titulacion_id) REFERENCES presus.modalidades_titulacion(id);


--
-- Name: tutores fkb2xb48y98w51rrxljckb63d92; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT fkb2xb48y98w51rrxljckb63d92 FOREIGN KEY (docente_id) REFERENCES presus.docente(id);


--
-- Name: actas fkbe1mhcpm4hh94m47oxihlkh6i; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.actas
    ADD CONSTRAINT fkbe1mhcpm4hh94m47oxihlkh6i FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: cronograma fkbp2d8c5i4pgy0mbhae0f5cj78; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT fkbp2d8c5i4pgy0mbhae0f5cj78 FOREIGN KEY (convocatoria_id) REFERENCES presus.convocatorias_titulacion(id);


--
-- Name: docente fkbs42aumodddav4mosgah3bomi; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.docente
    ADD CONSTRAINT fkbs42aumodddav4mosgah3bomi FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: solicitud fkbslas51apjrlk1sspwjxcahpd; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fkbslas51apjrlk1sspwjxcahpd FOREIGN KEY (creado_por) REFERENCES presus.usuarios(id);


--
-- Name: cronograma fkbt78p9nbvdowoh0dwisdpnk2n; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT fkbt78p9nbvdowoh0dwisdpnk2n FOREIGN KEY (estado_id) REFERENCES presus.estados_cronograma(id);


--
-- Name: historial_estados_solicitud fkcduxdoctlihmlrf7jc79hktbr; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_solicitud
    ADD CONSTRAINT fkcduxdoctlihmlrf7jc79hktbr FOREIGN KEY (estado_nuevo_id) REFERENCES presus.estados_solicitud(id);


--
-- Name: cronograma fkch4phbxqt1j7wck6fh34phj62; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT fkch4phbxqt1j7wck6fh34phj62 FOREIGN KEY (bloque_id) REFERENCES presus.bloques_horarios(id);


--
-- Name: evaluaciones fkcw8bowlxco293tni77pshk0un; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones
    ADD CONSTRAINT fkcw8bowlxco293tni77pshk0un FOREIGN KEY (rubrica_id) REFERENCES presus.rubricas(id);


--
-- Name: evaluadores fkdcv3fm9ch7f9otbph1sc0kc56; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluadores
    ADD CONSTRAINT fkdcv3fm9ch7f9otbph1sc0kc56 FOREIGN KEY (docente_id) REFERENCES presus.docente(id);


--
-- Name: disponibilidad_sala fkdnyk315lc3r3lxpja9ff5aa72; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.disponibilidad_sala
    ADD CONSTRAINT fkdnyk315lc3r3lxpja9ff5aa72 FOREIGN KEY (sala_id) REFERENCES presus.sala(id);


--
-- Name: evaluaciones_criterio fkdsk8q9dykx4cndxxmyxg679fc; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT fkdsk8q9dykx4cndxxmyxg679fc FOREIGN KEY (evaluador_id) REFERENCES presus.evaluadores(id);


--
-- Name: miembros_tribunal fkebkpg7s5whbr9va2yvfaf1act; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.miembros_tribunal
    ADD CONSTRAINT fkebkpg7s5whbr9va2yvfaf1act FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: tutoria_fases fkennbyt8lrhckfi7fwkpw8qgfm; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_fases
    ADD CONSTRAINT fkennbyt8lrhckfi7fwkpw8qgfm FOREIGN KEY (tutor_id) REFERENCES presus.tutores(id);


--
-- Name: evaluadores fkf2c67wiobryo5c5us94dxo2nj; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluadores
    ADD CONSTRAINT fkf2c67wiobryo5c5us94dxo2nj FOREIGN KEY (miembro_tribunal_id) REFERENCES presus.miembros_tribunal(id);


--
-- Name: evaluadores fkf7pg14kvrds8f3splp0a3d9m8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluadores
    ADD CONSTRAINT fkf7pg14kvrds8f3splp0a3d9m8 FOREIGN KEY (tipo_evaluador_id) REFERENCES presus.tipos_evaluador(id);


--
-- Name: solicitud fkg1jysw6105nj3u40g1ahsx14l; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fkg1jysw6105nj3u40g1ahsx14l FOREIGN KEY (linea_investigacion_id) REFERENCES presus.lineas_investigacion(id);


--
-- Name: bloques_horarios fkhimm4vkb8aaamcb8ig77qk8jr; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.bloques_horarios
    ADD CONSTRAINT fkhimm4vkb8aaamcb8ig77qk8jr FOREIGN KEY (jornada_id) REFERENCES presus.jornadas(id);


--
-- Name: historial_cronograma fkhlaxapi4vhyntrt7m1eukbvb6; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_cronograma
    ADD CONSTRAINT fkhlaxapi4vhyntrt7m1eukbvb6 FOREIGN KEY (sala_nueva_id) REFERENCES presus.sala(id);


--
-- Name: historial_cronograma fki3w1x79h0hqgek4qn3a3m2j5x; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_cronograma
    ADD CONSTRAINT fki3w1x79h0hqgek4qn3a3m2j5x FOREIGN KEY (cronograma_id) REFERENCES presus.cronograma(id);


--
-- Name: miembros_tribunal fkii8qums5v0taxx349emm7th6g; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.miembros_tribunal
    ADD CONSTRAINT fkii8qums5v0taxx349emm7th6g FOREIGN KEY (rol_jurado_id) REFERENCES presus.roles_jurado(id);


--
-- Name: evaluaciones_criterio fkiwxwbpykr3vo23lxeimiwmfji; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_criterio
    ADD CONSTRAINT fkiwxwbpykr3vo23lxeimiwmfji FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: tutoria_mensajes fkj3p4s6quhocjaojcdvw2t8ag2; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutoria_mensajes
    ADD CONSTRAINT fkj3p4s6quhocjaojcdvw2t8ag2 FOREIGN KEY (remitente_id) REFERENCES presus.usuarios(id);


--
-- Name: convocatorias_titulacion fkkhjxrhwxttrjg54fosa70k0ib; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.convocatorias_titulacion
    ADD CONSTRAINT fkkhjxrhwxttrjg54fosa70k0ib FOREIGN KEY (periodo_academico_id) REFERENCES presus.periodos_academicos(id);


--
-- Name: solicitud fkkocwlitlopkh7puo52h9tncxj; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.solicitud
    ADD CONSTRAINT fkkocwlitlopkh7puo52h9tncxj FOREIGN KEY (area_tematica_id) REFERENCES presus.areas_tematicas(id);


--
-- Name: usuarios fkku0it797v3701ntswwvl95ei; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.usuarios
    ADD CONSTRAINT fkku0it797v3701ntswwvl95ei FOREIGN KEY (rol_id) REFERENCES presus.roles_usuario(id);


--
-- Name: estudiante fkmb0c2sqmehp6spvhm53mej2b7; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.estudiante
    ADD CONSTRAINT fkmb0c2sqmehp6spvhm53mej2b7 FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: historial_cronograma fknjwyynwb33g4jaat7k03korot; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_cronograma
    ADD CONSTRAINT fknjwyynwb33g4jaat7k03korot FOREIGN KEY (usuario_id) REFERENCES presus.usuarios(id);


--
-- Name: cronograma fknkurftcc1qsc7alh7276l5uq1; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.cronograma
    ADD CONSTRAINT fknkurftcc1qsc7alh7276l5uq1 FOREIGN KEY (sala_id) REFERENCES presus.sala(id);


--
-- Name: anteproyectos fknuplmmjrbs76slhxrt6swwytj; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.anteproyectos
    ADD CONSTRAINT fknuplmmjrbs76slhxrt6swwytj FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: carreras fkpkbqfnd3xcstklurnni1v7hi8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.carreras
    ADD CONSTRAINT fkpkbqfnd3xcstklurnni1v7hi8 FOREIGN KEY (facultad_id) REFERENCES presus.facultades(id);


--
-- Name: tutores fkpva6fxtkmtc7e8y8qh3ci1jg8; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.tutores
    ADD CONSTRAINT fkpva6fxtkmtc7e8y8qh3ci1jg8 FOREIGN KEY (solicitud_id) REFERENCES presus.solicitud(id);


--
-- Name: evaluaciones_finales fkqr6nw8s42apawhtrjaufmemmk; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.evaluaciones_finales
    ADD CONSTRAINT fkqr6nw8s42apawhtrjaufmemmk FOREIGN KEY (rubrica_id) REFERENCES presus.rubricas(id);


--
-- Name: historial_estados_solicitud fkquuy4n6l595u3n1eesse65lm7; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.historial_estados_solicitud
    ADD CONSTRAINT fkquuy4n6l595u3n1eesse65lm7 FOREIGN KEY (estado_anterior_id) REFERENCES presus.estados_solicitud(id);


--
-- Name: docente fkroasnisuah9k11docjg9q46w6; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.docente
    ADD CONSTRAINT fkroasnisuah9k11docjg9q46w6 FOREIGN KEY (facultad_id) REFERENCES presus.facultades(id);


--
-- Name: miembros_tribunal fkrvimgktkif6hr7u3w0l2r345q; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.miembros_tribunal
    ADD CONSTRAINT fkrvimgktkif6hr7u3w0l2r345q FOREIGN KEY (docente_id) REFERENCES presus.docente(id);


--
-- Name: criterios_rubrica fkrwb21y79trk780jwtk34aiqr3; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.criterios_rubrica
    ADD CONSTRAINT fkrwb21y79trk780jwtk34aiqr3 FOREIGN KEY (rubrica_id) REFERENCES presus.rubricas(id);


--
-- Name: progreso_estudiante progreso_estudiante_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.progreso_estudiante
    ADD CONSTRAINT progreso_estudiante_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES presus.estudiante(id) ON DELETE CASCADE;


--
-- Name: recursos_titulacion recursos_titulacion_carrera_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.recursos_titulacion
    ADD CONSTRAINT recursos_titulacion_carrera_id_fkey FOREIGN KEY (carrera_id) REFERENCES presus.carreras(id) ON DELETE SET NULL;


--
-- Name: rol_permisos rol_permisos_permiso_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.rol_permisos
    ADD CONSTRAINT rol_permisos_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES presus.permisos(id) ON DELETE CASCADE;


--
-- Name: rol_permisos rol_permisos_rol_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.rol_permisos
    ADD CONSTRAINT rol_permisos_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES presus.roles_usuario(id) ON DELETE CASCADE;


--
-- Name: temas_guardados temas_guardados_estudiante_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_guardados
    ADD CONSTRAINT temas_guardados_estudiante_id_fkey FOREIGN KEY (estudiante_id) REFERENCES presus.estudiante(id) ON DELETE CASCADE;


--
-- Name: temas_guardados temas_guardados_tema_propuesto_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_guardados
    ADD CONSTRAINT temas_guardados_tema_propuesto_id_fkey FOREIGN KEY (tema_propuesto_id) REFERENCES presus.temas_propuestos(id) ON DELETE CASCADE;


--
-- Name: temas_propuestos temas_propuestos_area_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_propuestos
    ADD CONSTRAINT temas_propuestos_area_id_fkey FOREIGN KEY (area_id) REFERENCES presus.areas_tematicas(id) ON DELETE CASCADE;


--
-- Name: temas_propuestos temas_propuestos_carrera_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_propuestos
    ADD CONSTRAINT temas_propuestos_carrera_id_fkey FOREIGN KEY (carrera_id) REFERENCES presus.carreras(id) ON DELETE CASCADE;


--
-- Name: temas_propuestos temas_propuestos_linea_investigacion_id_fkey; Type: FK CONSTRAINT; Schema: presus; Owner: -
--

ALTER TABLE ONLY presus.temas_propuestos
    ADD CONSTRAINT temas_propuestos_linea_investigacion_id_fkey FOREIGN KEY (linea_investigacion_id) REFERENCES presus.lineas_investigacion(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--




-- =============================================================================
--  DATOS DE CATÁLOGO (tablas de referencia)
-- -----------------------------------------------------------------------------
--  Roles, permisos, estados, carreras, líneas de investigación, convocatorias,
--  salas, rúbricas y sus criterios, etc. Son los mismos valores que siembran
--  las migraciones Flyway. Necesarios para poder cargar database/datos_masivos.sql.
-- =============================================================================

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- Sembrar catálogos sin que los disparadores de auditoría registren estas altas.
SET session_replication_role = replica;

--
-- Data for Name: facultades; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.facultades (id, codigo, nombre) VALUES (1, 'FCI', 'Facultad de Ciencias de la Ingeniería');
INSERT INTO presus.facultades (id, codigo, nombre) VALUES (3, 'FCE', 'Facultad de Ciencias de la Educacion');


--
-- Data for Name: lineas_investigacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.lineas_investigacion (facultad_id, id, codigo, nombre, descripcion) VALUES (1, 1, 'ISW-CAL', 'Ingeniería de Software y Calidad', 'Procesos, arquitectura, pruebas y calidad de software.');
INSERT INTO presus.lineas_investigacion (facultad_id, id, codigo, nombre, descripcion) VALUES (1, 2, 'ISW-IA', 'Inteligencia Artificial y Ciencia de Datos', 'Aprendizaje automático, minería de datos y sistemas inteligentes.');
INSERT INTO presus.lineas_investigacion (facultad_id, id, codigo, nombre, descripcion) VALUES (1, 3, 'ISW-WEB', 'Tecnologías Web y Móviles', 'Desarrollo de aplicaciones web, móviles y arquitecturas distribuidas.');
INSERT INTO presus.lineas_investigacion (facultad_id, id, codigo, nombre, descripcion) VALUES (1, 4, 'ISW-SEG', 'Redes y Seguridad Informática', 'Infraestructura de redes, ciberseguridad y protección de datos.');


--
-- Data for Name: areas_tematicas; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (1, 1, 'Arquitectura y patrones de diseño', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (2, 1, 'Automatización de pruebas de software', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (3, 2, 'Procesamiento de lenguaje natural', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (4, 2, 'Sistemas de recomendación', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (5, 3, 'Desarrollo móvil multiplataforma', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (6, 3, 'Aplicaciones web progresivas', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (7, 4, 'Criptografía aplicada', NULL);
INSERT INTO presus.areas_tematicas (id, linea_investigacion_id, nombre, descripcion) VALUES (8, 4, 'Auditoría y análisis de vulnerabilidades', NULL);


--
-- Data for Name: jornadas; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.jornadas (id, codigo, nombre) VALUES (1, 'MATUTINA', 'Matutina');
INSERT INTO presus.jornadas (id, codigo, nombre) VALUES (2, 'VESPERTINA', 'Vespertina');
INSERT INTO presus.jornadas (id, codigo, nombre) VALUES (3, 'NOCTURNA', 'Nocturna');


--
-- Data for Name: bloques_horarios; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('09:00:00', '07:00:00', 1, 1, 'Bloque 1');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('11:00:00', '09:00:00', 2, 1, 'Bloque 2');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('13:00:00', '11:00:00', 3, 1, 'Bloque 3');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('15:00:00', '13:00:00', 4, 1, 'Bloque 4');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('09:00:00', '07:00:00', 5, 2, 'Bloque 1');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('11:00:00', '09:00:00', 6, 2, 'Bloque 2');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('13:00:00', '11:00:00', 7, 2, 'Bloque 3');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('15:00:00', '13:00:00', 8, 2, 'Bloque 4');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('09:00:00', '07:00:00', 9, 3, 'Bloque 1');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('11:00:00', '09:00:00', 10, 3, 'Bloque 2');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('13:00:00', '11:00:00', 11, 3, 'Bloque 3');
INSERT INTO presus.bloques_horarios (hora_fin, hora_inicio, id, jornada_id, nombre) VALUES ('15:00:00', '13:00:00', 12, 3, 'Bloque 4');


--
-- Data for Name: carreras; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.carreras (facultad_id, id, codigo, modalidad_estudio, nombre) VALUES (3, 4, 'EDB', 'Presencial', 'Educacion Basica');
INSERT INTO presus.carreras (facultad_id, id, codigo, modalidad_estudio, nombre) VALUES (1, 1, 'ISW', 'Presencial', 'Ingeniería en Software');


--
-- Data for Name: carrera_linea_investigacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.carrera_linea_investigacion (carrera_id, linea_investigacion_id) VALUES (1, 1);
INSERT INTO presus.carrera_linea_investigacion (carrera_id, linea_investigacion_id) VALUES (1, 2);
INSERT INTO presus.carrera_linea_investigacion (carrera_id, linea_investigacion_id) VALUES (1, 3);
INSERT INTO presus.carrera_linea_investigacion (carrera_id, linea_investigacion_id) VALUES (1, 4);


--
-- Data for Name: periodos_academicos; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (false, '2023-08-28', '2023-03-01', 1, 'PA-2023-1', 'Periodo Académico 2023-1');
INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (false, '2023-02-28', '2023-09-01', 2, 'PA-2023-2', 'Periodo Académico 2023-2');
INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (false, '2024-08-28', '2024-03-01', 3, 'PA-2024-1', 'Periodo Académico 2024-1');
INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (false, '2024-02-28', '2024-09-01', 4, 'PA-2024-2', 'Periodo Académico 2024-2');
INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (false, '2025-08-28', '2025-03-01', 5, 'PA-2025-1', 'Periodo Académico 2025-1');
INSERT INTO presus.periodos_academicos (activo, fecha_fin, fecha_inicio, id, codigo, nombre) VALUES (true, '2025-02-28', '2025-09-01', 6, 'PA-2025-2', 'Periodo Académico 2025-2');


--
-- Data for Name: convocatorias_titulacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-02-15', '2023-01-01', 1, 1, 'CONV-1', 'Convocatoria de Titulación 1');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-04-16', '2023-03-02', 2, 1, 'CONV-2', 'Convocatoria de Titulación 2');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-06-15', '2023-05-01', 3, 2, 'CONV-3', 'Convocatoria de Titulación 3');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-08-14', '2023-06-30', 4, 2, 'CONV-4', 'Convocatoria de Titulación 4');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-10-13', '2023-08-29', 5, 3, 'CONV-5', 'Convocatoria de Titulación 5');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2023-12-12', '2023-10-28', 6, 3, 'CONV-6', 'Convocatoria de Titulación 6');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2024-02-10', '2023-12-27', 7, 4, 'CONV-7', 'Convocatoria de Titulación 7');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2024-04-10', '2024-02-25', 8, 4, 'CONV-8', 'Convocatoria de Titulación 8');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2024-06-09', '2024-04-25', 9, 5, 'CONV-9', 'Convocatoria de Titulación 9');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2024-08-08', '2024-06-24', 10, 5, 'CONV-10', 'Convocatoria de Titulación 10');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (false, '2024-10-07', '2024-08-23', 11, 6, 'CONV-11', 'Convocatoria de Titulación 11');
INSERT INTO presus.convocatorias_titulacion (activa, fecha_fin, fecha_inicio, id, periodo_academico_id, codigo, nombre) VALUES (true, '2024-12-06', '2024-10-22', 12, 6, 'CONV-12', 'Convocatoria de Titulación 12');


--
-- Data for Name: rubricas; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 1, 'Rúbrica de Pre-sustentación v1', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 1');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 2, 'Rúbrica de Pre-sustentación v2', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 2');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 3, 'Rúbrica de Pre-sustentación v3', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 3');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 4, 'Rúbrica de Pre-sustentación v4', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 4');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 5, 'Rúbrica de Pre-sustentación v5', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 5');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 6, 'Rúbrica de Pre-sustentación v6', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 6');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 7, 'Rúbrica de Pre-sustentación v7', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 7');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 8, 'Rúbrica de Pre-sustentación v8', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 8');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 9, 'Rúbrica de Pre-sustentación v9', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 9');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 10, 'Rúbrica de Pre-sustentación v10', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 10');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 11, 'Rúbrica de Pre-sustentación v11', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 11');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 12, 'Rúbrica de Pre-sustentación v12', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 12');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 13, 'Rúbrica de Pre-sustentación v13', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 13');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 14, 'Rúbrica de Pre-sustentación v14', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 14');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 15, 'Rúbrica de Pre-sustentación v15', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 15');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 16, 'Rúbrica de Pre-sustentación v16', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 16');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 17, 'Rúbrica de Pre-sustentación v17', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 17');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 18, 'Rúbrica de Pre-sustentación v18', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 18');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 19, 'Rúbrica de Pre-sustentación v19', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 19');
INSERT INTO presus.rubricas (puntaje_maximo, id, nombre, descripcion) VALUES (100, 20, 'Rúbrica de Pre-sustentación v20', 'Rúbrica estándar de evaluación de pre-sustentaciones, versión 20');


--
-- Data for Name: criterios_rubrica; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 1, 1, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 2, 1, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 3, 1, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 4, 1, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 5, 1, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 6, 1, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 7, 2, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 8, 2, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 9, 2, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 10, 2, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 11, 2, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 12, 2, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 13, 3, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 14, 3, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 15, 3, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 16, 3, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 17, 3, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 18, 3, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 19, 4, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 20, 4, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 21, 4, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 22, 4, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 23, 4, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 24, 4, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 25, 5, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 26, 5, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 27, 5, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 28, 5, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 29, 5, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 30, 5, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 31, 6, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 32, 6, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 33, 6, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 34, 6, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 35, 6, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 36, 6, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 37, 7, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 38, 7, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 39, 7, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 40, 7, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 41, 7, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 42, 7, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 43, 8, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 44, 8, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 45, 8, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 46, 8, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 47, 8, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 48, 8, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 49, 9, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 50, 9, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 51, 9, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 52, 9, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 53, 9, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 54, 9, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 55, 10, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 56, 10, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 57, 10, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 58, 10, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 59, 10, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 60, 10, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 61, 11, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 62, 11, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 63, 11, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 64, 11, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 65, 11, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 66, 11, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 67, 12, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 68, 12, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 69, 12, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 70, 12, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 71, 12, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 72, 12, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 73, 13, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 74, 13, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 75, 13, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 76, 13, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 77, 13, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 78, 13, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 79, 14, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 80, 14, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 81, 14, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 82, 14, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 83, 14, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 84, 14, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 85, 15, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 86, 15, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 87, 15, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 88, 15, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 89, 15, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 90, 15, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 91, 16, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 92, 16, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 93, 16, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 94, 16, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 95, 16, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 96, 16, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 97, 17, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 98, 17, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 99, 17, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 100, 17, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 101, 17, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 102, 17, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 103, 18, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 104, 18, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 105, 18, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 106, 18, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 107, 18, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 108, 18, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 109, 19, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 110, 19, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 111, 19, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 112, 19, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 113, 19, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 114, 19, 'Respuesta a preguntas', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (1, 16.67, 115, 20, 'Estructura', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (2, 16.67, 116, 20, 'Fundamentación teórica', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (3, 16.67, 117, 20, 'Metodología', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (4, 16.67, 118, 20, 'Claridad de exposición', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (5, 16.67, 119, 20, 'Manejo del tema', NULL);
INSERT INTO presus.criterios_rubrica (orden, ponderacion, id, rubrica_id, nombre, descripcion) VALUES (6, 16.67, 120, 20, 'Respuesta a preguntas', NULL);


--
-- Data for Name: estados_academicos; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES (1, 'ACTIVO', 'Activo');
INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES (2, 'EGRESADO', 'Egresado');
INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES (3, 'GRADUADO', 'Graduado');
INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES (4, 'RETIRADO', 'Retirado');
INSERT INTO presus.estados_academicos (id, codigo, nombre) VALUES (5, 'SUSPENDIDO', 'Suspendido');


--
-- Data for Name: estados_acta; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.estados_acta (id, codigo, nombre, orden) VALUES (1, 'GENERADA', 'Generada', 1);
INSERT INTO presus.estados_acta (id, codigo, nombre, orden) VALUES (2, 'REVISADA', 'Revisada', 2);
INSERT INTO presus.estados_acta (id, codigo, nombre, orden) VALUES (3, 'OBSERVADA', 'Observada', 3);
INSERT INTO presus.estados_acta (id, codigo, nombre, orden) VALUES (4, 'FINALIZADA', 'Finalizada', 4);
INSERT INTO presus.estados_acta (id, codigo, nombre, orden) VALUES (5, 'ANULADA', 'Anulada', 5);


--
-- Data for Name: estados_cronograma; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.estados_cronograma (id, codigo, nombre) VALUES (1, 'PROGRAMADO', 'Programado');
INSERT INTO presus.estados_cronograma (id, codigo, nombre) VALUES (2, 'REALIZADO', 'Realizado');
INSERT INTO presus.estados_cronograma (id, codigo, nombre) VALUES (3, 'CANCELADO', 'Cancelado');


--
-- Data for Name: estados_proceso; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES (1, 'PENDIENTE', 'Pendiente');
INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES (2, 'EN_PROCESO', 'En proceso');
INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES (3, 'APROBADO', 'Aprobado');
INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES (4, 'OBSERVADO', 'Observado');
INSERT INTO presus.estados_proceso (id, codigo, nombre) VALUES (5, 'RECHAZADO', 'Rechazado');


--
-- Data for Name: estados_solicitud; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (1, 1, 'CREADA', 'Creada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (2, 2, 'ENVIADA', 'Enviada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (3, 3, 'TUTORIA', 'En tutoría');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (4, 4, 'EVALUACION', 'En evaluación');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (5, 5, 'CALIFICADA', 'Calificada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (6, 6, 'APROBADA', 'Aprobada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (7, 7, 'COMPLETADA', 'Completada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (8, 8, 'RECHAZADA', 'Rechazada');
INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES (9, 9, 'SUSPENDIDA', 'Suspendida');


--
-- Data for Name: modalidades_titulacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.modalidades_titulacion (id, codigo, nombre) VALUES (1, 'PROYECTO', 'Proyecto Tecnológico');
INSERT INTO presus.modalidades_titulacion (id, codigo, nombre) VALUES (2, 'ARTICULO', 'Artículo Académico');
INSERT INTO presus.modalidades_titulacion (id, codigo, nombre) VALUES (3, 'EXAMEN_COMPLEXIVO', 'Examen de Grado o de Fin de Carrera (Complexivo)');


--
-- Data for Name: permisos; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (1, 'USUARIOS_GESTIONAR', 'Gestionar usuarios', 'Administración', 'Crear, editar, activar/desactivar, eliminar usuarios y asignar sus roles');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (2, 'ROLES_PERMISOS_GESTIONAR', 'Gestionar roles y permisos', 'Administración', 'Crear/renombrar/eliminar roles y asignar permisos a cada rol');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (3, 'NOTIFICACIONES_GLOBAL_VER', 'Ver notificaciones globales', 'Administración', 'Ver el listado completo de notificaciones de todo el sistema');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (4, 'NOTIFICACIONES_ENVIAR', 'Enviar notificaciones', 'Administración', 'Crear notificaciones manuales dirigidas a otros usuarios');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (5, 'SOLICITUDES_REVISAR', 'Revisar solicitudes', 'Solicitudes', 'Aprobar, rechazar y listar solicitudes de pre-sustentación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (6, 'SOLICITUDES_SUSPENDER', 'Suspender solicitudes', 'Solicitudes', 'Suspender una solicitud en curso');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (7, 'TRIBUNAL_TUTOR_ASIGNAR', 'Asignar tutor y tribunal', 'Tribunal', 'Asignar o eliminar el tutor y los jurados de una solicitud');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (8, 'CRONOGRAMA_GESTIONAR', 'Gestionar cronograma', 'Cronograma', 'Programar, reprogramar y eliminar fechas de defensa');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (9, 'SALA_GESTIONAR', 'Gestionar salas', 'Cronograma', 'Crear y eliminar salas de sustentación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (10, 'RUBRICA_GESTIONAR', 'Gestionar rúbricas', 'Evaluación', 'Crear, editar y eliminar rúbricas y sus criterios');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (11, 'EVALUACION_CALIFICAR', 'Calificar solicitudes', 'Evaluación', 'Registrar la nota final ponderada de una solicitud');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (12, 'EVALUACION_RUBRICA_REGISTRAR', 'Registrar evaluación de rúbrica', 'Evaluación', 'Registrar la evaluación de un jurado/instructor por criterio');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (13, 'ACTA_GENERAR', 'Generar acta', 'Actas', 'Generar el acta de pre-sustentación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (14, 'ACTA_FIRMAR', 'Firmar acta', 'Actas', 'Firmar digitalmente el acta de pre-sustentación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (15, 'ANTEPROYECTO_REVISAR', 'Revisar anteproyecto', 'Tutoría', 'Aprobar o rechazar el anteproyecto de un estudiante');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (16, 'TUTORIA_GESTIONAR', 'Gestionar tutoría', 'Tutoría', 'Crear y aprobar fases de tutoría como tutor');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (17, 'TUTORIA_AVANCE_ESTUDIANTE', 'Registrar avance de tutoría', 'Tutoría', 'Subir correcciones/avances como estudiante en una fase de tutoría');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (18, 'REPORTES_VER', 'Ver reportes', 'Reportes', 'Generar reportes en PDF y estadísticas del sistema');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (19, 'AUDITORIA_VER', 'Ver auditoría', 'Administración', 'Consultar el historial de quién creó, modificó, aprobó o eliminó información y cuándo');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (20, 'ESTUDIANTES_GESTIONAR', 'Gestionar estudiantes', 'Estudiantes', 'Registrar estudiantes y editar carrera, semestre, período de ingreso y estado académico');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (21, 'CARRERAS_GESTIONAR', 'Gestionar carreras', 'Carreras', 'Administrar facultades, carreras, modalidades de titulación y períodos académicos');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (22, 'ACTAS_VER', 'Ver actas', 'Actas', 'Listar y consultar el detalle de las actas de pre-sustentación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (23, 'ACTAS_VER_PROPIAS', 'Ver actas propias', 'Actas', 'Que un docente vea las actas de las pre-sustentaciones en las que es tutor o jurado');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (24, 'ACTA_HISTORIAL_VER', 'Ver historial de acta', 'Actas', 'Consultar el historial de cambios de estado de un acta');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (25, 'ACTA_ESTADO_CAMBIAR', 'Cambiar estado de acta', 'Actas', 'Cambiar el estado de un acta (revisar, observar, finalizar, anular) según las reglas del proceso');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (26, 'ACTAS_GESTIONAR', 'Gestionar actas', 'Actas', 'Gestión administrativa completa de actas: buscar, filtrar, corregir y cambiar estado');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (27, 'ORIENTACION_CATALOGO_GESTIONAR', 'Gestionar catálogo de orientación', 'Orientación', 'Crear, editar y eliminar temas propuestos y recursos de titulación');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (28, 'ORIENTACION_TEMAS_VER', 'Ver temas propuestos', 'Orientación', 'Explorar el catálogo de temas de titulación propuestos y su detalle');
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES (29, 'BACKUPS_GESTIONAR', 'Gestionar respaldos de base de datos', 'Administración', 'Generar, descargar, restaurar y eliminar respaldos (dumps) completos de la base de datos');


--
-- Data for Name: recursos_titulacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (1, 'Plantilla oficial de anteproyecto de titulación', 'Plantillas', 'https://uteq.edu.ec/titulacion/plantilla-anteproyecto.docx', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (2, 'Guía para redactar el planteamiento del problema', 'Guías', 'https://uteq.edu.ec/titulacion/guia-planteamiento-problema.pdf', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (3, 'Normas APA 7ma edición — resumen práctico', 'Guías', 'https://uteq.edu.ec/titulacion/normas-apa-7.pdf', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (4, 'Reglamento de titulación vigente', 'Reglamentos', 'https://uteq.edu.ec/titulacion/reglamento-titulacion.pdf', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (5, 'Rúbrica de evaluación de la pre-sustentación', 'Reglamentos', 'https://uteq.edu.ec/titulacion/rubrica-pre-sustentacion.pdf', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (6, 'Cronograma general del proceso de titulación', 'Cronogramas', 'https://uteq.edu.ec/titulacion/cronograma-titulacion.pdf', NULL);
INSERT INTO presus.recursos_titulacion (id, titulo, categoria, url_archivo, carrera_id) VALUES (7, 'Lista de verificación previa a la entrega del anteproyecto', 'Checklists', 'https://uteq.edu.ec/titulacion/checklist-anteproyecto.pdf', NULL);


--
-- Data for Name: respaldo_config; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.respaldo_config (id, activo, cron, retener_diarios, retener_semanales, retener_mensuales, actualizado_en, actualizado_por, retener_dias_wal, diferencial_activo, cron_diferencial) VALUES (1, true, '0 0 23 * * SUN', 7, 5, 8, '2026-09-07 20:07:43.123037', 'admin@uteq.edu.ec', 14, false, '0 30 2 * * WED,FRI');


--
-- Data for Name: resultados_evaluacion; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.resultados_evaluacion (id, codigo, nombre) VALUES (1, 'APROBADO', 'Aprobado');
INSERT INTO presus.resultados_evaluacion (id, codigo, nombre) VALUES (2, 'REPROBADO', 'Reprobado');


--
-- Data for Name: roles_usuario; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (1, 'ADMIN', 'Administrador');
INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (2, 'DOCENTE', 'Docente');
INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (3, 'COORDINADOR', 'Coordinador');
INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (4, 'ESTUDIANTE', 'Estudiante');
INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (5, 'SECRETARIO', 'Secretario biblioteca');
INSERT INTO presus.roles_usuario (id, codigo, nombre) VALUES (6, 'EJEMPLO', 'Ejemplo1');


--
-- Data for Name: rol_permisos; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 1);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 2);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 3);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 4);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 5);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 6);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 7);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 8);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 9);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 10);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 11);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 12);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 13);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 14);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 15);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 16);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 17);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 18);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 19);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 20);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 21);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (5, 13);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (5, 14);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (5, 18);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 22);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 23);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 24);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 25);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 26);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 27);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 28);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (1, 29);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (4, 6);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (4, 17);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (4, 28);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 5);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 12);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 14);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 15);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 16);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 23);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (2, 24);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 4);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 5);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 6);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 7);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 8);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 9);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 10);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 11);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 12);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 13);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 14);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 15);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 16);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 17);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 18);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 20);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 22);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 24);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 25);
INSERT INTO presus.rol_permisos (rol_id, permiso_id) VALUES (3, 27);


--
-- Data for Name: roles_jurado; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.roles_jurado (id, codigo, nombre) VALUES (1, 'PRESIDENTE', 'Presidente');
INSERT INTO presus.roles_jurado (id, codigo, nombre) VALUES (2, 'VOCAL_1', 'Vocal 1');
INSERT INTO presus.roles_jurado (id, codigo, nombre) VALUES (3, 'VOCAL_2', 'Vocal 2');


--
-- Data for Name: sala; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (30, true, 1, 'SALA-01', 'Sala de Sustentación 1');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (40, true, 2, 'SALA-02', 'Sala de Sustentación 2');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (20, true, 3, 'SALA-03', 'Sala de Sustentación 3');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (30, true, 4, 'SALA-04', 'Sala de Sustentación 4');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (40, true, 5, 'SALA-05', 'Sala de Sustentación 5');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (20, true, 6, 'SALA-06', 'Sala de Sustentación 6');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (30, true, 7, 'SALA-07', 'Sala de Sustentación 7');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (40, true, 8, 'SALA-08', 'Sala de Sustentación 8');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (20, true, 9, 'SALA-09', 'Sala de Sustentación 9');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (30, true, 10, 'SALA-10', 'Sala de Sustentación 10');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (40, true, 11, 'SALA-11', 'Sala de Sustentación 11');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (20, true, 12, 'SALA-12', 'Sala de Sustentación 12');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (30, true, 13, 'SALA-13', 'Sala de Sustentación 13');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (40, true, 14, 'SALA-14', 'Sala de Sustentación 14');
INSERT INTO presus.sala (capacidad, disponible, id, codigo, nombre) VALUES (20, true, 15, 'SALA-15', 'Sala de Sustentación 15');


--
-- Data for Name: temas_propuestos; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (1, 'Plataforma de automatización de pruebas de regresión para APIs REST institucionales', 'Las pruebas de regresión de los servicios REST de la universidad se ejecutan manualmente, lo que retrasa cada despliegue y deja defectos sin detectar.', 'Desarrollar una plataforma que orqueste y ejecute automáticamente las pruebas de regresión de las APIs REST institucionales e informe los resultados.', '1) Modelar los casos de prueba de forma declarativa. 2) Implementar el motor de ejecución programada. 3) Integrar reportes y alertas con el pipeline de CI.', 'Reduce el tiempo de validación previo a cada despliegue y aumenta la cobertura de regresión sin costo humano recurrente.', 'Equipo de desarrollo y aseguramiento de calidad de la Dirección de TIC.', 'INTERMEDIO', 1, 1, 2);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (2, 'Detección temprana de deuda técnica mediante análisis estático en repositorios académicos', 'Los proyectos de titulación acumulan deuda técnica que nadie mide, y llega a producción sin control.', 'Construir una herramienta que analice estáticamente los repositorios y estime la deuda técnica con métricas comparables entre proyectos.', '1) Seleccionar métricas de mantenibilidad. 2) Integrar analizadores estáticos. 3) Generar un tablero histórico por repositorio.', 'Permite a los tutores dar retroalimentación objetiva sobre la calidad del código de los estudiantes.', 'Tutores y estudiantes de la carrera de Software.', 'INTERMEDIO', 1, 1, 1);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (3, 'Sistema de recomendación de tutores de titulación según afinidad temática', 'La asignación de tutores se hace manualmente y a menudo no coincide con la especialidad del docente ni el interés del estudiante.', 'Implementar un sistema de recomendación que sugiera tutores a cada estudiante según la afinidad entre el tema propuesto y la producción académica del docente.', '1) Construir el perfil temático de cada docente. 2) Vectorizar las propuestas de tema. 3) Calcular y explicar el ranking de afinidad.', 'Mejora la calidad del acompañamiento y reduce los cambios de tutor a mitad del proceso.', 'Coordinación de titulación, docentes y estudiantes.', 'AVANZADO', 1, 2, 4);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (4, 'Asistente conversacional para consultas sobre el reglamento de titulación', 'Los estudiantes hacen repetidamente las mismas preguntas sobre plazos y requisitos de titulación a la coordinación.', 'Desarrollar un asistente conversacional que responda consultas sobre el reglamento y el proceso de titulación con citas al documento oficial.', '1) Estructurar la base de conocimiento del reglamento. 2) Implementar la recuperación de pasajes relevantes. 3) Evaluar la exactitud de las respuestas.', 'Descarga a la coordinación de consultas rutinarias y da respuestas disponibles 24/7.', 'Estudiantes en proceso de titulación y personal de coordinación.', 'INTERMEDIO', 1, 2, 3);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (5, 'Aplicación web progresiva para el seguimiento de avances de tutoría sin conexión', 'Los estudiantes en prácticas rurales pierden acceso a la plataforma de tutorías cuando no tienen internet estable.', 'Construir una PWA que permita registrar y consultar avances de tutoría offline y sincronizar al recuperar conexión.', '1) Diseñar el modelo de datos local. 2) Implementar la sincronización con resolución de conflictos. 3) Validar en condiciones de conectividad intermitente.', 'Garantiza continuidad del acompañamiento para estudiantes en zonas con mala conectividad.', 'Estudiantes en prácticas preprofesionales fuera del campus.', 'INTERMEDIO', 1, 3, 6);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (6, 'Aplicación móvil multiplataforma para reservar salas de pre-sustentación', 'La reserva de salas para pre-sustentaciones se coordina por mensajería informal y genera choques de horario.', 'Desarrollar una aplicación móvil multiplataforma para consultar disponibilidad y reservar salas de pre-sustentación en tiempo real.', '1) Exponer la disponibilidad de salas como servicio. 2) Implementar la reserva con bloqueo optimista. 3) Enviar recordatorios push.', 'Elimina los conflictos de agenda y formaliza el uso de los espacios físicos.', 'Coordinación académica, tribunales y estudiantes.', 'BASICO', 1, 3, 5);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (7, 'Análisis automatizado de vulnerabilidades en los despliegues del sistema de titulación', 'No existe un proceso periódico que revise vulnerabilidades conocidas en las dependencias y la configuración del sistema.', 'Implementar un proceso automatizado que detecte y priorice vulnerabilidades en las dependencias y la configuración de los despliegues.', '1) Integrar el escaneo de dependencias en el pipeline. 2) Correlacionar hallazgos con severidad y explotabilidad. 3) Generar reportes accionables.', 'Reduce la superficie de ataque del sistema que custodia datos académicos sensibles.', 'Dirección de TIC y auditoría interna.', 'AVANZADO', 1, 4, 8);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (8, 'Cifrado de extremo a extremo para los documentos de anteproyecto almacenados', 'Los PDF de anteproyecto se guardan sin cifrar, expuestos ante cualquier acceso indebido al almacenamiento.', 'Diseñar e implementar un esquema de cifrado de extremo a extremo para los documentos de anteproyecto y sus correcciones.', '1) Definir el modelo de claves por usuario. 2) Implementar cifrado en cliente antes de subir. 3) Medir el impacto en el rendimiento.', 'Protege la propiedad intelectual de los estudiantes y cumple buenas prácticas de protección de datos.', 'Estudiantes, tutores y la institución.', 'AVANZADO', 1, 4, 7);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (9, 'Tablero de indicadores del proceso de pre-sustentación para coordinación', 'La coordinación no tiene una vista consolidada del embudo de pre-sustentaciones y detecta los cuellos de botella tarde.', 'Construir un tablero de indicadores que muestre en tiempo real el estado del proceso de pre-sustentación y sus tiempos por etapa.', '1) Definir los indicadores clave del proceso. 2) Construir las consultas agregadas. 3) Diseñar la visualización y las alertas por umbral.', 'Permite decisiones basadas en datos y anticipar retrasos antes de que afecten a los estudiantes.', 'Coordinación de titulación y autoridades de carrera.', 'INTERMEDIO', 1, 1, 1);
INSERT INTO presus.temas_propuestos (id, titulo, problema, objetivo_general, objetivos_especificos, justificacion, beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id) VALUES (10, 'Generación asistida de actas de pre-sustentación a partir de las evaluaciones de rúbrica', 'La redacción de actas es manual, repetitiva y propensa a inconsistencias con las notas registradas.', 'Automatizar la generación del borrador del acta de pre-sustentación a partir de las evaluaciones de rúbrica ya registradas.', '1) Modelar la plantilla del acta. 2) Poblarla con los datos de evaluación validados. 3) Permitir revisión y firma digital.', 'Ahorra tiempo al tribunal y elimina errores de transcripción entre la rúbrica y el acta.', 'Miembros de tribunal y secretaría académica.', 'BASICO', 1, 3, 6);


--
-- Data for Name: tipos_evaluador; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.tipos_evaluador (id, codigo, nombre) VALUES (1, 'JURADO', 'Jurado');
INSERT INTO presus.tipos_evaluador (id, codigo, nombre) VALUES (2, 'INSTRUCTOR', 'Instructor/Tutor');


--
-- Data for Name: tipos_mensaje; Type: TABLE DATA; Schema: presus; Owner: -
--

INSERT INTO presus.tipos_mensaje (id, codigo, nombre) VALUES (1, 'TEXTO', 'Texto');
INSERT INTO presus.tipos_mensaje (id, codigo, nombre) VALUES (2, 'ARCHIVO', 'Archivo');
INSERT INTO presus.tipos_mensaje (id, codigo, nombre) VALUES (3, 'SISTEMA', 'Sistema');


--
-- Name: areas_tematicas_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.areas_tematicas_id_seq', 8, true);


--
-- Name: bloques_horarios_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.bloques_horarios_id_seq', 12, true);


--
-- Name: carreras_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.carreras_id_seq', 4, true);


--
-- Name: convocatorias_titulacion_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.convocatorias_titulacion_id_seq', 12, true);


--
-- Name: criterios_rubrica_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.criterios_rubrica_id_seq', 120, true);


--
-- Name: estados_cronograma_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.estados_cronograma_seq', 4, false);


--
-- Name: estados_proceso_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.estados_proceso_seq', 6, false);


--
-- Name: estados_solicitud_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.estados_solicitud_seq', 10, false);


--
-- Name: facultades_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.facultades_id_seq', 3, true);


--
-- Name: jornadas_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.jornadas_seq', 4, false);


--
-- Name: lineas_investigacion_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.lineas_investigacion_id_seq', 4, true);


--
-- Name: periodos_academicos_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.periodos_academicos_id_seq', 7, true);


--
-- Name: recursos_titulacion_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.recursos_titulacion_id_seq', 7, true);


--
-- Name: resultados_evaluacion_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.resultados_evaluacion_seq', 3, false);


--
-- Name: roles_jurado_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.roles_jurado_seq', 4, false);


--
-- Name: rubricas_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.rubricas_id_seq', 20, true);


--
-- Name: sala_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.sala_id_seq', 15, true);


--
-- Name: temas_propuestos_id_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.temas_propuestos_id_seq', 10, true);


--
-- Name: tipos_evaluador_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.tipos_evaluador_seq', 3, false);


--
-- Name: tipos_mensaje_seq; Type: SEQUENCE SET; Schema: presus; Owner: -
--

SELECT pg_catalog.setval('presus.tipos_mensaje_seq', 4, false);

SET session_replication_role = DEFAULT;

-- =============================================================================
--  Fin de esquema.sql — la base queda lista para usar.
--  Para poblarla con ~1 000 000 de filas de prueba:
--    psql -d BdPresustentaciones -f database/datos_masivos.sql
-- =============================================================================


