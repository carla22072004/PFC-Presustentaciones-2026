-- =============================================================================
-- generar-volumen-datos.sql
-- Genera un volumen sintético coherente (~1,000,000+ filas) distribuido en todas
-- las tablas del esquema `presus`, respetando FKs y la jerarquía real del dominio:
--   usuarios -> estudiante/docente -> solicitud -> tutores -> anteproyectos ->
--   cronograma -> miembros_tribunal -> evaluadores/evaluaciones* -> actas
--                                    -> tutoria_fases -> tutoria_mensajes
-- Los conteos objetivo replican la distribución documentada en
-- docs/basedatos/VOLUMEN-DATOS.md (con ajustes menores: la tabla "jurados" ahí
-- listada es un remanente de un esquema anterior que ya no existe, y se
-- compensa su volumen en notificaciones).
--
-- Uso: docker exec -i amz-postgres psql -U postgres -d BdPresustentaciones -v ON_ERROR_STOP=1 -f - < scripts/generar-volumen-datos.sql
-- Idempotencia: NO es re-ejecutable sobre una base ya poblada por este script
-- (no usa ON CONFLICT en las tablas de volumen) -- pensado para correr una vez
-- sobre una base recién migrada (solo catálogos base + usuarios demo).
-- =============================================================================

SET statement_timeout = 0;
SET work_mem = '256MB';

DO $$
DECLARE
  nombres TEXT[] := ARRAY['Juan','María','Carlos','Ana','Luis','Sofía','Pedro','Laura','Diego','Valentina',
                           'Miguel','Camila','Andrés','Daniela','José','Gabriela','Fernando','Paula','Ricardo','Isabella',
                           'Jorge','Mariana','Alberto','Carolina','Eduardo','Andrea','Francisco','Patricia','Manuel','Verónica',
                           'Rafael','Cristina','Sergio','Alejandra','Roberto','Natalia','Óscar','Valeria','Iván','Lucía'];
  apellidos TEXT[] := ARRAY['González','Rodríguez','Pérez','López','Martínez','Sánchez','Ramírez','Torres','Flores','Rivera',
                             'Gómez','Díaz','Reyes','Morales','Cruz','Ortiz','Gutiérrez','Chávez','Ramos','Vargas',
                             'Castro','Jiménez','Romero','Álvarez','Mendoza','Ruiz','Herrera','Medina','Aguilar','Vega',
                             'Castillo','Guerrero','Silva','Rojas','Núñez','Delgado','Peña','Cabrera','Salazar','Paredes'];
  temas TEXT[] := ARRAY['Sistema web para','Aplicación móvil de','Plataforma de gestión de','Modelo predictivo para',
                         'Análisis de datos de','Automatización de procesos de','Sistema de monitoreo de',
                         'Herramienta de análisis para','Plataforma inteligente de','Sistema de recomendación para'];
  dominios TEXT[] := ARRAY['control de inventarios','gestión académica','seguridad informática','atención al cliente',
                            'análisis financiero','gestión hospitalaria','control de tráfico','gestión de proyectos',
                            'monitoreo ambiental','comercio electrónico','gestión de recursos humanos','control de calidad',
                            'logística y transporte','gestión documental','redes sociales'];
  areas_doc TEXT[] := ARRAY['Ingeniería de Software','Inteligencia Artificial','Bases de Datos','Redes y Seguridad',
                             'Desarrollo Web','Ciencia de Datos','Sistemas Distribuidos','Gestión de Proyectos TI'];
  mensajes_tutoria TEXT[] := ARRAY['Buenas tardes, adjunto la versión corregida del capítulo.',
                                    'Gracias por la observación, la corregiré esta semana.',
                                    'Quedo atento a su retroalimentación.','Se aprueba la fase, puede continuar con la siguiente.',
                                    'Favor revisar el marco teórico actualizado.','Entendido, procedo con los cambios sugeridos.',
                                    'Adjunto avance de la metodología propuesta.','Excelente trabajo, continúe así.'];
  notif_msgs TEXT[] := ARRAY['Su solicitud ha sido actualizada.','Tiene un nuevo mensaje de tutoría.',
                              'Su cronograma ha sido programado.','Su evaluación ha sido registrada.',
                              'Su anteproyecto fue revisado.','Recordatorio: revise su solicitud pendiente.'];
  crit_nombres TEXT[] := ARRAY['Estructura','Fundamentación teórica','Metodología','Claridad de exposición',
                                'Manejo del tema','Respuesta a preguntas'];
  pw_hash TEXT;
  v_est_ini BIGINT;
  v_doc_ini BIGINT;
