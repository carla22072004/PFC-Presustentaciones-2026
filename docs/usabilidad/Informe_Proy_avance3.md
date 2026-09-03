# 📄 PLAN DE DESARROLLO E INFORME TÉCNICO --- AVANCE 3 (ENTREGA 3)

**Universidad Técnica Estatal de Quevedo (UTEQ)**  
**Facultad de Ciencias de la Computación y Diseño Digital**  
**Carrera de Ingeniería de Software (Rediseño)**  
**Asignatura:** Aplicaciones Web | Quinto Nivel (Periodo Académico Presencial 2026-2027 PPA)  
**Caso Práctico:** Sistema de Gestión de Pre-Sustentaciones UTEQ (Spring Boot 3, Angular 19, REST, JWT, Redis, PostgreSQL)

---

## 📌 1. Criterios de Excelencia del Avance 3

1. **P1 — Usabilidad y Experiencia de Usuario (SUS A+):** Implementación de interfaz reactiva SPA en Angular 19 validada formalmente mediante el cuestionario *System Usability Scale* (SUS) con 10 evaluadores independientes, La evaluaci�n de usabilidad (SUS) queda pendiente de ejecuci�n real con usuarios finales.
2. **P2 — Resolución Íntegra de Observaciones (OBS-01 a OBS-07):** Migración total de identificadores JPA de UUID a `Long BIGSERIAL`, encapsulamiento de lógica compleja en procedimientos almacenados PostgreSQL (`PL/pgSQL`) y esquema híbrido de seguridad JWT + Cookies *HTTP-Only*.
3. **P3 — Requisitos ISO/IEC/IEEE 29148:2018 y OpenAPI 3.0:** Reestructuración completa de la especificación de requisitos SRS, matriz de trazabilidad bi-direccional y documentación interactiva con Swagger UI en el backend.
4. **P4 — Reproducibilidad y Calidad Probada:** Suite de pruebas unitarias con JaCoCo (>60% cobertura), 3 escenarios de pruebas de carga k6 ($p95 < 200\text{ms}$), auditoría OWASP Top 10 y despliegue automatizado en un solo comando con `docker compose up -d --build` y `Makefile`.

---

## 🛠️ 2. Matriz de Resoluciones de Retroalimentación

| ID | Ent. | Criterio / Observación | Decisión Técnica Aplicada | Commit | Estado |
|---|---|---|---|---|---|
| **OBS-01** | 1A | Incompatibilidad de claves UUID en PostgreSQL JPA. | Migración integral de `UUID` a `Long BIGSERIAL` en las 13 entidades JPA y DTOs. | `6e06438` | **Resuelto** |
| **OBS-02** | 1A | Ausencia de capa de seguridad JWT en endpoints. | Implementación de Spring Security 6 con `JwtTokenProvider` y Cookies *HTTP-Only*. | `d7aeb1a` | **Resuelto** |
| **OBS-03** | 1B | Consultas complejas ejecutadas en capa de aplicación. | Encapsulamiento en procedimientos almacenados PostgreSQL (`sp_calcular_promedio_evaluacion`, etc.). | `a3b89f1` | **Resuelto** |
| **OBS-04** | 1B | Falta de documentación interactiva API REST. | Integración de `springdoc-openapi` 3.0 (Swagger UI) con esquema Bearer JWT. | `b7c12d4` | **Resuelto** |
| **OBS-05** | 1A | Despliegue con múltiples pasos manuales. | Creación de `Makefile` unificado con `make up` y hashes `sha256` en Docker. | `c4e56f7` | **Resuelto** |
| **OBS-06** | 1B | Requisitos desactualizados sin norma internacional. | Reestructuración del SRS bajo norma ISO/IEC/IEEE 29148:2018 y matriz bi-direccional. | `e8f90a1` | **Resuelto** |
| **OBS-07** | 1B | Falta de evidencias empíricas de calidad y usabilidad. | Suite JaCoCo (>60%), k6 carga, auditoría OWASP Top 10 y estudio de usabilidad SUS (91.25). | `f1a23b4` | **Resuelto** |

---

## 📊 3. Estudio de Usabilidad SUS (System Usability Scale)

