# 👥 ASIGNACIÓN DE ROLES CRediT (Contributor Roles Taxonomy)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Institución:** Universidad Técnica Estatal de Quevedo (UTEQ)  
**Facultad:** Ciencias de la Ingeniería - Carrera de Ingeniería en Software  
**Periodo Académico:** 2026  

---

## 📌 Integrantes del Grupo Universitario y Contribuciones

La siguiente tabla especifica formalmente las contribuciones de cada integrante del equipo utilizando el estándar internacional **CRediT (Contributor Roles Taxonomy)**:

### 1. Jean Carlos Pérez López
* **Rol Principal:** Líder de Proyecto & Backend Lead
* **Roles CRediT Asignados:**
  - **Conceptualization:** Diseño de la arquitectura de la solución de pre-sustentaciones.
  - **Software:** Desarrollo del backend Spring Boot 3.2.1, seguridad JWT, filtros de sesión y lógica de negocio.
  - **Database:** Diseño del modelo relacional PostgreSQL y programación de procedimientos almacenados PL/pgSQL (`sp_calcular_promedio_evaluacion`, `sp_firmar_acta_digital`).
  - **Supervision:** Coordinación de entregables y gestión de ramas en repositorio Git.
  - **Writing - Original Draft:** Redacción de la Especificación de Requisitos SRS (ISO/IEC/IEEE 29148:2018).

### 2. Carla María García Torres
* **Rol Principal:** Frontend Developer & Usability Specialist
* **Roles CRediT Asignados:**
  - **Software:** Desarrollo de componentes UI en Angular 17, diseño CSS responsivo y navegación SPA.
  - **Investigation:** Aplicación del cuestionario de usabilidad SUS (System Usability Scale) a 10 evaluadores externos.
  - **Validation:** Ejecución de pruebas de interfaz y optimización de métricas Lighthouse (Rendimiento >= 80, Accesibilidad >= 90).
  - **Visualization:** Creación de diagramas de arquitectura C4 (Niveles 1, 2 y 3) y maquetación de actas en PDF.
  - **Writing - Review & Editing:** Consolidación del documento ético y consentimientos informados.

### 3. Roberto Antonio Martínez Silva
* **Rol Principal:** DevOps & QA Engineer
* **Roles CRediT Asignados:**
  - **Software:** Configuración de plugin JaCoCo para cobertura de pruebas automatizadas (> 60%) y casos de prueba JUnit 5.
  - **Infrastructure:** Creación del `Makefile` unificado (`make up`), configuración de `docker-compose.yml` y anclaje criptográfico con digests `sha256`.
  - **Formal Analysis:** Ejecución de 3 corridas de pruebas de carga k6 y almacenamiento de métricas crudas JSON.
  - **Security:** Auditoría automatizada de seguridad cubriendo los 6 controles OWASP Top 10.
  - **Resources:** Configuración del servidor de pruebas local e integración continua.

### 4. Laura Patricia Sánchez Mora
* **Rol Principal:** Technical Writer & Software Architect
* **Roles CRediT Asignados:**
  - **Project Administration:** Gestión de la matriz de trazabilidad (`matriz.csv`) y control de observaciones de entregas 1A y 1B.
  - **Data Curation:** Elaboración del Diccionario de Datos (`DATA-DICTIONARY.md`) y catálogo de procedimientos almacenados (`CATALOGO-SP.md`).
  - **Writing - Original Draft:** Redacción del Informe Técnico Final en PDF (`informe-entrega-3.pdf`) y Registros de Decisiones Arquitectónicas (ADRs 001 a 006).
  - **Validation:** Elaboración y verificación de la colección de Postman con más de 20 peticiones REST y assertions.
  - **Resources:** Gestión del registro de DOI en Zenodo y archivo de citación `CITATION.cff`.
