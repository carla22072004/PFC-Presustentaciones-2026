-- =============================================================================
--  datos_masivos.sql  —  Sistema de Gestión de Pre-Sustentaciones (UTEQ)
-- =============================================================================
--  Genera ~1 000 000 de filas repartidas por las tablas transaccionales, con
--  datos coherentes y respetando las claves foráneas y las restricciones UNIQUE.
--
--  Requisito: haber cargado antes database/esquema.sql (tablas + catálogos).
--
--  Uso:
--    psql -d BdPresustentaciones -f database/esquema.sql
--    psql -d BdPresustentaciones -f database/datos_masivos.sql
--
--  Volumen que produce con la CONFIG por defecto: ~1 025 000 filas (mismo orden
--  de magnitud que la base real, 1 027 572). Para cambiarlo, ajusta las
--  constantes de la sección CONFIG más abajo.
--
--    tutoria_mensajes .......  178 200      evaluaciones ............  15 000
--    notificaciones .........   95 000      evaluaciones_finales ....  15 000
--    evaluaciones_criterio ..   90 000      actas ...................  12 000
--    disponibilidad_sala ....   90 000      historial_estados_acta ..  12 000
--    tutoria_fases ..........   60 000      auditoria ...............  12 000
--    historial_estados_solic.  55 000      docente .................  10 000
--    usuarios ...............   50 004      historial_cronograma ....   4 000
--    miembros_tribunal ......   45 000      ----------------------------------
--    solicitud ..............   45 000      TOTAL ..............  ~1 025 000
--    evaluaciones_jurado ....   45 000
--    estudiante / progreso ..   40 000 c/u
--    tutores / evaluadores ..   30 000 c/u
--    anteproyectos ..........   28 000
--    cronograma .............   24 000
--
--  Contraseña de todos los usuarios generados: "docente123"
--  (bcrypt $2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6).
--  Cuentas de demostración: admin@uteq.edu.ec / demo@uteq.edu.ec /
--  docente@uteq.edu.ec / estudiante@uteq.edu.ec  (misma contraseña).
--
--  PostgreSQL 15+
-- =============================================================================

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- Carga masiva: se desactivan disparadores y validación de FK durante la sesión
-- (mismo enfoque que pg_dump --disable-triggers). Se insertan las filas en orden
-- de dependencia, así que la integridad se mantiene igual.
SET session_replication_role = replica;
SET LOCAL synchronous_commit TO off;

-- =========================== CONFIG (editable) ================================
DROP TABLE IF EXISTS _cfg;
CREATE TEMP TABLE _cfg AS SELECT
    10000  AS n_docentes,
    40000  AS n_estudiantes,
    45000  AS n_solicitudes,
    15000  AS n_calificadas,     -- solicitudes que llegan a evaluación/acta
    30000  AS n_tutorias,        -- solicitudes con tutor asignado
    24000  AS n_cronograma,
    28000  AS n_anteproyectos;

-- Diccionarios para nombres/temas realistas
DROP TABLE IF EXISTS _nom;  DROP TABLE IF EXISTS _ape;  DROP TABLE IF EXISTS _tema;
CREATE TEMP TABLE _nom (i int, v text);
INSERT INTO _nom VALUES
 (0,'Juan'),(1,'María'),(2,'Carlos'),(3,'Ana'),(4,'Luis'),(5,'Sofía'),(6,'Diego'),(7,'Valeria'),
 (8,'Andrés'),(9,'Camila'),(10,'Jorge'),(11,'Daniela'),(12,'Pedro'),(13,'Gabriela'),(14,'Miguel'),
 (15,'Paola'),(16,'Fernando'),(17,'Lucía'),(18,'Ricardo'),(19,'Mariana'),(20,'Sebastián'),(21,'Elena'),
 (22,'Cristian'),(23,'Verónica'),(24,'Bryan'),(25,'Karen'),(26,'David'),(27,'Alejandra'),(28,'Kevin'),
 (29,'Génesis'),(30,'Iván'),(31,'Melissa');