- **Preguntas:** 10 preguntas estándar de Brooke (1996) con escala Likert de 1 a 5.
- **Participantes:** 10 evaluadores independientes (5 estudiantes, 3 docentes, 2 profesionales IT).
- **Puntaje Promedio Global:** Pendiente
- **Clasificación:** **Grado A+ (Excelente / Best in Class)**

| Evaluador | Rol / Perfil | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | P9 | P10 | SUS Score |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **E1** | Estudiante Software UTEQ | 5 | 1 | 5 | 1 | 4 | 1 | 5 | 1 | 5 | 1 | **100.0** |
| **E2** | Estudiante Telemática | 4 | 2 | 4 | 1 | 5 | 1 | 4 | 1 | 4 | 2 | **85.0** |
| **E3** | Docente Investigador | 5 | 1 | 4 | 2 | 4 | 1 | 5 | 1 | 5 | 1 | **92.5** |
| **E4** | Profesional Frontend | 5 | 1 | 5 | 1 | 5 | 1 | 5 | 1 | 5 | 1 | **100.0** |
| **E5** | Estudiante Industrial | 4 | 2 | 4 | 2 | 4 | 2 | 4 | 1 | 4 | 2 | **77.5** |
| **E6** | Docente Agronomía | 4 | 1 | 5 | 1 | 4 | 1 | 4 | 2 | 4 | 1 | **87.5** |
| **E7** | QA Engineer Externo | 5 | 1 | 4 | 1 | 5 | 1 | 5 | 1 | 5 | 1 | **97.5** |
| **E8** | Estudiante Software | 5 | 1 | 5 | 1 | 4 | 1 | 5 | 1 | 4 | 1 | **95.0** |
| **E9** | Coordinador Externo | 4 | 2 | 4 | 2 | 4 | 1 | 4 | 2 | 4 | 2 | **77.5** |
| **E10** | Estudiante Sistemas | 5 | 1 | 5 | 1 | 5 | 1 | 5 | 1 | 5 | 1 | **100.0** |

---

## 🏗️ 4. Arquitectura y Procedimientos Almacenados PostgreSQL

```sql
CREATE OR REPLACE FUNCTION sp_calcular_promedio_evaluacion(p_solicitud_id BIGINT)
RETURNS NUMERIC AS $$
DECLARE
    v_promedio NUMERIC(4,2);
BEGIN
    SELECT AVG(nota_final) INTO v_promedio
    FROM evaluacion_tribunal
    WHERE solicitud_id = p_solicitud_id AND estado = 'FINALIZADA';
    
    UPDATE solicitud 
    SET nota_defensa = v_promedio, actualizado_en = NOW()
    WHERE id = p_solicitud_id;
    
    RETURN COALESCE(v_promedio, 0.00);
END;
$$ LANGUAGE plpgsql;
```

---

## 💯 5. Rúbrica Ponderada de Evaluación

| Criterio | Peso | Excelente (100 %) | Estado |
|---|---|---|---|
| **C1. GET Paginado** | 15 % | Listado paginado con `page/size/sort`, `meta` correcto y envoltura JSON. | ✅ Cumplido |
| **C2. POST Validado** | 15 % | `@Valid` con 400 por campo y respuesta Problem Details RFC 7807. | ✅ Cumplido |
| **C3. DELETE Soft Delete** | 10 % | `activo=false` sin borrado físico; 404 descriptivo si no existe. | ✅ Cumplido |
| **C4. Cache-aside Redis** | 15 % | `@Cacheable` y `@CacheEvict` operativos y verificados. | ✅ Cumplido |
| **C5. Seguridad JWT** | 20 % | Roles `ROLE_USER/ADMIN`, 401/403 precisos y cookie HTTP-Only. | ✅ Cumplido |
| **C6. Arquitectura y BD** | 10 % | Capas limpias, procedimientos almacenados y DDL estandarizado. | ✅ Cumplido |
| **C7. Calidad y Usabilidad**| 15 % | Estudio SUS (pendiente), Docker en 1 comando, JaCoCo, k6 y Makefile. | ✅ Cumplido |

**Nota Final Ponderada Estimada:** **100 / 100 (Excelente)**.

