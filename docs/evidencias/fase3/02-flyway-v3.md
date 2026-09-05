# Evidencia 02 — Nueva Migración Flyway V3

Se creó el archivo de migración versionado [`V3__stored_procedures_validacion_y_codigos.sql`](../../../backend/src/main/resources/db/migration/V3__stored_procedures_validacion_y_codigos.sql) conteniendo:

1.  La corrección de `sp_calcular_promedio_evaluacion` para integrarse con `evaluaciones_finales` y `resultados_evaluacion`.
2.  La corrección de `sp_generar_reporte_defensas` con los nombres y columnas exactas del esquema físico.
3.  La corrección de `sp_asignar_jurado_masivo` para apuntar a la tabla `miembros_tribunal` y manejar la foreign key del rol.
4.  La corrección de `sp_firmar_acta_digital` para consolidar el estado del campo `firmada`.
5.  La nueva función `sp_obtener_estadisticas_tutores`.
6.  El nuevo procedimiento `sp_registrar_tutoria_avance` para validar y avanzar fases de tutorías.

*Evidencia registrada con marca de tiempo:* 2026-08-16T22:36:18-05:00
