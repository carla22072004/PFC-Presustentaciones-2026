-- =============================================================================
-- V21__orientacion_permisos_y_semilla_temas.sql
-- Centro de Orientación y Titulación (continuación de V20):
--   1) Permisos dinámicos del módulo, dentro del mismo catálogo de V13 para que
--      "Gestionar Permisos" del panel de admin los administre como al resto.
--   2) Datos semilla del catálogo de temas propuestos y de la relación
--      carrera <-> línea de investigación -- sin esto "Generar ideas" y el
--      explorador de temas siempre devuelven una lista vacía en una base recién
--      migrada (mismo criterio que V8/V9 con modalidades y líneas).
--
-- Todo es ADITIVO e IDEMPOTENTE (ON CONFLICT / WHERE NOT EXISTS): no altera ni
-- elimina nada existente y puede convivir con una base ya poblada a mano.
-- =============================================================================

-- ── 1. Permisos ──────────────────────────────────────────────────────────────
-- Ids 1..25 ya usados (V13, V15, V16, V17, V19). Continúa en 26.
INSERT INTO presus.permisos (id, codigo, nombre, categoria, descripcion) VALUES
    (26, 'ORIENTACION_TEMAS_VER',        'Ver temas propuestos',         'Orientación',
     'Explorar el catálogo de temas de titulación propuestos y su detalle'),
    (27, 'ORIENTACION_CATALOGO_GESTIONAR','Gestionar catálogo de orientación','Orientación',
     'Crear, editar y eliminar temas propuestos y recursos de titulación')
ON CONFLICT (id) DO NOTHING;

-- ADMIN y COORDINADOR: ambos permisos. DOCENTE y ESTUDIANTE: solo ver.
INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 1, p.id FROM presus.permisos p WHERE p.codigo IN ('ORIENTACION_TEMAS_VER', 'ORIENTACION_CATALOGO_GESTIONAR')
ON CONFLICT DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 3, p.id FROM presus.permisos p WHERE p.codigo IN ('ORIENTACION_TEMAS_VER', 'ORIENTACION_CATALOGO_GESTIONAR')
ON CONFLICT DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 2, p.id FROM presus.permisos p WHERE p.codigo = 'ORIENTACION_TEMAS_VER'
ON CONFLICT DO NOTHING;

INSERT INTO presus.rol_permisos (rol_id, permiso_id)
SELECT 4, p.id FROM presus.permisos p WHERE p.codigo = 'ORIENTACION_TEMAS_VER'
ON CONFLICT DO NOTHING;

-- ── 2. Relación carrera <-> línea de investigación ───────────────────────────
-- Carrera ISW: la siembra initDemoData() en el arranque, DESPUÉS de Flyway, así que
-- en una base recién migrada aún no existe. Se siembra aquí con el mismo criterio
-- idempotente que V9 usó para la facultad, para que los INSERT de abajo tengan a
-- qué apuntar; initDemoData() sigue siendo no-op si la fila ya está.
INSERT INTO presus.carreras (id, facultad_id, codigo, nombre) OVERRIDING SYSTEM VALUE
VALUES (1, 1, 'ISW', 'Ingeniería en Software')
ON CONFLICT (id) DO NOTHING;

-- Carrera ISW con las 4 líneas de la FCI (V9).
INSERT INTO presus.carrera_linea_investigacion (carrera_id, linea_investigacion_id)
SELECT c.id, l.id
FROM presus.carreras c
JOIN presus.lineas_investigacion l ON l.codigo IN ('ISW-CAL', 'ISW-IA', 'ISW-WEB', 'ISW-SEG')
WHERE c.codigo = 'ISW'
ON CONFLICT DO NOTHING;

-- ── 3. Catálogo de temas propuestos ─────────────────────────────────────────
-- Se resuelven carrera / línea / área por código/nombre (no por id fijo) para no
-- depender del valor exacto de las secuencias. Idempotente por título.
INSERT INTO presus.temas_propuestos
    (titulo, problema, objetivo_general, objetivos_especificos, justificacion,
     beneficiarios, nivel_dificultad, carrera_id, linea_investigacion_id, area_id)
