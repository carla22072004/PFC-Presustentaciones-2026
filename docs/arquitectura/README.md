# 🏛️ ARQUITECTURA DEL SISTEMA (MODELO C4)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Estándar:** Modelo C4 (Simon Brown) - Niveles 1, 2 y 3  

---

## 📌 Nivel 1: Diagrama de Contexto del Sistema (System Context)

El siguiente diagrama ilustra cómo el **Sistema de Pre-Sustentaciones UTEQ** interactúa con las diferentes personas (roles) y sistemas externos.

```mermaid
graph TD
    Estudiante["👤 Estudiante Egresado<br/>(Usuario Principal)"]
    Docente["👤 Docente / Jurado / Presidente<br/>(Evaluador)"]
    Coordinador["👤 Coordinador de Titulación<br/>(Administrador Académico)"]

    Sistema["🏛️ Sistema de Gestión de Pre-Sustentaciones UTEQ<br/>[Sistema de Software]"]
    MailService["✉️ Servicio de Correo SMTP UTEQ<br/>[Sistema Externo]"]

    Estudiante -->|"Registra solicitudes y sube anteproyecto PDF"| Sistema
    Docente -->|"Evalúa rúbricas y firma actas digitales"| Sistema
    Coordinador -->|"Asigna jurados masivos y programa salas"| Sistema
    Sistema -->|"Envía notificaciones de agendamiento y actas"| MailService
```

---

## 📌 Nivel 2: Diagrama de Contenedores (Containers)

El diagrama de contenedores describe las aplicaciones de alto nivel y almacenes de datos que conforman el sistema.

```mermaid
graph TD
    UserBrowser["🌐 Navegador Web / Cliente<br/>(HTML5 / CSS3 / JS)"]

    subgraph "Infraestructura de Aplicación UTEQ"
        Frontend["🎨 Frontend SPA Angular 17<br/>[Contenedor Web / Nginx]<br/>Puerto: 4200"]
        Backend["⚙️ API REST Spring Boot 3.2.1<br/>[Contenedor Java 17 / JVM]<br/>Puerto: 8080"]
        PostgreSQL["🗄️ Base de Datos Relacional PostgreSQL 15<br/>[Contenedor BD]<br/>Puerto: 5432"]
        Redis["⚡ Almacén de Caché Redis 7<br/>[Contenedor In-Memory]<br/>Puerto: 6379"]
    end

    UserBrowser -->|"HTTPS / REST API JSON / Cookies HTTP-Only"| Frontend
    Frontend -->|"Peticiones HTTP REST / Header Bearer JWT"| Backend
    Backend -->|"Spring Data JPA + Procedimientos PL/pgSQL"| PostgreSQL
    Backend -->|"Sesiones y Caché de Rúbricas"| Redis
```

---

## 📌 Nivel 3: Diagrama de Componentes del Backend (Components)

El diagrama de componentes detalla la estructura interna del contenedor Backend (`presustentaciones.jar`).

```mermaid
graph TD
    subgraph "Contenedor Backend Spring Boot"
        SecurityFilter["🔐 JwtAuthenticationFilter<br/>(Validación Token y Cookies)"]
        AuthController["🎮 AuthController<br/>(Login, Registro, Refresh)"]
        SolicitudController["🎮 SolicitudController<br/>(CRUD Solicitudes)"]
        EvaluacionController["🎮 EvaluacionController<br/>(Cálculo Rúbricas)"]
        ActaController["🎮 ActaController<br/>(Firmas y PDF)"]

        SolicitudService["🛠️ SolicitudServiceImpl<br/>(Lógica de Negocio Solicitudes)"]
        EvaluacionService["🛠️ EvaluacionServiceImpl<br/>(Lógica de Negocio Evaluaciones)"]
        ActaService["🛠️ ActaServiceImpl<br/>(Lógica de Negocio Actas)"]

        JPARepositories["📦 Spring Data JPA Repositories<br/>(CRUD Elemental)"]
        StoredProcedures["🗄️ PL/pgSQL Stored Procedures<br/>(sp_calcular_promedio, sp_generar_reporte)"]
    end

    SecurityFilter --> AuthController
    SecurityFilter --> SolicitudController
    SecurityFilter --> EvaluacionController
    SecurityFilter --> ActaController

    SolicitudController --> SolicitudService
    EvaluacionController --> EvaluacionService
    ActaController --> ActaService

    SolicitudService --> JPARepositories
    EvaluacionService --> StoredProcedures
    ActaService --> StoredProcedures
```
