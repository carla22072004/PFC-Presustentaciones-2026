# ADR-006: Despliegue Reproducible mediante Docker Compose y Anclaje Criptográfico (SHA-256)

**Estado:** Aceptado  
**Fecha:** 2026-07-28  
**Decisores:** Equipo DevOps y Arquitectura UTEQ  

## Contexto
Se debe garantizar que el proyecto se levante en cualquier infraestructura limpia de forma idéntica mediante el comando único `make up`.

## Decisión
Contenerizar todos los componentes (PostgreSQL 15, Redis 7, Backend Java 17, Frontend Nginx) anclando cada imagen base en `docker-compose.yml` utilizando su digest criptográfico exacto `sha256:...` en lugar de etiquetas variables.

## Consecuencias
- **Positivas:** Reproducibilidad determinista al 100%; inmunidad a cambios o actualizaciones no deseadas en registros públicos de Docker Hub; despliegue con comando único `make up`.
- **Negativas:** Obliga a actualizar manualmente los hashes SHA-256 cuando se decida subir de versión una imagen base.
