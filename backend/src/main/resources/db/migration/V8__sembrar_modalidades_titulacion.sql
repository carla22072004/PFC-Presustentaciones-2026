-- =============================================================================
-- V8__sembrar_modalidades_titulacion.sql
-- Bug real encontrado probando el flujo de "Nueva Solicitud" de punta a punta como
-- estudiante en una base de datos recién clonada: presus.modalidades_titulacion
-- existe pero ninguna migración ni el CommandLineRunner de arranque la siembra, así
-- que el <select> "Modalidad de Titulación" del formulario queda vacío y, al ser un
-- campo obligatorio (SolicitudServiceImpl.crearSolicitud lanza "Debe seleccionar una
-- modalidad de titulación válida" si no llega un ID), un estudiante no puede crear
-- ninguna solicitud desde una instalación nueva -- el catálogo entero "no aparece"
-- porque nunca existió, no por un bug de lectura. Se siembra con el mismo código que
-- ya usaba la única solicitud de prueba pre-existente (PROYECTO) más las otras
-- modalidades de titulación vigentes en el reglamento de régimen académico del CES
-- para carreras de grado en Ecuador.
-- =============================================================================

INSERT INTO presus.modalidades_titulacion (id, codigo, nombre) VALUES
    (1, 'PROYECTO', 'Proyecto Tecnológico'),
    (2, 'ARTICULO', 'Artículo Académico'),
    (3, 'EXAMEN_COMPLEXIVO', 'Examen de Grado o de Fin de Carrera (Complexivo)')
ON CONFLICT (id) DO NOTHING;