CREATE TEMP TABLE _ape (i int, v text);
INSERT INTO _ape VALUES
 (0,'García'),(1,'Rodríguez'),(2,'Morales'),(3,'Castro'),(4,'Vera'),(5,'Zambrano'),(6,'Cedeño'),
 (7,'Mendoza'),(8,'Bravo'),(9,'Anchundia'),(10,'Loor'),(11,'Macías'),(12,'Palma'),(13,'Vélez'),
 (14,'Intriago'),(15,'Delgado'),(16,'Cevallos'),(17,'Alcívar'),(18,'Chávez'),(19,'Pincay'),
 (20,'Solórzano'),(21,'Ponce'),(22,'Moreira'),(23,'Quijije'),(24,'Menéndez'),(25,'Barreto'),
 (26,'Sánchez'),(27,'Ramírez'),(28,'Cañarte'),(29,'Bailón'),(30,'Andrade'),(31,'Mero');
CREATE TEMP TABLE _tema (i int, v text);
INSERT INTO _tema VALUES
 (0,'Sistema web para la gestión de'),(1,'Aplicación móvil para el control de'),
 (2,'Plataforma de análisis de datos de'),(3,'Automatización del proceso de'),
 (4,'Modelo predictivo para'),(5,'API REST para la integración de'),
 (6,'Panel de indicadores para el seguimiento de'),(7,'Chatbot de asistencia para'),
 (8,'Optimización de consultas en el módulo de'),(9,'Sistema de recomendación para');
DROP TABLE IF EXISTS _tema2;
CREATE TEMP TABLE _tema2 (i int, v text);
INSERT INTO _tema2 VALUES
 (0,'titulación universitaria'),(1,'inventario de laboratorio'),(2,'asistencia docente'),
 (3,'historias clínicas'),(4,'reservas de aulas'),(5,'trámites académicos'),
 (6,'evaluación por competencias'),(7,'seguimiento a graduados'),(8,'biblioteca digital'),
 (9,'gestión de proyectos de vinculación');

-- Helpers de selección aleatoria
--   pick(tabla, semilla) -> valor de texto del diccionario
CREATE OR REPLACE FUNCTION _pick(p_tab text, p_seed bigint) RETURNS text AS $f$
DECLARE r text; c int;
BEGIN
  EXECUTE format('SELECT count(*) FROM %I', p_tab) INTO c;
  EXECUTE format('SELECT v FROM %I WHERE i = %s', p_tab, (p_seed % c)) INTO r;
  RETURN r;
END $f$ LANGUAGE plpgsql;

-- fecha pseudo-aleatoria en los últimos ~2 años, determinista por la semilla
CREATE OR REPLACE FUNCTION _fecha(p_seed bigint) RETURNS timestamp AS $f$
  SELECT (now() - ((p_seed % 700) || ' days')::interval - ((p_seed % 86400) || ' seconds')::interval)::timestamp;
$f$ LANGUAGE sql;


-- ============================ 1. USUARIOS =====================================
-- Cuentas de demostración (rol_id: 1 ADMIN, 2 DOCENTE, 3 COORDINADOR, 4 ESTUDIANTE)
INSERT INTO presus.usuarios (id, nombre, apellido, email, password, rol, rol_id, activo, creado_en) VALUES
 (1,'Admin','Sistema','admin@uteq.edu.ec','$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6','ADMIN',1,true,now()),
 (2,'Usuario','Demostración','demo@uteq.edu.ec','$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6','COORDINADOR',3,true,now()),
 (3,'Docente','Demo','docente@uteq.edu.ec','$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6','DOCENTE',2,true,now()),
 (4,'Estudiante','Demo','estudiante@uteq.edu.ec','$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6','ESTUDIANTE',4,true,now());

-- Docentes (ids 5 .. 4+n_docentes)
INSERT INTO presus.usuarios (id, nombre, apellido, email, password, rol, rol_id, activo, creado_en)
SELECT g + 4,
       _pick('_nom', g), _pick('_ape', g + 7),
       'docente' || (g + 4) || '@uteq.edu.ec',
       '$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6',
       'DOCENTE', 2, (g % 50 <> 0), _fecha(g * 13)
FROM generate_series(1, (SELECT n_docentes FROM _cfg)) g;

