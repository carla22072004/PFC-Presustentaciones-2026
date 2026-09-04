# Centro de Orientación y Titulación

> Migraciones **V20** (tablas), **V21** (permisos + semilla de temas), **V22**
> (corrección del permiso `ORIENTACION_TEMAS_VER`) y **V23** (semilla de recursos).
> Extiende lo existente: reutiliza los catálogos de `carreras`,
> `lineas_investigacion` y `areas_tematicas` y el sistema de permisos dinámicos
> de V13 (`PermisoService.tienePermiso`).

## 1. Objetivo

Dar al estudiante un espacio para **explorar ideas de tema de titulación** antes
de registrar su solicitud, y **guardar** las que le interesen para retomarlas más
tarde. Coordinación y administración pueden consultar el mismo catálogo.

## 2. Modelo de datos (V20)

| Tabla | Uso |
|---|---|
| `presus.temas_propuestos` | Catálogo de ideas de tema (título, problema, objetivos, justificación, beneficiarios, nivel de dificultad) ligado a carrera / línea / área. |
| `presus.temas_guardados` | Temas que cada estudiante marcó (único por `estudiante_id + tema_propuesto_id`). |
| `presus.carrera_linea_investigacion` | Relación N:M configurable carrera ↔ línea de investigación. |
| `presus.recursos_titulacion` | Guías y recursos de titulación (`carrera_id` NULL = general). |
| `presus.progreso_estudiante` | Checklist de ruta de titulación por estudiante (`pasos_json` JSONB). |

Todas las tablas tienen trigger de auditoría genérica (`fn_auditoria_generica`, V15).

## 3. Permisos

| Código | id | Roles con el permiso | Qué habilita |
|---|---|---|---|
| `ORIENTACION_TEMAS_VER` | (V22, `MAX+1`) | ADMIN, COORDINADOR, DOCENTE, ESTUDIANTE | Explorar el catálogo y ver el detalle de un tema. |
| `ORIENTACION_CATALOGO_GESTIONAR` | 27 | ADMIN, COORDINADOR | Reservado para el futuro CRUD del catálogo de temas y recursos. |

Guardar / quitar / listar la lista personal **no** usa un permiso: es exclusivo del
rol `ESTUDIANTE` (`@PreAuthorize("hasRole('ESTUDIANTE')")`) y opera **siempre sobre
el estudiante autenticado** — el id se resuelve desde el JWT, nunca llega por la URL.

## 4. Endpoints (`TemaController`)

Base: `/api/v1/orientacion/temas`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/` | `ORIENTACION_TEMAS_VER` | Explorar el catálogo. Filtros opcionales: `carreraId`, `lineaInvestigacionId`, `areaId`, `nivelDificultad`. Si quien consulta es estudiante, cada tema trae `guardado: true/false`. |
| GET | `/{temaId}` | `ORIENTACION_TEMAS_VER` | Detalle de un tema. `404` si no existe. |
| POST | `/generar` | `ORIENTACION_TEMAS_VER` | Sugiere temas a partir de `carreraId` (obligatorio) y `lineaInvestigacionId` (opcional). |
| GET | `/guardados` | rol `ESTUDIANTE` | Lista de temas guardados del estudiante autenticado, más recientes primero. |
| POST | `/{temaId}/guardar` | rol `ESTUDIANTE` | Guarda el tema. `201` al crear, `409` si ya estaba guardado. |
| DELETE | `/{temaId}/guardar` | rol `ESTUDIANTE` | Quita el tema de la lista. `204` al quitar, `400` si no estaba. |

Errores estandarizados por `GlobalExceptionHandler`: `IllegalArgumentException` → 400,
`IllegalStateException` → **409** (nuevo handler), `AccessDeniedException` → 403.

### Recursos de titulación (`RecursoTitulacionController`)

Base: `/api/v1/orientacion/recursos`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/` | autenticado | Lista los recursos. Sin `carreraId`, un estudiante ve los generales + los de su carrera. |
| POST | `/` | `ORIENTACION_CATALOGO_GESTIONAR` | Crea un recurso (`titulo`, `categoria`, `urlArchivo`, `carreraId` opcional). |
| PUT | `/{id}` | `ORIENTACION_CATALOGO_GESTIONAR` | Actualiza un recurso. |
| DELETE | `/{id}` | `ORIENTACION_CATALOGO_GESTIONAR` | Elimina un recurso. |

### Ruta de titulación / progreso (`ProgresoTitulacionController`)

Base: `/api/v1/orientacion/progreso` — solo rol `ESTUDIANTE`, siempre sobre sí mismo.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Catálogo fijo de 8 pasos con el flag de completado de cada uno + `porcentaje`. |
| PUT | `/` | Body `{ "pasos": { "clave_paso": true/false } }`. Fusiona con lo guardado; ignora claves fuera del catálogo. |

El catálogo de pasos vive en `ProgresoTitulacionServiceImpl.CATALOGO` (no en BD): la
tabla solo guarda `{clave: bool}` en `pasos_json`.

## 5. Frontend

- Servicio: `services/orientacion.service.ts` (`/api/orientacion/temas`, el
  interceptor añade `/v1`).
- Componente: `components/orientacion/centro-orientacion.component.ts` →
  ruta `/dashboard/orientacion/centro` (`authGuard`).
- Cuatro pestañas: **Explorar temas** (filtros dependientes carrera → línea → área
  + nivel, tarjetas con detalle en modal), **Mis temas guardados** (solo estudiante),
  **Recursos de titulación** (agrupados por categoría, enlaces en pestaña nueva) y
  **Mi ruta de titulación** (checklist con barra de progreso, solo estudiante).
- Maneja estados de carga (spinner), error (con reintento) y vacío; los botones de
  guardar/quitar se deshabilitan mientras la petición está en curso; los mensajes
  de éxito/error usan `NotificationService` (SweetAlert2).
- Respeta el diseño existente (paleta `#1a5c2e`, clases `page-wrapper` /
  `page-header`, modo oscuro vía `body.dark-mode`).
- Entrada de menú "Centro de Orientación" para ESTUDIANTE, COORDINADOR y ADMIN.

## 6. Semilla (V21)

10 temas de ejemplo para la carrera **ISW** repartidos en las 4 líneas de la FCI
(V9) y la relación carrera↔línea correspondiente. Inserción idempotente
(`WHERE NOT EXISTS` por título); no toca datos existentes.

## 7. Pendiente

- Interfaz de administración para el CRUD de temas y recursos
  (`ORIENTACION_CATALOGO_GESTIONAR`); el backend ya existe para recursos.
- CRUD del catálogo de temas propuestos (hoy solo lectura + semilla).

## 8. Pruebas

- `TemaServiceImplTest` (10): filtros, marcado de guardados, detalle inexistente,
  guardar duplicado (409), quitar inexistente, mapeo de DTO.
- `TemaControllerTest` (7): resolución del estudiante desde el token, códigos de
  estado, rechazo cuando el usuario no tiene perfil de estudiante.
- `RecursoTitulacionServiceImplTest` (7): listar general vs por carrera, crear con
  y sin carrera, carrera/recurso inexistente, eliminar.
- `ProgresoTitulacionServiceImplTest` (5): estado vacío, cálculo de porcentaje,
  fusión de cambios, claves fuera de catálogo ignoradas, estudiante inexistente.
- `ProgresoTitulacionControllerTest` (2): el id del estudiante sale del token.