BEGIN
  CREATE EXTENSION IF NOT EXISTS pgcrypto;
  -- Una sola hash bcrypt reutilizada en todos los usuarios sintéticos (cuentas de
  -- prueba, no reales) -- generar una hash distinta por fila sería ~50-100ms x 51K
  -- filas (bcrypt es intencionalmente lento) y no aporta nada aquí.
  pw_hash := crypt('Test1234!', gen_salt('bf'));

  -- Las secuencias de identidad NO son transaccionales (un intento fallido previo
  -- deja huecos aunque el INSERT se revierta) -- este script asume que cada tabla
  -- de volumen empieza vacía y que sus ids son exactamente 1..N en el orden de
  -- inserción, así que se reinician aquí para que el script sea repetible tras un
  -- fallo a mitad de camino. NO tocar usuarios_id_seq (ya hay 2 filas reales).
  ALTER SEQUENCE presus.periodos_academicos_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.convocatorias_titulacion_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.bloques_horarios_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.sala_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.rubricas_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.criterios_rubrica_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.estudiante_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.docente_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.solicitud_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.tutores_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.anteproyectos_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.cronograma_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.miembros_tribunal_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.evaluadores_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.evaluaciones_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.evaluaciones_finales_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.evaluaciones_jurado_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.evaluaciones_criterio_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.actas_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.tutoria_fases_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.tutoria_mensajes_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.historial_estados_solicitud_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.historial_cronograma_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.notificaciones_id_seq RESTART WITH 1;
  ALTER SEQUENCE presus.disponibilidad_sala_id_seq RESTART WITH 1;

  -- ============================================================================
  -- 1. CATÁLOGOS que quedaron vacíos (no los siembra ninguna migración ni el
  --    CommandLineRunner -- ver docs/observaciones sobre el gap). Los códigos
  --    coinciden exactamente con los que el código Java busca por findByCodigo()
  --    (SolicitudServiceImpl, JuradoServiceImpl, EvaluacionServiceImpl,
  --    CronogramaServiceImpl, ActaServiceImpl, RubricaEvaluacionServiceImpl).
  -- ============================================================================
  INSERT INTO presus.estados_solicitud (id, orden, codigo, nombre) VALUES
    (1,1,'CREADA','Creada'),(2,2,'ENVIADA','Enviada'),(3,3,'TUTORIA','En tutoría'),
    (4,4,'EVALUACION','En evaluación'),(5,5,'CALIFICADA','Calificada'),(6,6,'APROBADA','Aprobada'),
    (7,7,'COMPLETADA','Completada'),(8,8,'RECHAZADA','Rechazada'),(9,9,'SUSPENDIDA','Suspendida')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.estados_cronograma (id, codigo, nombre) VALUES
    (1,'PROGRAMADO','Programado'),(2,'REALIZADO','Realizado'),(3,'CANCELADO','Cancelado')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.tipos_evaluador (id, codigo, nombre) VALUES
    (1,'JURADO','Jurado'),(2,'INSTRUCTOR','Instructor/Tutor')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.roles_jurado (id, codigo, nombre) VALUES
    (1,'PRESIDENTE','Presidente'),(2,'VOCAL_1','Vocal 1'),(3,'VOCAL_2','Vocal 2')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.resultados_evaluacion (id, codigo, nombre) VALUES
    (1,'APROBADO','Aprobado'),(2,'REPROBADO','Reprobado')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.jornadas (id, codigo, nombre) VALUES
    (1,'MATUTINA','Matutina'),(2,'VESPERTINA','Vespertina'),(3,'NOCTURNA','Nocturna')
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO presus.periodos_academicos (codigo, nombre, fecha_inicio, fecha_fin, activo)
  SELECT 'PA-' || (2023+((g-1)/2)) || '-' || (((g-1)%2)+1),
         'Periodo Académico ' || (2023+((g-1)/2)) || '-' || (((g-1)%2)+1),
         make_date(2023+((g-1)/2), CASE WHEN (g-1)%2=0 THEN 3 ELSE 9 END, 1),
         make_date(2023+((g-1)/2), CASE WHEN (g-1)%2=0 THEN 8 ELSE 2 END, 28),
         g=6
  FROM generate_series(1,6) g;

  INSERT INTO presus.convocatorias_titulacion (periodo_academico_id, codigo, nombre, fecha_inicio, fecha_fin, activa)
  SELECT ((g-1)/2)+1, 'CONV-' || g, 'Convocatoria de Titulación ' || g,
         (make_date(2023,1,1) + ((g-1)*60)), (make_date(2023,1,1) + ((g-1)*60) + 45), g=12
  FROM generate_series(1,12) g;

  INSERT INTO presus.bloques_horarios (jornada_id, nombre, hora_inicio, hora_fin)
  SELECT ((g-1)/4)+1, 'Bloque ' || (((g-1)%4)+1),
         ('07:00:00'::time + (((g-1)%4) * interval '2 hours'))::time,
         ('09:00:00'::time + (((g-1)%4) * interval '2 hours'))::time
  FROM generate_series(1,12) g;

  INSERT INTO presus.sala (codigo, nombre, capacidad, disponible)
  SELECT 'SALA-' || lpad(g::text,2,'0'), 'Sala de Sustentación ' || g, 20 + (g%3)*10, true
  FROM generate_series(1,15) g;

  INSERT INTO presus.rubricas (nombre, descripcion, puntaje_maximo)
  SELECT 'Rúbrica de Pre-sustentación v' || g, 'Rúbrica estándar de evaluación de pre-sustentaciones, versión ' || g, 100
  FROM generate_series(1,20) g;

  INSERT INTO presus.criterios_rubrica (rubrica_id, nombre, descripcion, orden, ponderacion)
  SELECT ((g-1)/6)+1, crit_nombres[((g-1)%6)+1], NULL, ((g-1)%6)+1, ROUND((100.0/6)::numeric,2)
  FROM generate_series(1,120) g;

  -- ============================================================================
  -- 2. USUARIOS (51,433 nuevos; ya existen 2 -- admin y demo -- ids 1,2)
  --    Distribución: 41,001 ESTUDIANTE, 9,807 DOCENTE, 500 COORDINADOR, 125 ADMIN
  -- ============================================================================
  INSERT INTO presus.usuarios (nombre, apellido, email, password, telefono, rol, rol_id, activo, creado_en)
  SELECT
    nombres[((g-1)%40)+1],
    apellidos[((g*7-1)%40)+1],
    'usuario' || (g+2) || '@uteq.edu.ec',
    pw_hash,
    '09' || lpad((80000000 + g)::text, 8, '0'),
    CASE WHEN g<=41001 THEN 'ESTUDIANTE' WHEN g<=50808 THEN 'DOCENTE' WHEN g<=51308 THEN 'COORDINADOR' ELSE 'ADMIN' END,
    CASE WHEN g<=41001 THEN 4 WHEN g<=50808 THEN 2 WHEN g<=51308 THEN 3 ELSE 1 END,
    true,
    now() - ((g % 900) * interval '1 day')
  FROM generate_series(1,51433) g
  ORDER BY g;

  SELECT min(id) INTO v_est_ini FROM presus.usuarios WHERE rol_id=4 AND id>2;
  SELECT min(id) INTO v_doc_ini FROM presus.usuarios WHERE rol_id=2 AND id>2;

  -- ============================================================================
  -- 3. ESTUDIANTE (41,001) -- 1:1 con los usuarios ESTUDIANTE recién creados
  -- ============================================================================
  INSERT INTO presus.estudiante (usuario_id, carrera_id, carrera, semestre, semestre_actual, telefono, expediente_codigo, periodo_ingreso_id, creado_en)
  SELECT v_est_ini + g - 1, 1, 'Ingeniería en Software', (((g-1)%10)+1)||'vo', (((g-1)%10)+1),
         '09'||lpad((70000000+g)::text,8,'0'), 'EXP-'||lpad((v_est_ini+g-1)::text,7,'0'),
         ((g-1)%6)+1, now() - ((g%900)*interval '1 day')
  FROM generate_series(1,41001) g
  ORDER BY g;

  -- ============================================================================
  -- 4. DOCENTE (9,807) -- 1:1 con los usuarios DOCENTE recién creados
  -- ============================================================================
  INSERT INTO presus.docente (usuario_id, facultad_id, area_especialidad, carga_horaria_semanal, disponible, creado_en)
  SELECT v_doc_ini + g - 1, 1, areas_doc[((g-1)%8)+1], 10 + (g%30), true, now() - ((g%900)*interval '1 day')
  FROM generate_series(1,9807) g
  ORDER BY g;

  -- ============================================================================
  -- 5. SOLICITUD (44,002) -- FK estudiante_id cíclico (algunos estudiantes con
  --    2 solicitudes en su carrera). estado_id sigue un embudo realista que las
  --    tablas siguientes (tutores/anteproyectos/cronograma/...) respetan.
  -- ============================================================================
  INSERT INTO presus.solicitud (estudiante_id, titulo_tema, modalidad_titulacion_id, linea_investigacion_id,
                                 area_tematica_id, convocatoria_id, estado_id, estado, fecha_registro,
                                 actualizado_en, creado_por, actualizado_por)
  SELECT s.estudiante_id, s.titulo, s.modalidad_titulacion_id, s.linea_investigacion_id, s.area_tematica_id,
         s.convocatoria_id, es.id, es.codigo, s.fecha_registro, s.actualizado_en, s.creador_usuario_id, s.creador_usuario_id
  FROM (
    SELECT g,
      ((g-1) % 41001) + 1 AS estudiante_id,
      v_est_ini + ((g-1) % 41001) AS creador_usuario_id,
      temas[((g-1)%10)+1] || ' ' || dominios[((g*3-1)%15)+1] AS titulo,
      ((g-1)%3)+1 AS modalidad_titulacion_id,
      ((g-1)%4)+1 AS linea_investigacion_id,
      ((g-1)%8)+1 AS area_tematica_id,
      ((g-1)%12)+1 AS convocatoria_id,
      now() - ((g%900)*interval '1 day') AS fecha_registro,
      now() - ((g%800)*interval '1 day') AS actualizado_en,
      CASE
        WHEN g <= 10780 THEN (CASE WHEN g % 10 = 0 THEN 8 ELSE 7 END)  -- COMPLETADA (con 10% RECHAZADA)
        WHEN g <= 15401 THEN 5                                         -- CALIFICADA
        WHEN g <= 24201 THEN 4                                         -- EVALUACION
        WHEN g <= 30801 THEN 3                                         -- TUTORIA
        WHEN g <= 40000 THEN 2                                         -- ENVIADA
        ELSE (CASE WHEN g % 7 = 0 THEN 9 ELSE 1 END)                   -- SUSPENDIDA / CREADA
      END AS estado_id_calc
    FROM generate_series(1,44002) g
  ) s
  JOIN presus.estados_solicitud es ON es.id = s.estado_id_calc
  ORDER BY s.g;

  -- ============================================================================
  -- 6. TUTORES (30,801) -- solicitud_id = g (subconjunto 1..30801 de solicitud,
  --    el embudo más ancho: toda solicitud que avanzó más allá de "ENVIADA")
  -- ============================================================================
  INSERT INTO presus.tutores (docente_id, solicitud_id, fecha_asignacion, estado, estado_id, observaciones)
  SELECT ((g-1)%9807)+1, g, now() - ((g%850)*interval '1 day'),
         CASE WHEN g<=10780 THEN 'COMPLETADA' ELSE 'ACTIVO' END,
         CASE WHEN g<=10780 THEN 3 ELSE 2 END,
         NULL
  FROM generate_series(1,30801) g
  ORDER BY g;

  -- ============================================================================
  -- 7. ANTEPROYECTOS (26,401) -- solicitud_id = g, subconjunto de tutores
  -- ============================================================================
  INSERT INTO presus.anteproyectos (solicitud_id, fecha_envio, estado, estado_id, tamano_bytes, sha256_hash, archivo_pdf, observaciones)
  SELECT g, (now() - ((g%800)*interval '1 day'))::date,
         CASE WHEN g<=10780 THEN 'APROBADO' WHEN g<=24201 THEN 'ENVIADO' ELSE 'PENDIENTE' END,
         CASE WHEN g<=10780 THEN 3 WHEN g<=24201 THEN 2 ELSE 1 END,
         500000 + (g%2000000),
         encode(sha256((g||'-anteproyecto')::bytea),'hex'),
         'anteproyectos/anteproyecto_' || g || '.pdf',
         NULL
  FROM generate_series(1,26401) g
  ORDER BY g;

  -- ============================================================================
  -- 8. CRONOGRAMA (24,201) -- solicitud_id = g, subconjunto de anteproyectos
  -- ============================================================================
  INSERT INTO presus.cronograma (solicitud_id, sala_id, bloque_id, convocatoria_id, estado_id, estado, numero_intento, duracion_min, fecha_inicio, creado_en)
  SELECT g, ((g-1)%15)+1, ((g-1)%12)+1, ((g-1)%12)+1,
         CASE WHEN g<=15401 THEN 2 ELSE 1 END,
         CASE WHEN g<=15401 THEN 'REALIZADO' ELSE 'PROGRAMADO' END,
         CASE WHEN g%50=0 THEN 2 ELSE 1 END,
         60,
         now() - ((g%700)*interval '1 day') + ((g%8)*interval '1 hour'),
         now() - ((g%750)*interval '1 day')
  FROM generate_series(1,24201) g
  ORDER BY g;

  -- ============================================================================
  -- 9. MIEMBROS_TRIBUNAL (46,203 = 15,401 solicitudes x 3 roles) --
  --    PRESIDENTE/VOCAL_1/VOCAL_2 con docentes distintos por solicitud
  -- ============================================================================
  INSERT INTO presus.miembros_tribunal (solicitud_id, docente_id, rol_jurado_id, confirmado, asignado_en)
  SELECT g, ((g-1+k) % 9807)+1, k+1, true, now() - ((g%700)*interval '1 day')
  FROM generate_series(1,15401) g, generate_series(0,2) k
  ORDER BY g, k;

  -- ============================================================================
  -- 10. EVALUADORES (30,803 = 15,401 instructor + 15,401 jurado-presidente + 1)
  -- ============================================================================
  INSERT INTO presus.evaluadores (solicitud_id, docente_id, tipo_evaluador_id, miembro_tribunal_id, peso, fecha_asignacion)
  SELECT * FROM (
    SELECT g AS solicitud_id, ((g-1)%9807)+1 AS docente_id, 2 AS tipo_evaluador_id,
           NULL::bigint AS miembro_tribunal_id, 0.4 AS peso, now() - ((g%700)*interval '1 day') AS fecha_asignacion
    FROM generate_series(1,15401) g
    UNION ALL
    SELECT g, ((g-1)%9807)+1, 1, (g-1)*3+1, 0.6, now() - ((g%700)*interval '1 day')
    FROM generate_series(1,15401) g
    UNION ALL
    SELECT 1, 1, 1, 1, 0.6, now()
  ) x
  ORDER BY solicitud_id, tipo_evaluador_id;

  -- ============================================================================
  -- 11. EVALUACIONES (15,400) y EVALUACIONES_FINALES (15,401)
  -- ============================================================================
  INSERT INTO presus.evaluaciones (solicitud_id, rubrica_id, peso_instructor, peso_jurado, nota_instructor, nota_jurado, nota_final, resultado)
  SELECT solicitud_id, rubrica_id, 0.4, 0.6, nota_instructor, nota_jurado, nota_final,
         CASE WHEN nota_final>=7 THEN 'APROBADO' ELSE 'REPROBADO' END
  FROM (
    SELECT base.*, ROUND((0.4*nota_instructor+0.6*nota_jurado)::numeric,2) AS nota_final
    FROM (
      SELECT g AS solicitud_id, ((g-1)%20)+1 AS rubrica_id,
             ROUND((5+(g%50)/10.0)::numeric,2) AS nota_instructor,
             ROUND((5+((g*3)%50)/10.0)::numeric,2) AS nota_jurado
      FROM generate_series(1,15400) g
    ) base
  ) calc
  ORDER BY solicitud_id;

  INSERT INTO presus.evaluaciones_finales (solicitud_id, rubrica_id, peso_instructor, peso_jurado, nota_instructor, nota_jurado_promedio, nota_final, resultado_id, fecha_calculo)
  SELECT solicitud_id, rubrica_id, 0.4, 0.6, nota_instructor, nota_jurado, nota_final,
         CASE WHEN nota_final>=7 THEN 1 ELSE 2 END, fecha_calculo
  FROM (
    SELECT base.*, ROUND((0.4*nota_instructor+0.6*nota_jurado)::numeric,2) AS nota_final
    FROM (
      SELECT g AS solicitud_id, ((g-1)%20)+1 AS rubrica_id,
             ROUND((5+(g%50)/10.0)::numeric,2) AS nota_instructor,
             ROUND((5+((g*3)%50)/10.0)::numeric,2) AS nota_jurado,
             now() - ((g%600)*interval '1 day') AS fecha_calculo
      FROM generate_series(1,15401) g
    ) base
  ) calc
  ORDER BY solicitud_id;

  -- ============================================================================
  -- 12. EVALUACIONES_JURADO (46,203) -- 1:1 con miembros_tribunal
  -- ============================================================================
  INSERT INTO presus.evaluaciones_jurado (solicitud_id, jurado_id, nota_jurado, resultado, fecha_registro)
  SELECT ((mt_id-1)/3)+1, mt_id, nota, CASE WHEN nota>=7 THEN 'APROBADO' ELSE 'REPROBADO' END,
         now() - ((mt_id%600)*interval '1 day')
  FROM (
    SELECT mt_id, ROUND((5+(mt_id%50)/10.0)::numeric,2) AS nota
    FROM generate_series(1,46203) mt_id
  ) x
  ORDER BY mt_id;

  -- ============================================================================
  -- 13. EVALUACIONES_CRITERIO (~92,409 = 30,803 evaluadores x 3 criterios c/u)
  --     jurado_id apunta siempre al PRESIDENTE del tribunal de esa solicitud.
  -- ============================================================================
  INSERT INTO presus.evaluaciones_criterio (evaluador_id, criterio_id, solicitud_id, jurado_id, nota_obtenida, escala, registrado_en)
  SELECT ev.id, c.criterio_id, ev.solicitud_id, (ev.solicitud_id-1)*3+1,
         (3 + ((ev.id + c.k*11) % 7))::float, 10, now() - ((ev.id%600)*interval '1 day')
  FROM presus.evaluadores ev
  CROSS JOIN LATERAL (
    SELECT k, ((ev.id + k*37 - 1) % 120) + 1 AS criterio_id
    FROM generate_series(0,2) k
  ) c;

  -- ============================================================================
  -- 14. ACTAS (10,780) -- solicitud_id = g, el subconjunto ya "COMPLETADA"
  -- ============================================================================
  INSERT INTO presus.actas (solicitud_id, fecha_generacion, firmada, firmada_presidente, firmada_tutor, firmada_vocal1, firmada_vocal2,
                             fecha_firma_presidente, fecha_firma_tutor, fecha_firma_vocal1, fecha_firma_vocal2)
  SELECT g, (now() - ((g%600)*interval '1 day'))::date,
         true, true, true, true, true,
         now() - ((g%600)*interval '1 day'), now() - ((g%600)*interval '1 day'),
         now() - ((g%600)*interval '1 day'), now() - ((g%600)*interval '1 day')
  FROM generate_series(1,10780) g
  ORDER BY g;

  -- ============================================================================
  -- 15. TUTORIA_FASES (49,283 = 30,801 fase-1 + 18,482 fase-2)
  -- ============================================================================
  INSERT INTO presus.tutoria_fases (tutor_id, numero_fase, fecha_inicio, fecha_aprobacion, estado, estado_id, sha256_pdf, archivo_pdf_estudiante, tamano_pdf_bytes)
  SELECT tutor_id, numero_fase, fecha_inicio, fecha_aprobacion, estado, estado_id, sha256_pdf, archivo_pdf_estudiante, tamano_pdf_bytes
  FROM (
    SELECT g AS tutor_id, 1 AS numero_fase,
           now() - ((g%700)*interval '1 day') AS fecha_inicio,
           CASE WHEN g<=24201 THEN now() - ((g%650)*interval '1 day') ELSE NULL END AS fecha_aprobacion,
           CASE WHEN g<=24201 THEN 'APROBADA' ELSE 'PENDIENTE_TUTOR' END AS estado,
           CASE WHEN g<=24201 THEN 3 ELSE 2 END AS estado_id,
           encode(sha256((g||'-fase1')::bytea),'hex') AS sha256_pdf,
           'tutorias/fase1_'||g||'.pdf' AS archivo_pdf_estudiante,
           (200000+(g%500000))::bigint AS tamano_pdf_bytes
    FROM generate_series(1,30801) g
    UNION ALL
    SELECT g, 2,
           now() - ((g%600)*interval '1 day'),
           CASE WHEN g<=15401 THEN now() - ((g%550)*interval '1 day') ELSE NULL END,
           CASE WHEN g<=15401 THEN 'APROBADA' ELSE 'PENDIENTE_TUTOR' END,
           CASE WHEN g<=15401 THEN 3 ELSE 2 END,
           encode(sha256((g||'-fase2')::bytea),'hex'),
           'tutorias/fase2_'||g||'.pdf',
           (200000+(g%500000))::bigint
    FROM generate_series(1,18482) g
  ) x
  ORDER BY tutor_id, numero_fase;

  -- ============================================================================
  -- 16. TUTORIA_MENSAJES (172,489 = fases con 3 o 4 mensajes cada una)
  -- ============================================================================
  INSERT INTO presus.tutoria_mensajes (fase_id, remitente_id, tipo, tipo_mensaje_id, contenido, fecha_envio, leido)
  SELECT tf.id,
         CASE WHEN k%2=0 THEN doc_u.id ELSE est_u.id END,
         CASE WHEN k=0 THEN 'TEXTO' WHEN k=mcnt.msgs_for_fase-1 AND tf.estado='APROBADA' THEN 'APROBACION' ELSE 'RESPUESTA' END,
         CASE WHEN k=0 THEN 1 WHEN k=mcnt.msgs_for_fase-1 AND tf.estado='APROBADA' THEN 3 ELSE 2 END,
         mensajes_tutoria[((tf.id+k)%8)+1],
         tf.fecha_inicio + (k * interval '2 days'),
         (k < mcnt.msgs_for_fase - 1)
  FROM presus.tutoria_fases tf
  JOIN presus.tutores tu ON tu.id = tf.tutor_id
  JOIN presus.docente d ON d.id = tu.docente_id
  JOIN presus.usuarios doc_u ON doc_u.id = d.usuario_id
  JOIN presus.solicitud sol ON sol.id = tu.solicitud_id
  JOIN presus.estudiante es ON es.id = sol.estudiante_id
  JOIN presus.usuarios est_u ON est_u.id = es.usuario_id
  CROSS JOIN LATERAL (SELECT CASE WHEN tf.id <= 24640 THEN 4 ELSE 3 END AS msgs_for_fase) mcnt
  CROSS JOIN LATERAL generate_series(0, mcnt.msgs_for_fase-1) k
  ORDER BY tf.id, k;

  -- ============================================================================
  -- 17. HISTORIAL_ESTADOS_SOLICITUD (52,806 = 44,002 base + 8,804 con 2da transición)
  -- ============================================================================
  INSERT INTO presus.historial_estados_solicitud (solicitud_id, estado_anterior_id, estado_nuevo_id, usuario_id, fecha_cambio, comentario)
  SELECT sol.id,
         CASE WHEN kk.k=0 THEN NULL ELSE 1 END,
         CASE WHEN kk.k=0 THEN 1 ELSE sol.estado_id END,
         est_u.id,
         sol.fecha_registro + (kk.k * interval '5 days'),
         CASE WHEN kk.k=0 THEN 'Solicitud creada' ELSE 'Actualización de estado' END
  FROM presus.solicitud sol
  JOIN presus.estudiante es ON es.id = sol.estudiante_id
  JOIN presus.usuarios est_u ON est_u.id = es.usuario_id
  CROSS JOIN LATERAL generate_series(0, CASE WHEN sol.id<=8804 THEN 1 ELSE 0 END) AS kk(k)
  ORDER BY sol.id, kk.k;

  -- ============================================================================
  -- 18. HISTORIAL_CRONOGRAMA (3,630) -- reprogramaciones sobre un subconjunto de cronogramas
  -- ============================================================================
  INSERT INTO presus.historial_cronograma (cronograma_id, sala_anterior_id, sala_nueva_id, usuario_id, fecha_anterior, fecha_cambio, fecha_nueva, motivo)
  SELECT g, ((g-1)%15)+1, (g%15)+1, 1,
         now() - ((g%600+5)*interval '1 day'),
         now() - ((g%600)*interval '1 day'),
         now() - (GREATEST(g%600-3,0)*interval '1 day'),
         'Reprogramación por disponibilidad de sala'
  FROM generate_series(1,3630) g
  ORDER BY g;

  -- ============================================================================
  -- 19. NOTIFICACIONES (145,000) -- volumen aumentado para compensar la tabla
  --     legacy "jurados" (documentada en VOLUMEN-DATOS.md, ya no existe en el esquema)
  -- ============================================================================
  INSERT INTO presus.notificaciones (usuario_id, mensaje, leida, fecha)
  SELECT
    CASE WHEN g%5=0 THEN v_doc_ini + ((g-1)%9807) ELSE v_est_ini + ((g-1)%41001) END,
    notif_msgs[((g-1)%6)+1],
    (g%3!=0),
    now() - ((g%900)*interval '1 day')
  FROM generate_series(1,145000) g
  ORDER BY g;

  -- ============================================================================
  -- 20. DISPONIBILIDAD_SALA (95,040 = 15 salas x 12 bloques x 528 días)
  -- ============================================================================
  INSERT INTO presus.disponibilidad_sala (sala_id, bloque_id, fecha, disponible, motivo)
  SELECT s, b, (make_date(2024,1,1) + d), (d%17!=0),
         CASE WHEN d%17=0 THEN 'Mantenimiento programado' ELSE NULL END
  FROM generate_series(1,15) s, generate_series(1,12) b, generate_series(0,527) d;

  RAISE NOTICE 'Generación de volumen de datos completada.';
END $$;

-- =============================================================================
-- Verificación: conteo total (misma consulta que docs/basedatos/VOLUMEN-DATOS.md)
-- =============================================================================
SELECT r.tablename,
  (xpath('/row/c/text()', query_to_xml(
     format('SELECT count(*) AS c FROM presus.%I', r.tablename), false, true, ''
  )))[1]::text::bigint AS filas
FROM pg_tables r WHERE r.schemaname = 'presus'
ORDER BY filas DESC;