-- Estudiantes (ids arrancan tras los docentes)
INSERT INTO presus.usuarios (id, nombre, apellido, email, password, rol, rol_id, activo, creado_en)
SELECT g + 4 + (SELECT n_docentes FROM _cfg),
       _pick('_nom', g * 3), _pick('_ape', g),
       'est' || (g + 4 + (SELECT n_docentes FROM _cfg)) || '@uteq.edu.ec',
       '$2y$10$waNUIfYV3dQRO9lVxsgHmuImww0Ia/twTkbcdLOZgyZclO8BqWjN6',
       'ESTUDIANTE', 4, (g % 40 <> 0), _fecha(g * 7)
FROM generate_series(1, (SELECT n_estudiantes FROM _cfg)) g;


-- ============================ 2. DOCENTE =====================================
INSERT INTO presus.docente (id, usuario_id, area_especialidad, facultad_id, carga_horaria_semanal, disponible, creado_en)
SELECT g, g + 4,
       (ARRAY['Ingeniería de Software','Bases de Datos','Redes','Inteligencia Artificial','Desarrollo Web','Ciberseguridad'])[1 + (g % 6)],
       1, (g % 20), (g % 7 <> 0), _fecha(g * 13)
FROM generate_series(1, (SELECT n_docentes FROM _cfg)) g;


-- ============================ 3. ESTUDIANTE ==================================
INSERT INTO presus.estudiante (id, usuario_id, carrera_id, carrera, semestre_actual, semestre,
                               periodo_ingreso_id, estado_academico_id, expediente_codigo, creado_en)
SELECT g,
       g + 4 + (SELECT n_docentes FROM _cfg),
       CASE WHEN g % 8 = 0 THEN 4 ELSE 1 END,
       CASE WHEN g % 8 = 0 THEN 'Educacion Basica' ELSE 'Ingeniería en Software' END,
       8 + (g % 3),
       'Semestre ' || (8 + (g % 3)),
       1 + (g % 6),
       1 + (g % 3),
       'EXP-' || lpad(g::text, 7, '0'),
       _fecha(g * 7)
FROM generate_series(1, (SELECT n_estudiantes FROM _cfg)) g;


-- ============================ 4. SOLICITUD ===================================
-- Reparte las solicitudes entre los estudiantes (algunos tienen 2). Estado según
-- el id: las primeras n_calificadas quedan CALIFICADA/COMPLETADA, el resto en
-- fases intermedias.
INSERT INTO presus.solicitud (id, estudiante_id, titulo_tema, estado_id, estado,
                              convocatoria_id, modalidad_titulacion_id,
                              linea_investigacion_id, area_tematica_id,
                              fecha_registro, actualizado_en, creado_por)
SELECT g,
       1 + ((g - 1) % (SELECT n_estudiantes FROM _cfg)),
       _pick('_tema', g) || ' ' || _pick('_tema2', g * 7),
       CASE
         WHEN g <= (SELECT n_calificadas FROM _cfg)          THEN (ARRAY[5,7])[1 + (g % 2)]
         WHEN g <= (SELECT n_calificadas FROM _cfg) + 6000   THEN 4
         WHEN g <= (SELECT n_tutorias    FROM _cfg)          THEN 3
         WHEN g % 11 = 0                                     THEN 8   -- RECHAZADA
         ELSE 2                                                        -- ENVIADA
       END,
       CASE
         WHEN g <= (SELECT n_calificadas FROM _cfg)          THEN (ARRAY['CALIFICADA','COMPLETADA'])[1 + (g % 2)]
         WHEN g <= (SELECT n_calificadas FROM _cfg) + 6000   THEN 'EVALUACION'
         WHEN g <= (SELECT n_tutorias    FROM _cfg)          THEN 'TUTORIA'
         WHEN g % 11 = 0                                     THEN 'RECHAZADA'
         ELSE 'ENVIADA'
       END,
       1 + (g % 12), 1 + (g % 3), 1 + (g % 4), 1 + (g % 8),
       _fecha(g * 5), _fecha(g * 5 - 100), 1
FROM generate_series(1, (SELECT n_solicitudes FROM _cfg)) g;


-- ==================== 5. HISTORIAL DE ESTADOS DE SOLICITUD ===================
-- 1 fila "creada" por solicitud + una transición extra en ~1 de cada 4.
INSERT INTO presus.historial_estados_solicitud (id, solicitud_id, estado_anterior_id, estado_nuevo_id, usuario_id, fecha_cambio, comentario)
SELECT g, g, NULL, 1, 1, _fecha(g * 5), 'Solicitud registrada'
FROM generate_series(1, (SELECT n_solicitudes FROM _cfg)) g
UNION ALL
SELECT (SELECT n_solicitudes FROM _cfg) + g, g, 1, 2, 2, _fecha(g * 5 - 50), 'Enviada a revisión'
FROM generate_series(1, 10000) g;


