# Fase 3.5 — Integración de Stored Procedures en Controladores REST

## Estado: ✅ COMPLETADA

**Commit:** `e608494`
**Tests:** 46 OK, 0 fallos — BUILD SUCCESS

---

## Nuevos endpoints REST que exponen SPs

### 1. `POST /api/evaluaciones/calcular-promedio/{solicitudId}`

**Controlador:** `EvaluacionController`  
**SP:** `presus.sp_calcular_promedio_evaluacion(p_solicitud_id)`  
**Rol requerido:** público (autenticado)  
**Flujo:**
```
POST /api/evaluaciones/calcular-promedio/42
    → EvaluacionController.calcularPromedio(42)
    → EvaluacionService.calcularPromedioSP(42)
    → EvaluacionFinalRepository.calcularPromedioEvaluacionSp(42)
    → SELECT * FROM presus.sp_calcular_promedio_evaluacion(42)
    ← Map<String, Object> con nota_ponderada, nota_instructor, nota_jurado
```

**Ejemplo de respuesta:**
```json
{
  "solicitud_id": 42,
  "nota_ponderada": 8.5,
  "nota_instructor": 9.0,
  "nota_jurado": 7.6
}
```

---

### 2. `GET /api/solicitudes/reporte-defensas?carrera={carrera}`

**Controlador:** `SolicitudController`  
**SP:** `presus.sp_generar_reporte_defensas(p_carrera)`  
**Rol requerido:** `ADMIN`, `DOCENTE`, `COORDINADOR`  
**Flujo:**
```
GET /api/solicitudes/reporte-defensas?carrera=Informatica
    → SolicitudController.reporteDefensas("Informatica")
    → SolicitudService.generarReporteDefensasSP("Informatica")
    → SolicitudRepository.generarReporteDefensasSp("Informatica")
    → SELECT * FROM presus.sp_generar_reporte_defensas('Informatica')
    ← List<Map<String, Object>> con datos consolidados de defensas
```

---

### 3. `POST /api/jurados/asignar-masivo`

**Controlador:** `JuradoController`  
**SP:** `presus.sp_asignar_jurado_masivo(p_solicitud_ids, p_docente_ids, p_rol)`  
**Rol requerido:** `ADMIN`, `COORDINADOR`  
**Body esperado:**
```json
{
  "solicitudIds": [1, 2, 3],
  "docenteIds": [10, 11, 12],
  "rol": "PRESIDENTE"
}
```
**Flujo:**
```
POST /api/jurados/asignar-masivo
    → JuradoController.asignarMasivo(body)
    → JuradoService.asignarJuradoMasivoSP([1,2,3], [10,11,12], "PRESIDENTE")
    → JuradoRepository.spAsignarJuradoMasivo(...)
    → CALL presus.sp_asignar_jurado_masivo(...)
    ← { "mensaje": "Asignación masiva ejecutada correctamente", "asignados": 3, "rol": "PRESIDENTE" }
```

---

### 4. `GET /api/tutores/estadisticas`

**Controlador:** `TutorController`  
**SP:** `presus.sp_obtener_estadisticas_tutores()`  
**Rol requerido:** `ADMIN`, `COORDINADOR`, `DOCENTE`  
**Flujo:**
```
GET /api/tutores/estadisticas
    → TutorController.estadisticas()
    → TutorService.obtenerEstadisticasTutoresSP()
    → TutorRepository.obtenerEstadisticasTutoresSp()
    → SELECT * FROM presus.sp_obtener_estadisticas_tutores()
    ← List<Map<String, Object>> con métricas por tutor
```

---

### 5. `POST /api/tutorias/{tutorId}/registrar-avance`

**Controlador:** `TutoriaController`  
**SP:** `presus.sp_registrar_tutoria_avance(p_tutor_id, p_numero_fase, p_archivo_pdf, p_tamano_bytes, p_sha256)`  
**Rol requerido:** `ESTUDIANTE`, `ADMIN`, `COORDINADOR`  
**Body esperado:**
```json
{
  "numeroFase": 2,
  "archivoPdf": "tutor_7_fase2_uuid.pdf",
  "tamanoBytes": 1048576,
  "sha256": "a3f1...d4c9"
}
```
**Flujo:**
```
POST /api/tutorias/7/registrar-avance
    → TutoriaController.registrarAvanceSP(7, body)
    → TutoriaService.registrarAvanceSP(7, 2, "tutor_7_fase2_uuid.pdf", 1048576, "a3f1...")
    → TutoriaFaseRepository.spRegistrarTutoriaAvance(...)
    → CALL presus.sp_registrar_tutoria_avance(...)
    ← { "mensaje": "Avance de fase registrado correctamente vía stored procedure", ... }
```

---

### 6. `POST /api/actas/firmar/{actaId}` *(ya existía)*

**Controlador:** `ActaController`  
**SP:** `presus.sp_firmar_acta_digital(p_acta_id, p_rol, p_observacion)`  
**Estado:** ✅ ya estaba integrado — no requirió modificación

---

## Resumen de endpoints SP expuestos

| Método | Endpoint | SP | Roles |
|--------|----------|----|-------|
| `POST` | `/api/evaluaciones/calcular-promedio/{id}` | `sp_calcular_promedio_evaluacion` | autenticado |
| `GET` | `/api/solicitudes/reporte-defensas` | `sp_generar_reporte_defensas` | ADMIN, DOCENTE, COORD |
| `POST` | `/api/jurados/asignar-masivo` | `sp_asignar_jurado_masivo` | ADMIN, COORD |
| `GET` | `/api/tutores/estadisticas` | `sp_obtener_estadisticas_tutores` | ADMIN, COORD, DOCENTE |
| `POST` | `/api/tutorias/{id}/registrar-avance` | `sp_registrar_tutoria_avance` | ESTUDIANTE, ADMIN, COORD |
| `POST` | `/api/actas/firmar/{id}` | `sp_firmar_acta_digital` | autenticado ✅ pre-existente |

---

## Resultado de pruebas tras implementación

```
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Correcciones adicionales incluidas en este commit

- `ActaRepository`: faltaba `import org.springframework.data.repository.query.Param`
- `TutorRepository`: faltaban `import org.springframework.data.jpa.repository.Query` e `import org.springframework.data.repository.query.Param`
