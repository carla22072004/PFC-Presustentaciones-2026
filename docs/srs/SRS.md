# 📘 ESPECIFICACIÓN DE REQUISITOS DE SOFTWARE (SRS)
## Estándar ISO/IEC/IEEE 29148:2018

**Sistema:** Sistema de Gestión de Pre-Sustentaciones de Trabajos de Titulación  
**Organización:** Universidad Técnica Estatal de Quevedo (UTEQ)  
**Versión:** 0.9.0-rc  
**Fecha:** 30 de Julio de 2026  

---

## 1. Introducción

### 1.1 Propósito
El presente documento define los requisitos funcionales y no funcionales para el **Sistema de Gestión de Pre-Sustentaciones UTEQ**, elaborado bajo el estándar internacional **ISO/IEC/IEEE 29148:2018**. Sirve como contrato técnico entre la coordinación académica, docentes, estudiantes y el equipo desarrollador.

### 1.2 Alcance del Sistema
El sistema automatiza el flujo completo de pre-sustentación de anteproyectos de titulación, incluyendo:
- Registro de solicitudes por parte de estudiantes.
- Carga de anteproyectos en formato PDF con verificación de integridad MD5/SHA256.
- Asignación de jurados calificadores por parte de la coordinación.
- Agendamiento de salas y cronogramas de defensa.
- Evaluación en tiempo real basada en rúbricas institucionales.
- Emisión y firma digital multi-actor de actas de pre-sustentación.

### 1.3 Definiciones, Acrónimos y Abreviaturas
- **SRS:** Software Requirements Specification (ISO/IEC/IEEE 29148:2018).
- **UTEQ:** Universidad Técnica Estatal de Quevedo.
- **JWT:** JSON Web Token.
- **SP:** Stored Procedure (Procedimiento Almacenado PL/pgSQL).
- **SUS:** System Usability Scale.

---

## 2. Descripción General

### 2.1 Perspectiva del Producto
El sistema es una solución web arquitectura de N-Capas con Frontend en Angular (SPA) y Backend en Spring Boot 3.x sobre base de datos relacional PostgreSQL 15.

### 2.2 Funciones del Producto
- **RF-01:** Autenticación y Autorización basada en Roles (Estudiante, Docente, Presidente, Vocal, Coordinador, Admin).
- **RF-02:** Gestión de Solicitudes y Documentos de Anteproyecto con Control de Versiones.
- **RF-03:** Asignación y Notificación Automática de Jurados Calificadores.
- **RF-04:** Programación e Integración de Cronogramas con Validación de Disponibilidad de Salas.
- **RF-05:** Evaluación de Defensa Mediante Rúbricas Ponderadas Institucionales.
- **RF-06:** Generación Automática de Actas de Pre-Sustentación en PDF.
- **RF-07:** Firma Digital Multi-Actor de Actas.

### 2.3 Características de los Usuarios
| Rol | Descripción | Permisos Clave |
|---|---|---|
| **Estudiante** | Postulante a titulación | Registrar solicitud, subir anteproyecto, consultar estado y acta. |
| **Docente / Jurado** | Evaluador asignado | Revisar anteproyecto, calificar rúbrica, registrar observaciones. |
| **Presidente del Tribunal** | Líder de la defensa | Presidir pre-sustentación, consolidar notas, firmar acta. |
| **Coordinador** | Gestor académico | Asignar jurados, programar fechas y salas, emitir reportes. |
| **Administrador** | Administrador del sistema | Gestionar usuarios, catálogos, parámetros y seguridad. |

---

## 3. Requisitos Específicos

### 3.1 Requisitos Funcionales y Historias de Usuario

#### HU-01: Autenticación Segura (RF-01)
* **Como:** Usuario del sistema (Estudiante/Docente/Admin).
* **Quiero:** Iniciar sesión con mi correo institucional y contraseña.
* **Para:** Acceder a las funcionalidades correspondientes a mi rol de forma segura.
* **Criterios de Aceptación:**
  - Debe retornar un token JWT válido (expiración 24h).
  - Debe establecer la cookie HTTP-Only `jwtToken`.

#### HU-02: Registro de Solicitud de Pre-Sustentación (RF-02)
* **Como:** Estudiante egresado.
* **Quiero:** Registrar mi tema de titulación y adjuntar el anteproyecto PDF.
* **Para:** Iniciar el proceso formal de revisión y agendamiento.

#### HU-03: Asignación de Jurados (RF-03)
* **Como:** Coordinador de carrera.
* **Quiero:** Asignar 3 docentes jurados a una solicitud.
* **Para:** Conformar el tribunal calificador de la pre-sustentación.

#### HU-04: Evaluación por Rúbrica (RF-05)
* **Como:** Jurado calificador.
* **Quiero:** Evaluar cada criterio de la rúbrica (Propuesta, Documento, Exposición).
* **Para:** Calcular la calificación ponderada del estudiante de forma automática.

#### HU-05: Emisión de Actas (RF-06, RF-07)
* **Como:** Presidente del tribunal.
* **Quiero:** Generar el acta final de pre-sustentación y registrar la firma digital.
* **Para:** Dar validez legal y académica al resultado obtenido.

---

## 4. Requisitos No Funcionales (RNF)

### 4.1 Rendimiento (RNF-01)
- El tiempo de respuesta de las peticiones REST no debe superar los 500 ms bajo una carga promedio de 50 usuarios concurrentes.
- El cálculo de notas mediante el SP `sp_calcular_promedio_evaluacion` debe ejecutarse en menos de 50 ms.

### 4.2 Seguridad (RNF-02)
- Comunicación HTTPS y protección contra OWASP Top 10 (Inyección SQL, XSS, CSRF, Exposición de Datos).
- Uso exclusivo de consultas parametrizadas y procedimientos almacenados PL/pgSQL.

### 4.3 Usabilidad (RNF-03)
- El puntaje global del Cuestionario SUS (System Usability Scale) aplicado a evaluadores externos debe ser superior a 75/100 puntos.

### 4.4 Mantenibilidad y Calidad de Código (RNF-04)
- Cobertura de pruebas unitarias e integración comprobada mediante JaCoCo mayor o igual al 60%.