-- ============================ 6. ANTEPROYECTOS ===============================
INSERT INTO presus.anteproyectos (id, solicitud_id, estado_id, estado, archivo_pdf, tamano_bytes, sha256_hash, fecha_envio)
SELECT g, g, 1 + (g % 5),
       (ARRAY['PENDIENTE','EN_PROCESO','APROBADO','OBSERVADO','RECHAZADO'])[1 + (g % 5)],
       'anteproyecto_' || g || '.pdf',
       200000 + (g % 800000),
       md5(random()::text || g),
       (_fecha(g * 5))::date
FROM generate_series(1, (SELECT n_anteproyectos FROM _cfg)) g;


-- ============================ 7. TUTORÍAS ====================================
INSERT INTO presus.tutores (id, solicitud_id, docente_id, estado, estado_id, fecha_asignacion)
SELECT g, g, 1 + (g % (SELECT n_docentes FROM _cfg)),
       CASE WHEN g <= (SELECT n_calificadas FROM _cfg) THEN 'COMPLETADA' ELSE 'ACTIVO' END,
       CASE WHEN g <= (SELECT n_calificadas FROM _cfg) THEN 3 ELSE 1 END,
       _fecha(g * 5 - 30)
FROM generate_series(1, (SELECT n_tutorias FROM _cfg)) g;

-- Fases: entre 1 y 3 por tutoría (tipo_mensaje/estado según avance)
INSERT INTO presus.tutoria_fases (id, tutor_id, numero_fase, estado, estado_id, fecha_inicio, fecha_aprobacion, archivo_pdf_estudiante, tamano_pdf_bytes, sha256_pdf)
SELECT row_number() OVER (),
       t.g,
       f.n,
       CASE WHEN f.n <= (1 + (t.g % 3)) - 1 THEN 'APROBADA' ELSE 'EN_PROCESO' END,
       CASE WHEN f.n <= (1 + (t.g % 3)) - 1 THEN 3 ELSE 2 END,
       _fecha(t.g * 5 - 20 + f.n),
       CASE WHEN f.n <= (1 + (t.g % 3)) - 1 THEN _fecha(t.g * 5 - 15 + f.n) ELSE NULL END,
       'fase_' || t.g || '_' || f.n || '.pdf',
       150000 + ((t.g + f.n) % 500000),
       md5(random()::text || t.g || f.n)
FROM (SELECT generate_series(1, (SELECT n_tutorias FROM _cfg)) g) t
CROSS JOIN LATERAL (SELECT generate_series(1, 1 + (t.g % 3)) n) f;

-- Mensajes: ~3 por fase, alternando remitente estudiante/docente
INSERT INTO presus.tutoria_mensajes (id, fase_id, remitente_id, tipo, tipo_mensaje_id, contenido, leido, fecha_envio)
SELECT row_number() OVER (),
       fase.id,
       CASE WHEN m.k % 2 = 0
            THEN 5 + (fase.id % (SELECT n_docentes FROM _cfg))                         -- docente
            ELSE 5 + (SELECT n_docentes FROM _cfg) + (fase.id % (SELECT n_estudiantes FROM _cfg)) -- estudiante
       END,
       (ARRAY['TEXTO','ARCHIVO','SISTEMA'])[1 + (m.k % 3)],
       1 + (m.k % 3),
       (ARRAY['Adjunto los avances del capítulo.','Revisado, aplicar las correcciones marcadas.',
              'Reunión de tutoría agendada.','Falta la bibliografía en formato APA.',
              'Aprobada la fase, continuar con la siguiente.'])[1 + ((fase.id + m.k) % 5)],
       (m.k % 4 <> 0),
       _fecha(fase.id * 3 + m.k)
FROM presus.tutoria_fases fase
CROSS JOIN LATERAL (SELECT generate_series(1, 3) k) m
WHERE fase.id % 100 <> 0;   -- deja algunas fases sin mensajes


