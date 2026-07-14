# Sistema de Gestión de Pre-Sustentaciones UTEQ

Sistema integral para la gestión de pre-sustentaciones de trabajos de titulación en la Universidad Técnica Estatal de Quevedo.

## 🚀 Tecnologías

- **Java 17**
- **Spring Boot 3.2.1**
- **PostgreSQL 12+**
- **Maven**
- **Lombok**
- **Swagger/OpenAPI**

## 📋 Requisitos Previos

1. **Java JDK 17** o superior
2. **PostgreSQL 12** o superior
3. **Maven 3.6** o superior
4. **IntelliJ IDEA** (recomendado) o cualquier IDE compatible

## ⚙️ Configuración

### 1. Base de Datos

Ejecuta el script SQL proporcionado en PostgreSQL para crear el schema `presus` y todas las tablas necesarias.

```bash
psql -U postgres -d presus_db -f schema.sql
```

### 2. Configuración de Aplicación

Edita `src/main/resources/application.properties` con tus credenciales de base de datos:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/presus_db
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
```

## 🏗️ Instalación y Ejecución

### Usando Maven

```bash
# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

### Usando IntelliJ IDEA

1. Abre el proyecto (File → Open → selecciona la carpeta del proyecto)
2. Espera a que Maven descargue las dependencias
3. Ejecuta la clase `PreSustentacionesApplication` (botón verde ▶️)

La aplicación estará disponible en: `http://localhost:8080/api`

## 📚 Documentación API

Una vez iniciada la aplicación, accede a:

- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/api/api-docs

## 🔗 Endpoints Principales

### Usuarios
- `GET /api/usuarios` - Listar todos los usuarios
- `GET /api/usuarios/{id}` - Obtener usuario por ID
- `POST /api/usuarios` - Crear nuevo usuario
- `PUT /api/usuarios/{id}` - Actualizar usuario
- `DELETE /api/usuarios/{id}` - Eliminar usuario

### Estudiantes
- `GET /api/estudiantes` - Listar todos los estudiantes
- `GET /api/estudiantes/{id}` - Obtener estudiante por ID
- `POST /api/estudiantes` - Crear nuevo estudiante

### Solicitudes
- `GET /api/solicitudes` - Listar todas las solicitudes
- `GET /api/solicitudes/estado/{estado}` - Filtrar por estado
- `POST /api/solicitudes` - Crear nueva solicitud
- `PATCH /api/solicitudes/{id}/estado` - Cambiar estado

## 📁 Estructura del Proyecto

```
src/main/java/ec/edu/uteq/presustentaciones/
├── config/              # Configuraciones
├── controllers/         # Controladores REST
├── dto/                 # Objetos de transferencia de datos
├── entities/            # Entidades JPA
├── enums/               # Enumeraciones
├── exceptions/          # Manejo de excepciones
├── repositories/        # Repositorios JPA
└── services/            # Lógica de negocio
```

## 🧪 Testing

```bash
# Ejecutar tests
mvn test
```

## 🔐 Seguridad

**IMPORTANTE**: Antes de desplegar en producción:

1. Cambia las credenciales de la base de datos
2. Usa variables de entorno para configuración sensible
3. Implementa Spring Security (próxima versión)
4. Habilita HTTPS

## 📝 Estado del Proyecto

**Versión**: 1.0.0-SNAPSHOT

**Implementado**:
- ✅ Gestión de Usuarios
- ✅ Gestión de Estudiantes
- ✅ Gestión de Docentes
- ✅ Gestión de Solicitudes
- ✅ Gestión de Salas
- ✅ Gestión de Cronogramas
- ✅ API REST completa
- ✅ Documentación Swagger

**Pendiente**:
- ⏳ Tribunal y asignaciones
- ⏳ Sistema de evaluación
- ⏳ Generación de actas
- ⏳ Notificaciones
- ⏳ Autenticación y autorización
- ⏳ Tests unitarios e integración

## 👥 Equipo de Desarrollo

- Castro Coello Mario Sebastián
- Benites Perez Dariem Alberto
- Torrales Avilés Mark Yair

## 📄 Licencia

Universidad Técnica Estatal de Quevedo - Todos los derechos reservados
