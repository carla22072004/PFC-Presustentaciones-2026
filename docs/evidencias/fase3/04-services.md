# Evidencia 04 — Integración en Servicios

Se integraron las llamadas a los procedimientos y funciones en las implementaciones de los servicios del backend:

1.  [`EvaluacionServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/EvaluacionServiceImpl.java): Método `calcularPromedioSP` implementado para mapear el retorno de la función `sp_calcular_promedio_evaluacion`.
2.  [`SolicitudServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/SolicitudServiceImpl.java): Método `generarReporteDefensasSP` implementado para convertir el listado genérico de filas en una lista de mapas estructurados.
3.  [`JuradoServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/JuradoServiceImpl.java): Método `asignarJuradoMasivoSP` implementado para delegar la asignación masiva al procedimiento almacenado.
4.  [`TutorServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/TutorServiceImpl.java): Método `obtenerEstadisticasTutoresSP` implementado para procesar las métricas de los tutores.
5.  [`TutoriaServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/TutoriaServiceImpl.java): Método `registrarAvanceSP` implementado para invocar la validación y registro de avance.
6.  [`ActaServiceImpl.java`](file:///C:/Users/carla/OneDrive/Documentos/PROYECTO%20PFC/backend/src/main/java/ec/edu/uteq/presustentaciones/services/ActaServiceImpl.java): Se modificó `firmarActa` para que la firma física y la consolidación de actas se realicen llamando al procedimiento almacenado `sp_firmar_acta_digital` en PostgreSQL.

*Evidencia registrada con marca de tiempo:* 2026-08-16T22:51:00-05:00