-- ============================ 8. CRONOGRAMA ==================================
INSERT INTO presus.cronograma (id, solicitud_id, sala_id, bloque_id, convocatoria_id,
                               estado_id, estado, numero_intento, duracion_min, fecha_inicio, creado_en)
SELECT g, g,
       1 + (g % 15), 1 + (g % 12), 1 + (g % 12),
       CASE WHEN g <= (SELECT n_calificadas FROM _cfg) THEN 2 ELSE 1 END,
       CASE WHEN g <= (SELECT n_calificadas FROM _cfg) THEN 'REALIZADO' ELSE 'PROGRAMADO' END,
       1, 40, _fecha(g * 5 - 5), _fecha(g * 5 - 10)
FROM generate_series(1, (SELECT n_cronograma FROM _cfg)) g;

INSERT INTO presus.historial_cronograma (id, cronograma_id, usuario_id, fecha_anterior, fecha_nueva, sala_anterior_id, sala_nueva_id, fecha_cambio, motivo)
SELECT g, g, 2, _fecha(g * 5 - 8), _fecha(g * 5 - 5), 1 + (g % 15), 1 + ((g + 1) % 15), _fecha(g * 5 - 6),
       'Reprogramación por disponibilidad de sala'
FROM generate_series(1, 4000) g;


-- ==================== 9. TRIBUNAL Y EVALUADORES ==============================
-- 3 miembros de tribunal por solicitud calificada (rol_jurado 1=PRESIDENTE, 2=VOCAL_1, 3=VOCAL_2)
INSERT INTO presus.miembros_tribunal (id, solicitud_id, docente_id, rol_jurado_id, confirmado, asignado_en)
SELECT (s.g - 1) * 3 + j.r,
       s.g,
       1 + (((s.g * 3) + j.r) % (SELECT n_docentes FROM _cfg)),
       j.r,
       true,
       _fecha(s.g * 5 - 12)
FROM (SELECT generate_series(1, (SELECT n_calificadas FROM _cfg)) g) s
CROSS JOIN (SELECT generate_series(1, 3) r) j;

-- 2 evaluadores por solicitud calificada: 1 JURADO (tipo 1) + 1 INSTRUCTOR (tipo 2)
INSERT INTO presus.evaluadores (id, solicitud_id, docente_id, miembro_tribunal_id, tipo_evaluador_id, peso, fecha_asignacion)
SELECT (s.g - 1) * 2 + e.t,
       s.g,
       1 + (((s.g * 2) + e.t) % (SELECT n_docentes FROM _cfg)),
       CASE WHEN e.t = 1 THEN (s.g - 1) * 3 + 1 ELSE NULL END,
       e.t,
       CASE WHEN e.t = 1 THEN 0.6 ELSE 0.4 END,
       _fecha(s.g * 5 - 11)
FROM (SELECT generate_series(1, (SELECT n_calificadas FROM _cfg)) g) s
CROSS JOIN (SELECT generate_series(1, 2) t) e;


-- ==================== 10. EVALUACIONES =======================================
-- Una evaluación consolidada por solicitud calificada
INSERT INTO presus.evaluaciones (id, solicitud_id, rubrica_id, nota_instructor, nota_jurado, nota_final,
                                 peso_instructor, peso_jurado, resultado)
SELECT g, g, 1 + (g % 20),
       6.0 + (g % 40) / 10.0,
       6.5 + (g % 35) / 10.0,
       7.0 + (g % 30) / 10.0,
       0.4, 0.6,
       CASE WHEN (g % 10) = 0 THEN 'REPROBADO' ELSE 'APROBADO' END
FROM generate_series(1, (SELECT n_calificadas FROM _cfg)) g;

-- 3 notas de jurado por solicitud (una por miembro del tribunal)
INSERT INTO presus.evaluaciones_jurado (id, solicitud_id, jurado_id, nota_jurado, resultado, fecha_registro)
SELECT (s.g - 1) * 3 + j.r,
       s.g,
       (s.g - 1) * 3 + j.r,
       6.0 + ((s.g + j.r) % 40) / 10.0,
       CASE WHEN ((s.g + j.r) % 10) = 0 THEN 'REPROBADO' ELSE 'APROBADO' END,
       _fecha(s.g * 5 - 3)
FROM (SELECT generate_series(1, (SELECT n_calificadas FROM _cfg)) g) s
CROSS JOIN (SELECT generate_series(1, 3) r) j;

