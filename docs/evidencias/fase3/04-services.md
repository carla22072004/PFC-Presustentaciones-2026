# Fase 3.4 — Integración de Stored Procedures en Servicios Java

## Estado: ✅ COMPLETADA

**Commit de referencia:** `41796cc` (base) + commits de Fase 3 anteriores

---

## Flujo de integración implementado

```
HTTP Request
    ↓
Controller  (REST endpoint)
    ↓
Service     (lógica de negocio)
    ↓
Repository  (@Procedure / @Query nativeQuery)
    ↓
Stored Procedure en PostgreSQL (schema presus)
```

---

## Stored Procedures integrados

### 1. `sp_calcular_promedio_evaluacion`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `EvaluacionFinalRepository` | `calcularPromedioEvaluacionSp(Long solicitudId)` |
| Service | `EvaluacionServiceImpl` | `calcularPromedioSP(Long solicitudId)` |
| Interfaz | `EvaluacionService` | `Map<String, Object> calcularPromedioSP(Long)` |

**Query en repositorio:**
```java
@Query(value = "SELECT * FROM presus.sp_calcular_promedio_evaluacion(:solicitudId)", nativeQuery = true)
List<Object[]> calcularPromedioEvaluacionSp(@Param("solicitudId") Long solicitudId);
```

---

### 2. `sp_generar_reporte_defensas`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `SolicitudRepository` | `generarReporteDefensasSp(String carrera)` |
| Service | `SolicitudServiceImpl` | `generarReporteDefensasSP(String carrera)` |
| Interfaz | `SolicitudService` | `List<Map<String, Object>> generarReporteDefensasSP(String)` |

**Query en repositorio:**
```java
@Query(value = "SELECT * FROM presus.sp_generar_reporte_defensas(:carrera)", nativeQuery = true)
List<Object[]> generarReporteDefensasSp(@Param("carrera") String carrera);
```

---

### 3. `sp_asignar_jurado_masivo`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `JuradoRepository` | `spAsignarJuradoMasivo(Long[], Long[], String)` |
| Service | `JuradoServiceImpl` | `asignarJuradoMasivoSP(Long[], Long[], String)` |
| Interfaz | `JuradoService` | `void asignarJuradoMasivoSP(Long[], Long[], String)` |

**Procedure en repositorio:**
```java
@Procedure(procedureName = "presus.sp_asignar_jurado_masivo")
void spAsignarJuradoMasivo(
    @Param("p_solicitud_ids") Long[] solicitudIds,
    @Param("p_docente_ids") Long[] docenteIds,
    @Param("p_rol") String rol
);
```

---

### 4. `sp_firmar_acta_digital`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `ActaRepository` | `spFirmarActaDigital(Long, String, String)` |
| Service | `ActaServiceImpl` | `firmarActa(Long actaId, String rol)` |
| Interfaz | `ActaService` | `Acta firmarActa(Long, String)` |

**Procedure en repositorio:**
```java
@Procedure(procedureName = "presus.sp_firmar_acta_digital")
void spFirmarActaDigital(
    @Param("p_acta_id") Long actaId,
    @Param("p_rol") String rol,
    @Param("p_observacion") String observacion
);
```

---

### 5. `sp_obtener_estadisticas_tutores`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `TutorRepository` | `obtenerEstadisticasTutoresSp()` |
| Service | `TutorServiceImpl` | `obtenerEstadisticasTutoresSP()` |
| Interfaz | `TutorService` | `List<Map<String, Object>> obtenerEstadisticasTutoresSP()` |

**Query en repositorio:**
```java
@Query(value = "SELECT * FROM presus.sp_obtener_estadisticas_tutores()", nativeQuery = true)
List<Object[]> obtenerEstadisticasTutoresSp();
```

---

### 6. `sp_registrar_tutoria_avance`

| Capa | Clase | Método |
|------|-------|--------|
| Repository | `TutoriaFaseRepository` | `spRegistrarTutoriaAvance(Long, Integer, String, Long, String)` |
| Service | `TutoriaServiceImpl` | `registrarAvanceSP(Long, Integer, String, Long, String)` |
| Interfaz | `TutoriaService` | `void registrarAvanceSP(Long, Integer, String, Long, String)` |

**Procedure en repositorio:**
```java
@Procedure(procedureName = "presus.sp_registrar_tutoria_avance")
void spRegistrarTutoriaAvance(
    @Param("p_tutor_id") Long tutorId,
    @Param("p_numero_fase") Integer numeroFase,
    @Param("p_archivo_pdf") String archivoPdf,
    @Param("p_tamano_bytes") Long tamanoBytes,
    @Param("p_sha256") String sha256
);
```

---

## Resultado de pruebas tras integración

```
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
