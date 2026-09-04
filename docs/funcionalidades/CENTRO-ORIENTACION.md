# Centro de Orientación y Titulación

> Migraciones **V20** (tablas), **V21** (permisos + semilla de temas) y **V22**
> (corrección del permiso `ORIENTACION_TEMAS_VER`).
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
| `presus.recursos_titulacion` | Guías y recursos de titulación (backend pendiente). |
| `presus.progreso_estudiante` | Checklist de ruta de titulación por estudiante (backend pendiente). |

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

## 5. Frontend

- Servicio: `services/orientacion.service.ts` (`/api/orientacion/temas`, el
  interceptor añade `/v1`).
- Componente: `components/orientacion/centro-orientacion.component.ts` →
  ruta `/dashboard/orientacion/centro` (`authGuard`).
- Dos pestañas: **Explorar temas** (filtros dependientes carrera → línea → área +
  nivel, tarjetas con detalle en modal) y **Mis temas guardados** (solo estudiante).
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

- Backend + frontend de `recursos_titulacion` (guías de titulación).
- Backend + frontend de `progreso_estudiante` (checklist de ruta de titulación).
- CRUD del catálogo de temas para `ORIENTACION_CATALOGO_GESTIONAR`.

## 8. Pruebas

- `TemaServiceImplTest` (10 casos): filtros, marcado de guardados, detalle
  inexistente, guardar duplicado (409), quitar inexistente, mapeo de DTO.
- `TemaControllerTest` (7 casos): resolución del estudiante desde el token,
  códigos de estado, rechazo cuando el usuario no tiene perfil de estudiante.
