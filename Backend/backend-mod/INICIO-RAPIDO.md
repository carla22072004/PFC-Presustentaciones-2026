# 🚀 GUÍA DE INICIO RÁPIDO

## Paso 1: Configurar Base de Datos PostgreSQL

### Opción A: Usando psql (línea de comandos)
```bash
# Crear base de datos
createdb -U postgres presus_db

# Ejecutar script de schema
psql -U postgres -d presus_db -f schema.sql

# Ejecutar datos iniciales (opcional, para testing)
psql -U postgres -d presus_db -f datos-iniciales.sql
```

### Opción B: Usando IntelliJ IDEA (Recomendado)
1. Abre IntelliJ IDEA
2. Ve a View → Tool Windows → Database
3. Añade conexión PostgreSQL (ver guía completa)
4. Ejecuta schema.sql desde Query Console
5. Ejecuta datos-iniciales.sql (opcional)

## Paso 2: Configurar el Proyecto

1. **Edita application.properties**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/presus_db
   spring.datasource.username=postgres
   spring.datasource.password=TU_PASSWORD
   ```

2. **Verifica que Java 17 esté instalado**
   ```bash
   java -version
   # Debe mostrar: java version "17" o superior
   ```

## Paso 3: Ejecutar el Proyecto

### Opción A: Desde IntelliJ IDEA (Recomendado)
1. Abre el proyecto en IntelliJ IDEA
2. Espera a que Maven descargue dependencias (barra de progreso abajo)
3. Busca la clase `PreSustentacionesApplication.java`
4. Haz clic derecho → Run 'PreSustentacionesApplication'
5. O simplemente presiona el botón verde ▶️

### Opción B: Desde línea de comandos
```bash
# Compilar
mvn clean install

# Ejecutar
mvn spring-boot:run
```

## Paso 4: Verificar que Todo Funciona

1. **Espera el mensaje de inicio:**
   ```
   ✅ Sistema de Pre-Sustentaciones UTEQ iniciado correctamente
   📚 Swagger UI: http://localhost:8080/api/swagger-ui.html
   ```

2. **Abre Swagger UI en tu navegador:**
   http://localhost:8080/api/swagger-ui.html

3. **Prueba el endpoint de usuarios:**
   - Expande "Usuarios" → GET /api/usuarios
   - Haz clic en "Try it out"
   - Haz clic en "Execute"
   - Deberías ver la lista de usuarios si ejecutaste datos-iniciales.sql

## 🎯 Endpoints de Prueba

### Listar todos los usuarios
```http
GET http://localhost:8080/api/usuarios
```

### Crear un nuevo usuario
```http
POST http://localhost:8080/api/usuarios
Content-Type: application/json

{
  "cedula": "9999888777",
  "nombres": "Nuevo",
  "apellidos": "Usuario",
  "email": "nuevo@uteq.edu.ec",
  "rol": "ESTUDIANTE",
  "activo": true
}
```

### Listar usuarios por rol
```http
GET http://localhost:8080/api/usuarios/rol/ESTUDIANTE
```

### Buscar usuario por email
```http
GET http://localhost:8080/api/usuarios/email/jperez@uteq.edu.ec
```

## ⚠️ Solución de Problemas Comunes

### Error: "Connection refused"
- Verifica que PostgreSQL esté ejecutándose
- Confirma el puerto (5432 por defecto)
- Revisa usuario y contraseña en application.properties

### Error: "Schema presus does not exist"
- Ejecuta el script schema.sql primero
- Verifica que se creó el schema: `\dn` en psql

### Error: "Port 8080 already in use"
- Cambia el puerto en application.properties:
  ```properties
  server.port=8081
  ```

### Error: Maven no descarga dependencias
- Verifica tu conexión a Internet
- Ejecuta: `mvn clean install -U` (fuerza actualización)
- Si estás detrás de un proxy, configura Maven

### Error: "lombok requires enabled annotation processing"
- En IntelliJ: Settings → Build, Execution, Deployment → Compiler → Annotation Processors
- Marca "Enable annotation processing"

## 📚 Próximos Pasos

1. ✅ Explora la documentación Swagger
2. ✅ Revisa las entidades en el package `entities`
3. ✅ Examina los controladores REST
4. ✅ Implementa nuevos endpoints según tus necesidades
5. ✅ Añade validaciones con Bean Validation
6. ✅ Implementa DTOs para separar la capa de persistencia

## 🆘 ¿Necesitas Ayuda?

- Revisa el archivo README.md para documentación completa
- Consulta la guía de IntelliJ IDEA proporcionada
- Revisa los logs en la consola para más detalles de errores

¡Listo para empezar a desarrollar! 🎉
