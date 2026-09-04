-- =============================================================================
-- V24__corregir_tipo_estudiante_id_orientacion.sql
-- Corrige un bug real de V20 (tablas del Centro de Orientación): estudiante_id se
-- declaró INTEGER en dos tablas, pero presus.estudiante.id es BIGINT (Estudiante.id
-- es Long en la entidad). Postgres tolera una FK entre int4 e int8 (son comparables),
-- pero Hibernate con spring.jpa.hibernate.ddl-auto=validate no: al arrancar la app
-- empaquetada (no en `mvn test`, que nunca carga ese application.properties -- ver
-- comentario en src/test/resources/application.properties) falla con
-- "Schema-validation: wrong column type encountered in column [estudiante_id] ...
-- found [int4], but expecting [bigint]" y el backend no arranca.
--
-- Afecta:
--   - progreso_estudiante.estudiante_id  (ProgresoEstudiante.estudiante, @OneToOne)
--   - temas_guardados.estudiante_id      (TemaGuardadoEstudiante.estudiante, @ManyToOne)
--
-- No se modifica V20 (ya aplicada). INTEGER -> BIGINT es un ensanchamiento seguro
-- (sin pérdida de datos) y Postgres permite ALTER COLUMN ... TYPE bigint con las
-- FK/UNIQUE ya existentes sin necesidad de recrearlas a mano. Ambas tablas están
-- vacías en este momento (verificado), pero el ALTER es seguro de todos modos.
-- =============================================================================

ALTER TABLE presus.progreso_estudiante
    ALTER COLUMN estudiante_id TYPE BIGINT;

ALTER TABLE presus.temas_guardados
    ALTER COLUMN estudiante_id TYPE BIGINT;