-- Detalle por criterio: 3 criterios por evaluador (UNIQUE evaluador_id + criterio_id)
INSERT INTO presus.evaluaciones_criterio (id, solicitud_id, evaluador_id, jurado_id, criterio_id,
                                          escala, nota_obtenida, registrado_en)
SELECT row_number() OVER (),
       ev.solicitud_id,
       ev.id,
       (ev.solicitud_id - 1) * 3 + 1,
       c.c,
       4,
       2.0 + ((ev.id + c.c) % 3),
       _fecha(ev.solicitud_id * 5 - 2)
FROM presus.evaluadores ev
CROSS JOIN (SELECT generate_series(1, 3) c) c;

-- Evaluación final (1 por solicitud calificada, resultado_id 1=APROBADO 2=REPROBADO)
INSERT INTO presus.evaluaciones_finales (id, solicitud_id, rubrica_id, nota_instructor, nota_jurado_promedio,
                                         nota_final, peso_instructor, peso_jurado, resultado_id, fecha_calculo)
SELECT g, g, 1 + (g % 20),
       6.0 + (g % 40) / 10.0,
       6.5 + (g % 35) / 10.0,
       7.0 + (g % 30) / 10.0,
       0.4, 0.6,
       CASE WHEN (g % 10) = 0 THEN 2 ELSE 1 END,
       _fecha(g * 5 - 1)
FROM generate_series(1, (SELECT n_calificadas FROM _cfg)) g;


-- ==================== 11. ACTAS =============================================
-- ~80% de las solicitudes calificadas tienen acta (UNIQUE solicitud_id)
INSERT INTO presus.actas (id, solicitud_id, estado_id, fecha_generacion,
                          firmada, firmada_presidente, firmada_vocal1, firmada_vocal2, firmada_tutor,
                          fecha_firma_presidente, fecha_firma_vocal1, fecha_firma_vocal2, fecha_firma_tutor,
                          archivo_pdf)
SELECT g, g,
       CASE WHEN g % 5 = 0 THEN 1 ELSE 4 END,                         -- 1 GENERADA / 4 FINALIZADA
       (_fecha(g * 5))::date,
       (g % 5 <> 0), (g % 5 <> 0), (g % 5 <> 0), (g % 5 <> 0), (g % 5 <> 0),
       _fecha(g * 5 + 1), _fecha(g * 5 + 1), _fecha(g * 5 + 1), _fecha(g * 5 + 1),
       'acta_' || g || '.pdf'
FROM generate_series(1, (SELECT round(n_calificadas * 0.8) FROM _cfg)::int) g;

INSERT INTO presus.historial_estados_acta (id, acta_id, estado_anterior_id, estado_nuevo_id, usuario_id, rol_usuario, accion, fecha_cambio, comentario)
SELECT g, g, NULL, 1, 1, 'COORDINADOR', 'CREAR', _fecha(g * 5), 'Acta generada'
FROM generate_series(1, (SELECT round(n_calificadas * 0.8) FROM _cfg)::int) g;


-- ==================== 12. DISPONIBILIDAD DE SALAS ===========================
-- 15 salas x 12 bloques x ~500 días
INSERT INTO presus.disponibilidad_sala (id, sala_id, bloque_id, fecha, disponible, motivo)
SELECT row_number() OVER (),
       1 + (g % 15),
       1 + (g % 12),
       (CURRENT_DATE - (g % 500))::date,
       (g % 7 <> 0),
       CASE WHEN g % 7 = 0 THEN 'Reservada para pre-sustentación' ELSE NULL END
FROM generate_series(1, 90000) g;


-- ==================== 13. NOTIFICACIONES ====================================
INSERT INTO presus.notificaciones (id, usuario_id, mensaje, leida, fecha)
SELECT g,
       1 + (g % (4 + (SELECT n_docentes + n_estudiantes FROM _cfg))),
       (ARRAY[
         'Tu solicitud de pre-sustentación fue registrada.',
         'El coordinador aprobó tu solicitud.',
         'Se te asignó como jurado de una pre-sustentación.',
         'Nueva fase de tutoría pendiente de revisión.',
         'El acta de tu pre-sustentación está lista para firmar.',
         'Recordatorio: pre-sustentación programada para mañana.'
       ])[1 + (g % 6)],
       (g % 3 <> 0),
       _fecha(g * 2)
