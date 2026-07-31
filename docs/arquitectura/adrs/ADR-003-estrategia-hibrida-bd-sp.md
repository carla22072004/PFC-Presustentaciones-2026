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