SELECT s.titulo, s.problema, s.objetivo_general, s.objetivos_especificos, s.justificacion,
       s.beneficiarios, s.nivel_dificultad,
       c.id,
       l.id,
       (SELECT a.id FROM presus.areas_tematicas a
         WHERE a.linea_investigacion_id = l.id AND a.nombre = s.area_nombre LIMIT 1)
FROM (VALUES
    ('Plataforma de automatización de pruebas de regresión para APIs REST institucionales',
     'Las pruebas de regresión de los servicios REST de la universidad se ejecutan manualmente, lo que retrasa cada despliegue y deja defectos sin detectar.',
     'Desarrollar una plataforma que orqueste y ejecute automáticamente las pruebas de regresión de las APIs REST institucionales e informe los resultados.',
     '1) Modelar los casos de prueba de forma declarativa. 2) Implementar el motor de ejecución programada. 3) Integrar reportes y alertas con el pipeline de CI.',
     'Reduce el tiempo de validación previo a cada despliegue y aumenta la cobertura de regresión sin costo humano recurrente.',
     'Equipo de desarrollo y aseguramiento de calidad de la Dirección de TIC.',
     'INTERMEDIO', 'ISW-CAL', 'Automatización de pruebas de software'),
    ('Detección temprana de deuda técnica mediante análisis estático en repositorios académicos',
     'Los proyectos de titulación acumulan deuda técnica que nadie mide, y llega a producción sin control.',
     'Construir una herramienta que analice estáticamente los repositorios y estime la deuda técnica con métricas comparables entre proyectos.',
     '1) Seleccionar métricas de mantenibilidad. 2) Integrar analizadores estáticos. 3) Generar un tablero histórico por repositorio.',
     'Permite a los tutores dar retroalimentación objetiva sobre la calidad del código de los estudiantes.',
     'Tutores y estudiantes de la carrera de Software.',
     'INTERMEDIO', 'ISW-CAL', 'Arquitectura y patrones de diseño'),
    ('Sistema de recomendación de tutores de titulación según afinidad temática',
     'La asignación de tutores se hace manualmente y a menudo no coincide con la especialidad del docente ni el interés del estudiante.',
     'Implementar un sistema de recomendación que sugiera tutores a cada estudiante según la afinidad entre el tema propuesto y la producción académica del docente.',
     '1) Construir el perfil temático de cada docente. 2) Vectorizar las propuestas de tema. 3) Calcular y explicar el ranking de afinidad.',
     'Mejora la calidad del acompañamiento y reduce los cambios de tutor a mitad del proceso.',
     'Coordinación de titulación, docentes y estudiantes.',
     'AVANZADO', 'ISW-IA', 'Sistemas de recomendación'),
    ('Asistente conversacional para consultas sobre el reglamento de titulación',
     'Los estudiantes hacen repetidamente las mismas preguntas sobre plazos y requisitos de titulación a la coordinación.',
     'Desarrollar un asistente conversacional que responda consultas sobre el reglamento y el proceso de titulación con citas al documento oficial.',
     '1) Estructurar la base de conocimiento del reglamento. 2) Implementar la recuperación de pasajes relevantes. 3) Evaluar la exactitud de las respuestas.',
     'Descarga a la coordinación de consultas rutinarias y da respuestas disponibles 24/7.',
     'Estudiantes en proceso de titulación y personal de coordinación.',
     'INTERMEDIO', 'ISW-IA', 'Procesamiento de lenguaje natural'),
    ('Aplicación web progresiva para el seguimiento de avances de tutoría sin conexión',
     'Los estudiantes en prácticas rurales pierden acceso a la plataforma de tutorías cuando no tienen internet estable.',
     'Construir una PWA que permita registrar y consultar avances de tutoría offline y sincronizar al recuperar conexión.',
     '1) Diseñar el modelo de datos local. 2) Implementar la sincronización con resolución de conflictos. 3) Validar en condiciones de conectividad intermitente.',
     'Garantiza continuidad del acompañamiento para estudiantes en zonas con mala conectividad.',
     'Estudiantes en prácticas preprofesionales fuera del campus.',
     'INTERMEDIO', 'ISW-WEB', 'Aplicaciones web progresivas'),
    ('Aplicación móvil multiplataforma para reservar salas de pre-sustentación',
     'La reserva de salas para pre-sustentaciones se coordina por mensajería informal y genera choques de horario.',
     'Desarrollar una aplicación móvil multiplataforma para consultar disponibilidad y reservar salas de pre-sustentación en tiempo real.',
     '1) Exponer la disponibilidad de salas como servicio. 2) Implementar la reserva con bloqueo optimista. 3) Enviar recordatorios push.',
     'Elimina los conflictos de agenda y formaliza el uso de los espacios físicos.',
     'Coordinación académica, tribunales y estudiantes.',
     'BASICO', 'ISW-WEB', 'Desarrollo móvil multiplataforma'),
    ('Análisis automatizado de vulnerabilidades en los despliegues del sistema de titulación',
     'No existe un proceso periódico que revise vulnerabilidades conocidas en las dependencias y la configuración del sistema.',
     'Implementar un proceso automatizado que detecte y priorice vulnerabilidades en las dependencias y la configuración de los despliegues.',
     '1) Integrar el escaneo de dependencias en el pipeline. 2) Correlacionar hallazgos con severidad y explotabilidad. 3) Generar reportes accionables.',
     'Reduce la superficie de ataque del sistema que custodia datos académicos sensibles.',
     'Dirección de TIC y auditoría interna.',
     'AVANZADO', 'ISW-SEG', 'Auditoría y análisis de vulnerabilidades'),
    ('Cifrado de extremo a extremo para los documentos de anteproyecto almacenados',
     'Los PDF de anteproyecto se guardan sin cifrar, expuestos ante cualquier acceso indebido al almacenamiento.',
     'Diseñar e implementar un esquema de cifrado de extremo a extremo para los documentos de anteproyecto y sus correcciones.',
     '1) Definir el modelo de claves por usuario. 2) Implementar cifrado en cliente antes de subir. 3) Medir el impacto en el rendimiento.',
     'Protege la propiedad intelectual de los estudiantes y cumple buenas prácticas de protección de datos.',
     'Estudiantes, tutores y la institución.',
     'AVANZADO', 'ISW-SEG', 'Criptografía aplicada'),
    ('Tablero de indicadores del proceso de pre-sustentación para coordinación',
     'La coordinación no tiene una vista consolidada del embudo de pre-sustentaciones y detecta los cuellos de botella tarde.',
     'Construir un tablero de indicadores que muestre en tiempo real el estado del proceso de pre-sustentación y sus tiempos por etapa.',
     '1) Definir los indicadores clave del proceso. 2) Construir las consultas agregadas. 3) Diseñar la visualización y las alertas por umbral.',
     'Permite decisiones basadas en datos y anticipar retrasos antes de que afecten a los estudiantes.',
     'Coordinación de titulación y autoridades de carrera.',
     'INTERMEDIO', 'ISW-CAL', 'Arquitectura y patrones de diseño'),
    ('Generación asistida de actas de pre-sustentación a partir de las evaluaciones de rúbrica',
     'La redacción de actas es manual, repetitiva y propensa a inconsistencias con las notas registradas.',
     'Automatizar la generación del borrador del acta de pre-sustentación a partir de las evaluaciones de rúbrica ya registradas.',
     '1) Modelar la plantilla del acta. 2) Poblarla con los datos de evaluación validados. 3) Permitir revisión y firma digital.',
     'Ahorra tiempo al tribunal y elimina errores de transcripción entre la rúbrica y el acta.',
     'Miembros de tribunal y secretaría académica.',
     'BASICO', 'ISW-WEB', 'Aplicaciones web progresivas')
) AS s(titulo, problema, objetivo_general, objetivos_especificos, justificacion,
       beneficiarios, nivel_dificultad, linea_codigo, area_nombre)
JOIN presus.carreras c ON c.codigo = 'ISW'
JOIN presus.lineas_investigacion l ON l.codigo = s.linea_codigo
WHERE NOT EXISTS (
    SELECT 1 FROM presus.temas_propuestos tp WHERE tp.titulo = s.titulo
);