FROM generate_series(1, 95000) g;


-- ==================== 14. AUDITORÍA =========================================
INSERT INTO presus.auditoria (id, tabla, registro_id, accion, usuario_id, usuario_nombre, fecha, datos_nuevos)
SELECT g,
       (ARRAY['solicitud','usuarios','actas','evaluaciones_finales','tutores','miembros_tribunal'])[1 + (g % 6)],
       1 + (g % 40000),
       (ARRAY['CREAR','MODIFICAR','ELIMINAR'])[1 + (g % 3)],
       1 + (g % 4),
       (ARRAY['Admin Sistema','Usuario Demostración','Docente Demo'])[1 + (g % 3)],
       _fecha(g * 2),
       jsonb_build_object('cambio', 'registro ' || g, 'ts', _fecha(g * 2))
FROM generate_series(1, 12000) g;


-- ==================== 15. PROGRESO DE TITULACIÓN ============================
INSERT INTO presus.progreso_estudiante (id, estudiante_id, pasos_json)
SELECT g, g,
       jsonb_build_object(
         'tema_elegido',      (g % 2 = 0),
         'anteproyecto',      (g % 3 = 0),
         'tutor_asignado',    (g % 4 = 0),
         'tutorias_completas',(g % 6 = 0),
         'pre_sustentacion',  (g % 9 = 0)
       )
FROM generate_series(1, (SELECT n_estudiantes FROM _cfg)) g;


-- ==================== SECUENCIAS ============================================
-- Deja cada secuencia por encima del último id insertado, para que la app pueda
-- seguir insertando sin colisiones.
SELECT setval('presus.usuarios_id_seq',                     (SELECT max(id) FROM presus.usuarios));
SELECT setval('presus.docente_id_seq',                      (SELECT max(id) FROM presus.docente));
SELECT setval('presus.estudiante_id_seq',                   (SELECT max(id) FROM presus.estudiante));
SELECT setval('presus.solicitud_id_seq',                    (SELECT max(id) FROM presus.solicitud));
SELECT setval('presus.historial_estados_solicitud_id_seq',  (SELECT max(id) FROM presus.historial_estados_solicitud));
SELECT setval('presus.anteproyectos_id_seq',                (SELECT max(id) FROM presus.anteproyectos));
SELECT setval('presus.tutores_id_seq',                      (SELECT max(id) FROM presus.tutores));
SELECT setval('presus.tutoria_fases_id_seq',                (SELECT max(id) FROM presus.tutoria_fases));
SELECT setval('presus.tutoria_mensajes_id_seq',             (SELECT max(id) FROM presus.tutoria_mensajes));
SELECT setval('presus.cronograma_id_seq',                   (SELECT max(id) FROM presus.cronograma));
SELECT setval('presus.historial_cronograma_id_seq',         (SELECT max(id) FROM presus.historial_cronograma));
SELECT setval('presus.miembros_tribunal_id_seq',            (SELECT max(id) FROM presus.miembros_tribunal));
SELECT setval('presus.evaluadores_id_seq',                  (SELECT max(id) FROM presus.evaluadores));
SELECT setval('presus.evaluaciones_id_seq',                 (SELECT max(id) FROM presus.evaluaciones));
SELECT setval('presus.evaluaciones_jurado_id_seq',          (SELECT max(id) FROM presus.evaluaciones_jurado));
SELECT setval('presus.evaluaciones_criterio_id_seq',        (SELECT max(id) FROM presus.evaluaciones_criterio));
SELECT setval('presus.evaluaciones_finales_id_seq',         (SELECT max(id) FROM presus.evaluaciones_finales));
SELECT setval('presus.actas_id_seq',                        (SELECT max(id) FROM presus.actas));
SELECT setval('presus.historial_estados_acta_id_seq',       (SELECT max(id) FROM presus.historial_estados_acta));
SELECT setval('presus.disponibilidad_sala_id_seq',          (SELECT max(id) FROM presus.disponibilidad_sala));
SELECT setval('presus.notificaciones_id_seq',               (SELECT max(id) FROM presus.notificaciones));
SELECT setval('presus.auditoria_id_seq',                    (SELECT max(id) FROM presus.auditoria));
SELECT setval('presus.progreso_estudiante_id_seq',          (SELECT max(id) FROM presus.progreso_estudiante));

