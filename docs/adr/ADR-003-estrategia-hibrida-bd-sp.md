# ADR-003: Estrategia Híbrida de Acceso a Datos (Spring Data JPA + PL/pgSQL Stored Procedures)

**Estado:** Aceptado  
**Fecha:** 2026-07-20  
**Decisores:** Equipo de Base de Datos y Backend UTEQ  

## Contexto
El sistema gestiona operaciones CRUD simples junto con algoritmos de cálculo de notas ponderadas, generación de reportes multi-tabla y firmas digitales que exigen alto rendimiento transactional y cero riesgo de inyección SQL.

## Decisión
Adopción de una estrategia de persistencia híbrida:
1. Spring Data JPA para CRUD elementales de entidades.
2. Procedimientos almacenados PL/pgSQL puros (`sp_calcular_promedio_evaluacion`, `sp_generar_reporte_defensas`, `sp_asignar_jurado_masivo`, `sp_firmar_acta_digital`) para lógica compleja.

## Consecuencias
- **Positivas:** Máxima velocidad de ejecución en el motor PostgreSQL; cero SQL dinámico o concatenaciones propensas a SQLi; encapsulamiento de reglas académicas institucionales a nivel de BD.
- **Negativas:** Requiere mantener scripts de migración Flyway sincronizados entre entornos de desarrollo y producción.

## Decisión explícita sobre la convención de carpetas (Fase 6, 2026-08-17)

La guía de la Unidad IV sugiere ubicar los procedimientos almacenados en `db/procs/` en la raíz del
repositorio. Este proyecto **ya tenía**, desde antes de esta decisión, una convención distinta y
funcional: los procedimientos almacenados PL/pgSQL viven como parte de las migraciones versionadas de
Flyway en [`backend/src/main/resources/db/migration/V2__stored_procedures.sql`](../../backend/src/main/resources/db/migration/V2__stored_procedures.sql), junto al resto del esquema (`V1__schema_inicial.sql`).

**Decisión: se mantiene la convención Flyway existente. No se espeja una copia de los procedimientos en
`db/procs/`.** Esta es una decisión consciente, no una omisión, por las siguientes razones:

1. **Una sola fuente de verdad ejecutable.** Flyway es lo que realmente aplica estos scripts contra la
   base de datos en cada arranque de la aplicación (`spring.flyway.enabled=true`). Un espejo en
   `db/procs/` sería un archivo de solo lectura, desconectado del pipeline real de migración —
   cualquier cambio futuro a un procedimiento correría el riesgo real de actualizarse en un lugar y no
   en el otro, generando exactamente el tipo de discrepancia "existe pero en la ruta equivocada, y
   además desactualizado" que la regla transversal de la guía busca evitar.
2. **Ya se descubrió en la Fase 5 que la duplicación de contenido SQL en este repositorio es un riesgo
   real, no teórico**: `V1__schema_inicial.sql` tenía su contenido completo duplicado 4 veces por un
   error de copiado, lo que rompía cualquier despliegue desde cero. Introducir una segunda copia
   deliberada (aunque con otro propósito) del mismo contenido SQL iría en contra de la lección aprendida
   de ese incidente.
3. **La ruta real ya está documentada de forma consistente**: `docs/basedatos/CATALOGO-SP.md` documenta
   cada procedimiento almacenado con su ubicación real en `backend/src/main/resources/db/migration/`, y
   `docs/trazabilidad/matriz.csv` referencia los procedimientos por nombre (`sp_asignar_jurado_masivo`,
   `sp_calcular_promedio_evaluacion`, `sp_firmar_acta_digital`, `sp_generar_reporte_defensas`),
   verificable con [`../../scripts/validate-traceability.sh`](../../scripts/validate-traceability.sh).

Si en el futuro el equipo decide que `db/procs/` debe existir literalmente (p. ej. porque una
herramienta de la cátedra lo exige por ruta exacta y no acepta un README de redirección), la alternativa
sería generar esa carpeta con un script que **extrae** los procedimientos desde
`V2__stored_procedures.sql` en vez de mantenerlos escritos a mano por duplicado — así se preserva una
única fuente de verdad incluso si se necesita la ruta literal.
