# Evidencia 03 — Conexión con Spring Data JPA

Se configuraron los repositorios de Java para llamar a los procedimientos y funciones:

1.  [`EvaluacionFinalRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/EvaluacionFinalRepository.java): Método `calcularPromedioEvaluacionSp` mapeado con `@Query(..., nativeQuery = true)`.
2.  [`SolicitudRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/SolicitudRepository.java): Método `generarReporteDefensasSp` mapeado con `@Query(..., nativeQuery = true)`.
3.  [`JuradoRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/JuradoRepository.java): Método `spAsignarJuradoMasivo` mapeado con `@Procedure`.
4.  [`ActaRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/ActaRepository.java): Método `spFirmarActaDigital` mapeado con `@Procedure`.
5.  [`TutorRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/TutorRepository.java): Método `obtenerEstadisticasTutoresSp` mapeado con `@Query(..., nativeQuery = true)`.
6.  [`TutoriaFaseRepository.java`](../../../backend/src/main/java/ec/edu/uteq/presustentaciones/repositories/TutoriaFaseRepository.java): Método `spRegistrarTutoriaAvance` mapeado con `@Procedure`.

*Evidencia registrada con marca de tiempo:* 2026-08-16T22:45:00-05:00