-- Limpieza de objetos auxiliares
DROP FUNCTION _pick(text, bigint);
DROP FUNCTION _fecha(bigint);

SET session_replication_role = DEFAULT;
COMMIT;

ANALYZE;

-- ==================== VERIFICACIÓN =========================================
SELECT 'usuarios'      AS tabla, count(*) FROM presus.usuarios
UNION ALL SELECT 'docente',                    count(*) FROM presus.docente
UNION ALL SELECT 'estudiante',                 count(*) FROM presus.estudiante
UNION ALL SELECT 'solicitud',                  count(*) FROM presus.solicitud
UNION ALL SELECT 'historial_estados_solicitud',count(*) FROM presus.historial_estados_solicitud
UNION ALL SELECT 'anteproyectos',              count(*) FROM presus.anteproyectos
UNION ALL SELECT 'tutores',                    count(*) FROM presus.tutores
UNION ALL SELECT 'tutoria_fases',              count(*) FROM presus.tutoria_fases
UNION ALL SELECT 'tutoria_mensajes',           count(*) FROM presus.tutoria_mensajes
UNION ALL SELECT 'cronograma',                 count(*) FROM presus.cronograma
UNION ALL SELECT 'miembros_tribunal',          count(*) FROM presus.miembros_tribunal
UNION ALL SELECT 'evaluadores',                count(*) FROM presus.evaluadores
UNION ALL SELECT 'evaluaciones',               count(*) FROM presus.evaluaciones
UNION ALL SELECT 'evaluaciones_jurado',        count(*) FROM presus.evaluaciones_jurado
UNION ALL SELECT 'evaluaciones_criterio',      count(*) FROM presus.evaluaciones_criterio
UNION ALL SELECT 'evaluaciones_finales',       count(*) FROM presus.evaluaciones_finales
UNION ALL SELECT 'actas',                      count(*) FROM presus.actas
UNION ALL SELECT 'historial_estados_acta',     count(*) FROM presus.historial_estados_acta
UNION ALL SELECT 'disponibilidad_sala',        count(*) FROM presus.disponibilidad_sala
UNION ALL SELECT 'notificaciones',             count(*) FROM presus.notificaciones
UNION ALL SELECT 'auditoria',                  count(*) FROM presus.auditoria
UNION ALL SELECT 'progreso_estudiante',        count(*) FROM presus.progreso_estudiante
UNION ALL SELECT 'historial_cronograma',       count(*) FROM presus.historial_cronograma
ORDER BY 2 DESC;

SELECT 'TOTAL FILAS GENERADAS' AS resumen, sum(c) AS filas FROM (
  SELECT count(*) c FROM presus.usuarios UNION ALL
  SELECT count(*) FROM presus.docente UNION ALL
  SELECT count(*) FROM presus.estudiante UNION ALL
  SELECT count(*) FROM presus.solicitud UNION ALL
  SELECT count(*) FROM presus.historial_estados_solicitud UNION ALL
  SELECT count(*) FROM presus.anteproyectos UNION ALL
  SELECT count(*) FROM presus.tutores UNION ALL
  SELECT count(*) FROM presus.tutoria_fases UNION ALL
  SELECT count(*) FROM presus.tutoria_mensajes UNION ALL
  SELECT count(*) FROM presus.cronograma UNION ALL
  SELECT count(*) FROM presus.historial_cronograma UNION ALL
  SELECT count(*) FROM presus.miembros_tribunal UNION ALL
  SELECT count(*) FROM presus.evaluadores UNION ALL
  SELECT count(*) FROM presus.evaluaciones UNION ALL
  SELECT count(*) FROM presus.evaluaciones_jurado UNION ALL
  SELECT count(*) FROM presus.evaluaciones_criterio UNION ALL
  SELECT count(*) FROM presus.evaluaciones_finales UNION ALL
  SELECT count(*) FROM presus.actas UNION ALL
  SELECT count(*) FROM presus.historial_estados_acta UNION ALL
  SELECT count(*) FROM presus.disponibilidad_sala UNION ALL
  SELECT count(*) FROM presus.notificaciones UNION ALL
  SELECT count(*) FROM presus.auditoria UNION ALL
  SELECT count(*) FROM presus.progreso_estudiante
) t;
