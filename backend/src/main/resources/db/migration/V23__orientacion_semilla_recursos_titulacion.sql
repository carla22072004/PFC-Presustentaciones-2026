-- =============================================================================
-- V23__orientacion_semilla_recursos_titulacion.sql
-- Datos semilla del Centro de Recursos de Titulación (tabla recursos_titulacion,
-- creada en V20). Sin esto el listado sale vacío en una base recién migrada.
--
-- Recursos generales (carrera_id NULL = visibles para todas las carreras).
-- Idempotente por (titulo, categoria). No toca datos existentes.
-- =============================================================================

INSERT INTO presus.recursos_titulacion (titulo, categoria, url_archivo, carrera_id)
SELECT s.titulo, s.categoria, s.url_archivo, NULL
FROM (VALUES
    ('Plantilla oficial de anteproyecto de titulación', 'Plantillas',
     'https://uteq.edu.ec/titulacion/plantilla-anteproyecto.docx'),
    ('Guía para redactar el planteamiento del problema', 'Guías',
     'https://uteq.edu.ec/titulacion/guia-planteamiento-problema.pdf'),
    ('Normas APA 7ma edición — resumen práctico', 'Guías',
     'https://uteq.edu.ec/titulacion/normas-apa-7.pdf'),
    ('Reglamento de titulación vigente', 'Reglamentos',
     'https://uteq.edu.ec/titulacion/reglamento-titulacion.pdf'),
    ('Rúbrica de evaluación de la pre-sustentación', 'Reglamentos',
     'https://uteq.edu.ec/titulacion/rubrica-pre-sustentacion.pdf'),
    ('Cronograma general del proceso de titulación', 'Cronogramas',
     'https://uteq.edu.ec/titulacion/cronograma-titulacion.pdf'),
    ('Lista de verificación previa a la entrega del anteproyecto', 'Checklists',
     'https://uteq.edu.ec/titulacion/checklist-anteproyecto.pdf')
) AS s(titulo, categoria, url_archivo)
WHERE NOT EXISTS (
    SELECT 1 FROM presus.recursos_titulacion r
    WHERE r.titulo = s.titulo AND r.categoria = s.categoria
);
