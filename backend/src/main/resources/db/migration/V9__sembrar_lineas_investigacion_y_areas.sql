-- =============================================================================
-- V9__sembrar_lineas_investigacion_y_areas.sql
-- Mismo problema que V8 (catálogo requerido nunca sembrado), encontrado un paso más
-- adelante en el mismo formulario "Nueva Solicitud": RegistrarSolicitudComponent
-- (Frontend) marca "lineaInvestigacion" y "areaTematica" como Validators.required,
-- pero presus.lineas_investigacion y presus.areas_tematicas están vacías en una base
-- de datos recién clonada -- los <select> quedan sin opciones y el botón "Enviar a
-- Revisión" se queda deshabilitado para siempre. Se siembran líneas y áreas
-- razonables para Ingeniería en Software (la única carrera sembrada por el
-- CommandLineRunner de arranque, ver PreSustentacionesApplication.initDemoData()).
-- =============================================================================

-- La FK facultad_id -> facultades depende de la fila id=1 que
-- PreSustentacionesApplication.initDemoData() siembra en el arranque de la app --
-- pero eso corre DESPUÉS de las migraciones de Flyway, así que en una base de datos
-- recién migrada esa fila todavía no existe. Se siembra aquí con el mismo criterio
-- idempotente (OVERRIDING SYSTEM VALUE + ON CONFLICT DO NOTHING) para que esta
-- migración no falle por la FK; initDemoData() sigue corriendo sin problema porque
-- su propio INSERT también es no-op si la fila ya existe.
INSERT INTO presus.facultades (id, codigo, nombre) OVERRIDING SYSTEM VALUE VALUES
    (1, 'FCI', 'Facultad de Ciencias de la Ingeniería')
ON CONFLICT (id) DO NOTHING;

INSERT INTO presus.lineas_investigacion (facultad_id, codigo, nombre, descripcion) VALUES
    (1, 'ISW-CAL', 'Ingeniería de Software y Calidad', 'Procesos, arquitectura, pruebas y calidad de software.'),
    (1, 'ISW-IA', 'Inteligencia Artificial y Ciencia de Datos', 'Aprendizaje automático, minería de datos y sistemas inteligentes.'),
    (1, 'ISW-WEB', 'Tecnologías Web y Móviles', 'Desarrollo de aplicaciones web, móviles y arquitecturas distribuidas.'),
    (1, 'ISW-SEG', 'Redes y Seguridad Informática', 'Infraestructura de redes, ciberseguridad y protección de datos.')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO presus.areas_tematicas (linea_investigacion_id, nombre, descripcion)
SELECT l.id, a.nombre, a.descripcion
FROM presus.lineas_investigacion l
JOIN (VALUES
    ('ISW-CAL', 'Automatización de pruebas de software', NULL),
    ('ISW-CAL', 'Arquitectura y patrones de diseño', NULL),
    ('ISW-IA', 'Sistemas de recomendación', NULL),
    ('ISW-IA', 'Procesamiento de lenguaje natural', NULL),
    ('ISW-WEB', 'Aplicaciones web progresivas', NULL),
    ('ISW-WEB', 'Desarrollo móvil multiplataforma', NULL),
    ('ISW-SEG', 'Auditoría y análisis de vulnerabilidades', NULL),
    ('ISW-SEG', 'Criptografía aplicada', NULL)
) AS a(codigo_linea, nombre, descripcion) ON a.codigo_linea = l.codigo
WHERE NOT EXISTS (
    SELECT 1 FROM presus.areas_tematicas existente
    WHERE existente.linea_investigacion_id = l.id AND existente.nombre = a.nombre
);
