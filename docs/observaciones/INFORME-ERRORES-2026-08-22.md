# 🐛 Informe de Errores — Pruebas en Vivo (2026-08-22)

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Origen:** Pruebas manuales del dueño del proyecto contra el backend (`localhost:8080`) y frontend (`localhost:4200`) corriendo en local, conectados a la base de datos real de desarrollo (1,010,242 registros), más una auditoría de código dirigida por cada punto reportado.
**Metodología:** Cada hallazgo fue verificado leyendo el código real (archivo:línea citado), no supuesto. Donde la causa raíz no pudo confirmarse con certeza total, se marca explícitamente como "hipótesis más probable" en vez de presentarse como hecho.
**Alcance de este informe:** Solo diagnóstico. Ninguno de los hallazgos de este documento fue corregido todavía (a diferencia de los cambios de roles de BD, índices y el botón "Tribunal" en `ENVIADA`, ya aplicados en sesiones anteriores del mismo día).
**Cómo usar los "Prompt para corregir":** cada error trae, al final de su sección, un prompt autocontenido — se le puede pegar directamente a Claude Code (o a cualquier desarrollador) en una sesión nueva para que aplique esa corrección puntual, sin necesitar el resto de este documento como contexto.

---

## Matriz resumen

| ID | Área | Problema reportado | Causa raíz confirmada | Prioridad sugerida |
|---|---|---|---|---|
| **ERR-01** | Transversal | Los filtros de solicitudes son pestañas por estado (Todas/Por revisar/...), no hay buscador de texto libre en ningún listado del sistema | No es un bug — es una funcionalidad que nunca se construyó | Media (mejora de usabilidad) |
| **ERR-02** | Transversal | Los combobox (`<select>`) de docentes se traban al abrir | `GET /api/docentes` trae las **9,807 filas completas** sin paginar; se renderizan como 9,807 `<option>` en 2 selects del tribunal | 🔴 Alta |
| **ERR-03** | Coordinador — Tribunal | "Muchos errores de validación" en asignación manual y automática | El frontend usa roles `VOCAL_1`/`VOCAL_2`; el backend solo acepta `VOCAL`/`SECRETARIO`/`PRESIDENTE` → **toda asignación manual de Vocal 1 o Vocal 2 falla siempre** | 🔴 Alta (bloquea el flujo central de la demo) |
| **ERR-04** | Estudiante/Docente — Anteproyecto | "Ver PDF en pantalla" no hace nada; "Descargar PDF" da error al abrir | El componente usa `fetch()` crudo a una URL sin `/v1`, que el backend ya no expone (todo vive bajo `/api/v1/**`) → 404/401 confundido con el PDF | 🔴 Alta |
| **ERR-05** | Coordinador — Reportes | "Cronograma PDF" y "Estadísticas PDF" salen casi vacíos ("Total: 0") | El backend filtra cronogramas por estado `"ACTIVO"`, código que **no existe** en la BD real (el código real es `"PROGRAMADO"`) | 🔴 Alta |
| **ERR-06** | Seguridad transversal | (no reportado por ti, encontrado en la auditoría) 6 controllers backend sin `@PreAuthorize` en operaciones de escritura | Cualquier usuario autenticado (incluido un ESTUDIANTE) puede borrar rúbricas, firmar actas, crear/borrar cronogramas, asignar tutores, borrar salas o calificar jurados de otras solicitudes | 🔴 Alta (seguridad) |
| **ERR-07** | Estudiante/Docente/Coordinador | Varios `catch`/`subscribe` vacíos que ocultan errores al usuario | Ver detalle por rol más abajo | 🟡 Media |
| **ERR-08** | Estudiante — Anteproyecto | (encontrado al revisar cómo se guardan los PDF) subir/reemplazar el PDF de anteproyecto no valida dueño ni rol | `enviar/{solicitudId}` en `AnteproyectoController.java` no tiene `@PreAuthorize` ni verifica que el usuario autenticado sea el estudiante dueño de esa solicitud | 🔴 Alta (seguridad) |

---

## ERR-01 — Falta buscador de texto libre (filtros por pestaña únicamente)

**Lo que pediste:** cambiar el esquema de pestañas (Todas, Por revisar, Aprobadas, etc.) por una búsqueda real, aplicada "en todo el proyecto en general".

**Diagnóstico:** esto no es un error de código — es correcto que las pestañas filtren por `estado`, pero **no existe en ningún listado del sistema** (revisar-solicitudes, mis-asignaciones, gestión de usuarios, etc.) un campo de texto que busque por nombre de estudiante, título del tema, cédula, etc. Es una funcionalidad que nunca se implementó, no una regresión.

**Qué se necesitaría:** un input de búsqueda por pantalla + un endpoint de backend que soporte filtrado por texto (`LIKE`/`ILIKE` sobre nombre/apellido/título) combinado con el filtro de estado existente — hoy los listados solo aceptan el filtro de estado. Es un cambio de alcance mediano: toca el frontend (input + lógica de filtrado) y el backend (parámetro de búsqueda en los repositorios paginados) de cada pantalla con listado. Recomiendo tratarlo como una tarea aparte, no como "bug urgente" antes de la sustentación.

> ### 🔧 Prompt para corregir ERR-01
> ```
> En el proyecto PFC-Presustentaciones-2026, agrega un buscador de texto libre a la
> pantalla del coordinador Frontend/src/app/components/admin/revisar-solicitudes/
> (revisar-solicitudes.component.ts/.html), que hoy solo filtra por pestañas de estado
> (Todas, Por revisar, Aprobadas, etc.).
>
> 1. Agrega un <input> de búsqueda arriba de la tabla, con un debounce de ~300ms,
>    que filtre por nombre/apellido del estudiante o título del tema (s.tituloTema,
>    s.estudiante.usuario.nombre/apellido).
> 2. El filtro de texto debe combinarse con el filtro de pestaña activo (estado),
>    no reemplazarlo — ambos deben aplicar a la vez.
> 3. Si la lista ya viene paginada desde el backend (revisa el servicio de
>    solicitudes y si existe un endpoint /api/v1/solicitudes/paginado), agrega un
>    parámetro de búsqueda (`q` o `texto`) a ese endpoint y al repositorio
>    (JPQL con LOWER(...) LIKE LOWER(CONCAT('%', :q, '%')) o Specification/
>    Criteria API) en vez de filtrar solo en el frontend, para que funcione también
>    sobre datos no cargados en la página actual.
> 4. Si por ahora el filtrado es 100% en el cliente (array ya cargado), déjalo así
>    pero avísame explícitamente que es una solución client-side y no escala a los
>    volúmenes reales de la tabla `solicitud` (44,000+ filas).
> 5. No toques el resto de las pantallas todavía — solo esta, como referencia
>    reutilizable para las demás después.
> ```

---

## ERR-02 — Combobox de docentes se traba (9,807 opciones sin paginar)

**Dónde:** pantalla "Asignar Tribunal" del coordinador — `Frontend/src/app/components/admin/asignar-jurados/asignar-jurados.component.ts` y su gemelo casi duplicado `asignar-tribunal.component.ts` (2 selects cada uno: jurado y tutor).

**Causa raíz confirmada:**
- Ambos componentes llaman `DocenteService.listar()` → `GET /api/docentes` (sin parámetros).
- En el backend, ese endpoint (`DocenteController.java`) hace `docenteRepository.findAll()` — **trae las 9,807 filas de la tabla completa**, sin `Pageable`.
- El HTML renderiza un `<option>` por cada docente → ~9,807 nodos DOM por select, dos veces por pantalla. Con eso el navegador se congela un momento al abrir el desplegable, exactamente como reportaste.
- Ya existe un endpoint paginado hermano y sin usar: `GET /api/docentes/paginado` (con `Pageable`), preparado siguiendo la misma convención que `/api/v1/solicitudes/paginado` — pero ningún componente lo llama todavía.
- **No hay ninguna librería de combobox con búsqueda** instalada en el proyecto (`Frontend/package.json` no tiene `ng-select`, `primeng`, Angular Material ni similar) — solo `<select>` nativo de HTML en todo el frontend. Cualquier solución necesita agregar una librería o construir un combobox filtrado a mano.

**Recomendación:** cambiar `asignar-jurados`/`asignar-tribunal` para usar `/api/docentes/paginado` combinado con un combobox de búsqueda (typeahead) en vez del `<select>` nativo — esto resuelve tanto el congelamiento como el pedido de "que no aparezcan todas las opciones de golpe, pero que se pueda buscar".

> ### 🔧 Prompt para corregir ERR-02
> ```
> En el proyecto PFC-Presustentaciones-2026, el frontend Angular usa <select> nativos
> para elegir un docente en Frontend/src/app/components/admin/asignar-jurados/
> asignar-jurados.component.ts (y su duplicado asignar-tribunal.component.ts),
> alimentados por DocenteService.listar() -> GET /api/docentes, que trae las 9,807
> filas de la tabla docente sin paginar. Renderizar ~9,807 <option> congela el
> navegador al abrir el desplegable.
>
> 1. NO agregues ninguna librería externa nueva sin confirmar conmigo antes
>    (Frontend/package.json no tiene ng-select/primeng/Angular Material hoy).
>    Construye un combobox propio simple: un <input> de texto + una lista
>    desplegable filtrada que se muestra solo mientras se escribe, mostrando
>    máximo ~20 resultados a la vez.
> 2. Cambia la fuente de datos: en vez de traer todos los docentes de una vez,
>    usa el endpoint ya existente GET /api/docentes/paginado (o agrega un
>    parámetro de búsqueda `nombre` a ese endpoint si no lo tiene) y dispara la
>    consulta al backend con debounce (~300ms) cada vez que el usuario escribe,
>    en vez de cargar las 9,807 filas al abrir la pantalla.
> 3. Aplica el mismo cambio en los 2 selects de asignar-jurados.component (jurado
>    y tutor) y en los 2 selects equivalentes de asignar-tribunal.component
>    -- revisa primero si asignar-tribunal.component es código muerto/duplicado
>    de asignar-jurados antes de arreglar los dos; si nadie lo usa, dime y no lo
>    toques.
> 4. Mantén el mismo (value) que usa hoy el formulario (docenteId) para no romper
>    la llamada a asignarJurado()/asignarTutor().
> ```

---

## ERR-03 — Validación de asignación de Tribunal (el hallazgo más importante)

Este es probablemente **el bug más grave de los reportados**, porque afecta directamente el flujo que armamos hoy mismo para la demo.

**Causa raíz — vocabulario de roles no coincide entre frontend y backend:**
- El formulario manual (`asignar-jurados.component.ts:36-40`, constante `ROLES`) solo ofrece `PRESIDENTE`, `VOCAL_1`, `VOCAL_2`.
- El backend (`JuradoServiceImpl.java:62-65`) valida contra una lista **distinta**: `PRESIDENTE`, `VOCAL`, `SECRETARIO`.
- Como `VOCAL_1`/`VOCAL_2` nunca están en la lista válida del backend, **toda asignación manual de Vocal 1 o Vocal 2 falla siempre** con "Rol inválido". Solo Presidente puede asignarse manualmente con éxito.
- La asignación automática (`JuradoServiceImpl.java:203-204`) usa el modelo de 3 roles (`PRESIDENTE`/`VOCAL`/`SECRETARIO`) — es decir, aunque "funciona" sin error, arma un tribunal con roles distintos a los que el resto de la pantalla espera mostrar.
- Aun si el rol pasara la validación, `crearJuradoSinNotificar()` (línea 271-274) convierte cualquier rol que empiece con `"VOCAL"` al código genérico `"VOCAL"` antes de guardar — así que en la base de datos **nunca queda guardado** `VOCAL_1` ni `VOCAL_2` literalmente, sin importar el camino de asignación.

**Efecto en la pantalla de Evaluar (confirmado):** `evaluar-solicitud.component.ts` (funciones `getNombreJuradoPorRol()`, `firmaEstado()`, `formatearRol()`) busca específicamente `'VOCAL_1'`/`'VOCAL_2'` para mostrar el nombre del jurado y el estado de firma del acta. Como esos códigos nunca se guardan tal cual, **los dos Vocales del tribunal nunca se muestran ni se pueden firmar correctamente en la pantalla de evaluación** — siempre sale `—`.

**Otras validaciones que faltan (confirmado, no hay ninguna verificación de esto):**
- No se valida que el **tutor asignado no sea también miembro del tribunal** de la misma solicitud (conflicto de interés real).
- No se valida `docente.disponible` ni en la asignación manual ni en el fallback de la automática.
- El chequeo de "rol duplicado" compara el string crudo (`VOCAL_1`) contra el código ya colapsado (`VOCAL`) guardado en BD — nunca coinciden, así que ese chequeo tampoco funciona de forma confiable.

**Sobre el botón "Guardar" que preguntaste:** hoy **no existe ningún paso de revisión antes de guardar** — cada acción (asignar jurado, asignación automática, eliminar jurado, asignar tutor) llama al backend y se guarda de inmediato, sin una etapa intermedia de "revisar y confirmar". Agregar un botón "Guardar" no arreglaría el problema de fondo (el desajuste de roles), pero sí sería una mejora de UX razonable una vez corregido el vocabulario de roles.

**Detalle menor:** `ngOnInit` de `asignar-jurados.component.ts` (línea 65) fuerza `cargando = false` con un timeout fijo de 10 segundos sin importar si los datos realmente cargaron — es un parche que sugiere que las cargas a veces son lentas o se cuelgan.

> ### 🔧 Prompt para corregir ERR-03
> ```
> En el proyecto PFC-Presustentaciones-2026 hay un desajuste de vocabulario de roles
> de tribunal entre frontend y backend que hace fallar SIEMPRE la asignación manual
> de Vocal 1 y Vocal 2, y que impide que se muestren o firmen correctamente en la
> pantalla de Evaluar.
>
> Archivos involucrados:
> - Frontend/src/app/components/admin/asignar-jurados/asignar-jurados.component.ts
>   (constante ROLES, líneas 36-40: usa 'PRESIDENTE', 'VOCAL_1', 'VOCAL_2')
> - backend/src/main/java/ec/edu/uteq/presustentaciones/services/JuradoServiceImpl.java
>   (rolesValidos líneas 62-65: usa 'PRESIDENTE', 'VOCAL', 'SECRETARIO';
>    asignarJuradosAutomaticamente líneas 203-204: mismo modelo de 3 roles;
>    crearJuradoSinNotificar líneas 271-274: colapsa cualquier rol que empiece
>    con 'VOCAL' al código genérico 'VOCAL' antes de guardar)
> - Frontend/src/app/components/admin/evaluar-solicitud/evaluar-solicitud.component.ts
>   (getNombreJuradoPorRol, firmaEstado, formatearRol/ROLES_FIRMA: buscan
>    literalmente 'VOCAL_1'/'VOCAL_2')
>
> 1. Decide y aplica UN solo vocabulario de roles de tribunal en todo el sistema
>    (recomiendo el de 2 vocales, 'PRESIDENTE'/'VOCAL_1'/'VOCAL_2', porque es el
>    que ya usa la pantalla de Evaluar, que es más central) y propágalo:
>    - rolesValidos en JuradoServiceImpl.java debe aceptar exactamente esos 3 códigos
>    - crearJuradoSinNotificar() NO debe colapsar VOCAL_1/VOCAL_2 a 'VOCAL' -- debe
>      guardar el código tal cual llega
>    - asignarJuradosAutomaticamente() debe generar tribunales con esos mismos
>      3 roles, no con 'SECRETARIO'
>    - revisa si existe algún dato ya guardado en la tabla roles_jurado o
>      miembros_tribunal con el código viejo 'SECRETARIO'/'VOCAL' colapsado y
>      dime qué encontraste antes de decidir si hace falta una migración de datos
> 2. Corrige el chequeo de "rol duplicado" (línea ~67-71 de JuradoServiceImpl.java)
>    para comparar el código YA NORMALIZADO contra lo guardado, no el string crudo
>    de entrada contra el código colapsado.
> 3. Agrega dos validaciones nuevas que hoy no existen en asignarJurado() y en el
>    fallback de asignación automática:
>    a) Rechazar si el docente a asignar como jurado es el mismo que ya está
>       asignado como tutor de esa misma solicitud (conflicto de interés).
>    b) Rechazar (o al menos advertir) si el docente tiene disponible = false.
> 4. NO agregues todavía un botón "Guardar"/paso de revisión -- eso lo pediré
>    aparte una vez esto esté corregido, para no mezclar cambios.
> 5. Después de corregir, verifica manualmente contra la base de datos real
>    (Docker container amz-postgres, BD BdPresustentaciones) que se puede asignar
>    Presidente + Vocal 1 + Vocal 2 a una solicitud real sin error, y que la
>    pantalla de Evaluar los muestra a los tres correctamente.
> ```

---

## ERR-04 — "Ver PDF en pantalla" no hace nada / "Descargar PDF" da error al abrir

**Causa raíz confirmada:** `ver-anteproyecto.component.ts` (funciones `abrirPdf()` y `descargarPdf()`, líneas ~60-92) llaman al backend usando `fetch()` nativo del navegador, **no** el `HttpClient` de Angular, apuntando a `http://localhost:8080/api/anteproyectos/...` (sin `/v1`).

El interceptor de autenticación (`auth.interceptor.ts`) reescribe automáticamente `/api/` → `/api/v1/` en cada petición — pero **solo intercepta peticiones hechas con `HttpClient`**. Como `fetch()` no pasa por ahí, la URL sin `/v1` llega tal cual al backend. Y en el backend, `CustomWebMvcRegistrations.java` obliga a que **todos** los controllers vivan bajo `/api/v1/**` — la ruta vieja sin `/v1` ya no tiene ningún handler que la atienda, así que responde 404/401.

- **"Ver PDF en pantalla" no hace nada:** `abrirPdf()` recibe ese error, lo captura en el `catch`, y solo cambia una variable de estado a "error" — visualmente parece que no pasó nada.
- **"Descargar PDF" descarga algo que da error al abrir:** `descargarPdf()` **no valida si la respuesta fue exitosa** (`res.ok`) antes de guardar el archivo — toma el cuerpo de la respuesta de error (JSON/HTML de Spring) y lo guarda con extensión `.pdf`. El archivo "se descarga" pero no es un PDF real, por eso el visor de PDF tira error al abrirlo.

**Nota:** el resto de la pantalla (los datos de la solicitud) sí carga bien porque esa llamada específica usa `HttpClient` y sí pasa por la reescritura a `/v1`. Solo las dos acciones de PDF usan `fetch()` crudo.

**Contexto adicional (cómo se guarda el PDF, confirmado leyendo el código):** el archivo NO se guarda en la base de datos como binario. Se guarda en disco (`uploads/anteproyectos/`, configurable con `app.upload.dir`), y la tabla `anteproyectos` solo guarda el nombre del archivo (`archivo_pdf varchar(255)`), su tamaño (`tamano_bytes`) y un hash `sha256_hash` calculado al subirlo, usado para verificar integridad después. El endpoint que sirve el PDF (`GET /api/anteproyectos/ver/{solicitudId}`) está bien construido — responde con `Content-Type: application/pdf` y `Content-Disposition: inline`. El bug está solo en cómo el frontend lo llama, no en cómo el backend lo guarda o lo sirve.

> ### 🔧 Prompt para corregir ERR-04
> ```
> En el proyecto PFC-Presustentaciones-2026, el componente Frontend/src/app/
> components/docente/ver-anteproyecto/ver-anteproyecto.component.ts tiene dos
> funciones, abrirPdf() y descargarPdf() (líneas ~60-92), que usan fetch() nativo
> del navegador apuntando a http://localhost:8080/api/anteproyectos/... (sin /v1).
> El interceptor Frontend/src/app/interceptors/auth.interceptor.ts reescribe
> /api/ -> /api/v1/ SOLO para peticiones hechas con HttpClient de Angular, así que
> estas dos llamadas con fetch() nunca pasan por esa reescritura y le pegan a una
> ruta que el backend ya no expone (todo vive bajo /api/v1/** por
> CustomWebMvcRegistrations.java) -- por eso "Ver PDF en pantalla" no hace nada y
> "Descargar PDF" descarga un archivo de error disfrazado de .pdf.
>
> 1. Reemplaza los dos fetch() por HttpClient de Angular (inyecta HttpClient en el
>    componente o, mejor, muévelas al servicio Frontend/src/app/services/
>    anteproyecto.service.ts si ese servicio ya existe), usando
>    { observe: 'response', responseType: 'blob' } para poder leer el PDF como
>    blob y también los headers/código de estado.
> 2. Para "Ver PDF en pantalla": arma una URL de blob (URL.createObjectURL) con
>    tipo application/pdf y ábrela en una nueva pestaña o en un <iframe>/<embed>
>    dentro de la misma pantalla -- lo que ya intente hacer hoy el HTML existente,
>    solo corrigiendo el origen de los bytes.
> 3. Para "Descargar PDF": antes de disparar la descarga, verifica que la respuesta
>    haya sido exitosa (status 200) -- si no, muestra un mensaje de error claro al
>    usuario en vez de descargar el cuerpo del error como si fuera el PDF.
> 4. Confirma que ambas llamadas queden pasando por el interceptor (o arma tú
>    mismo la URL con /api/v1/ si decides no usar HttpClient) y pruébalo contra
>    un anteproyecto real ya subido en la base de datos de desarrollo.
> 5. No toques el backend (AnteproyectoController.java, endpoint /ver/{solicitudId})
>    -- ya está bien implementado, el problema es 100% del lado del frontend.
> ```

---

## ERR-05 — "Cronograma PDF" y "Estadísticas PDF" salen casi vacíos

**Cronograma PDF — causa raíz confirmada:** `ReporteController.java` (línea ~57-58) filtra los cronogramas así:
```java
List<Cronograma> lista = cronogramaRepo.findAll().stream()
    .filter(c -> c.getEstado() != null && "ACTIVO".equalsIgnoreCase(c.getEstado().getCodigo()))
```
El código `"ACTIVO"` **no existe** en la tabla `estados_cronograma` de la base de datos real — el código que de verdad se usa al programar una pre-sustentación es `"PROGRAMADO"` (confirmado en `CronogramaServiceImpl.java`, donde se busca explícitamente `findByCodigo("PROGRAMADO")` al crear cada cronograma). Como ninguna de las 24,201 filas reales tiene el código `"ACTIVO"`, el filtro descarta absolutamente todo, y el PDF sale con "Total: 0" — exactamente lo que viste.

Esto también explica la lentitud: antes de aplicar el filtro (que termina descartando todo), el backend ya trajo las 24,201 filas completas con 4 relaciones en modo `EAGER` (solicitud, convocatoria, sala, bloque), sin `JOIN FETCH` — es decir, decenas de miles de consultas SQL individuales (N+1) solo para terminar con una lista vacía.

**Estadísticas PDF — hipótesis más probable (no pude confirmarla con la misma certeza):** no encontré un filtro roto tan claro como el de Cronograma — los códigos de resultado (`APROBADO`/`REPROBADO`) que usa sí coinciden con los que asigna el código real. Lo que sí confirmé es el mismo patrón de relaciones `EAGER` sin `JOIN FETCH` sobre las 15,401 filas de `evaluaciones_finales`, con un bucle que además navega `evaluacion.getSolicitud().getEstudiante().getUsuario()` fila por fila — sospecho que el reporte se vuelve tan lento/pesado que sale truncado o corrupto antes de terminar, pero confirmarlo requeriría probarlo en vivo contra los datos reales, algo que quedó fuera del alcance de esta revisión de solo lectura del código.

> ### 🔧 Prompt para corregir ERR-05
> ```
> En el proyecto PFC-Presustentaciones-2026, backend/src/main/java/ec/edu/uteq/
> presustentaciones/controllers/ReporteController.java tiene, alrededor de la
> línea 57-58, un filtro que descarta TODOS los cronogramas reales al generar el
> "Cronograma PDF":
>
>     .filter(c -> c.getEstado() != null && "ACTIVO".equalsIgnoreCase(c.getEstado().getCodigo()))
>
> El código correcto (el que de verdad se asigna al programar una pre-sustentación,
> confirmado en CronogramaServiceImpl.java vía
> estadoCronogramaRepository.findByCodigo("PROGRAMADO")) es "PROGRAMADO", no
> "ACTIVO". Ese único código mal escrito hace que el reporte salga con
> "Total: 0 pre-sustentación(es) programadas" aunque existan miles de filas reales.
>
> 1. Cambia "ACTIVO" por "PROGRAMADO" en ese filtro. Antes de asumir que ese es el
>    único código relevante, revisa la tabla estados_cronograma completa (hay
>    también códigos para canceladas/reprogramadas, por ejemplo) y decide si el
>    reporte de cronograma debe incluir solo "PROGRAMADO" o también otros estados
>    -- dime cuáles encontraste y confírmame antes de decidir el filtro final.
> 2. Corrige también el problema de rendimiento: cronogramaRepo.findAll() carga
>    TODAS las filas de la tabla cronograma con 4 relaciones en EAGER (solicitud,
>    convocatoria, sala, bloque) sin JOIN FETCH, generando N+1 queries. Cambia el
>    método del repositorio para que use una query JPQL con JOIN FETCH de esas
>    4 relaciones (o un @EntityGraph), y agrégale el filtro de estado directamente
>    en la consulta SQL (WHERE) en vez de traer todo y filtrar en memoria con
>    .stream().filter(...).
> 3. Revisa el método equivalente para "Estadísticas PDF" en el mismo
>    ReporteController.java (busca reporteEstadisticas(), alrededor de la línea
>    109-175): tiene el mismo patrón de findAll() con relaciones EAGER sin JOIN
>    FETCH sobre evaluaciones_finales (15,401 filas reales) y un bucle que navega
>    evaluacion.getSolicitud().getEstudiante().getUsuario() fila por fila. No
>    encontré ahí un filtro de código roto como en Cronograma, pero aplícale la
>    misma optimización de JOIN FETCH por las dudas de que el volumen real esté
>    causando timeout o truncamiento. Pruébalo generando el PDF real contra la
>    base de datos de desarrollo (1,010,242 registros) y confírmame cuántas filas
>    salen y cuánto tarda, antes y después del cambio.
> ```

---

## ERR-06 — Endpoints sin protección de rol (hallazgo de seguridad, no lo pediste pero es serio)

Durante la auditoría general encontré varios controllers que **mutan datos sin ningún `@PreAuthorize`**, es decir, cualquier usuario autenticado (incluyendo un ESTUDIANTE) puede llamarlos directamente:

| Controller | Endpoints sin protección | Riesgo concreto |
|---|---|---|
| `RubricaController.java` | POST/DELETE de rúbricas y criterios | Un estudiante podría borrar o alterar la rúbrica de calificación de todos |
| `ActaController.java` | POST `/generar/{id}`, `/firmar/{id}` | Cualquiera podría generar o firmar el acta oficial de cualquier solicitud |
| `CronogramaController.java` | POST `/crear`, `/auto/{id}`, DELETE `/{id}` | Cualquiera podría crear o borrar la fecha programada de otro estudiante |
| `TutorController.java` | POST `/asignar`, DELETE `/{id}` | Cualquiera podría asignar/quitar el tutor de cualquier solicitud |
| `SalaController.java` | POST/DELETE de salas | Cualquiera podría borrar una sala referenciada por un cronograma existente |
| `EvaluacionJuradoController.java` | POST `/guardar` | Cualquiera podría registrar una nota de jurado para cualquier solicitud |

También en el frontend: de todas las rutas de coordinador/docente en `app.routes.ts`, **solo** `admin/usuarios` tiene `canActivate: [roleGuard(['ADMIN'])]`. El resto (revisar-solicitudes, asignar-jurados, evaluar, cronograma, mis-asignaciones, etc.) solo verifica que haya sesión iniciada (`authGuard`), no el rol — un ESTUDIANTE autenticado podría escribir la URL directamente en el navegador y cargar esas pantallas.

Esto es más relevante para tu criterio de **seguridad** de la sustentación (ya que hay un ADR-005 dedicado a seguridad OWASP) que para el de optimización, pero lo dejo documentado aquí porque salió de la misma auditoría.

> ### 🔧 Prompt para corregir ERR-06
> ```
> En el proyecto PFC-Presustentaciones-2026, los siguientes controllers backend
> tienen endpoints que mutan datos (POST/DELETE) sin ningún @PreAuthorize, así que
> cualquier usuario autenticado -- incluido un ESTUDIANTE -- puede llamarlos
> directamente aunque el frontend no le muestre el botón:
>
> - backend/.../controllers/RubricaController.java (POST/DELETE de rubricas y criterios)
> - backend/.../controllers/ActaController.java (POST /generar/{id}, /firmar/{id})
> - backend/.../controllers/CronogramaController.java (POST /crear, /auto/{id}, DELETE /{id})
> - backend/.../controllers/TutorController.java (POST /asignar, DELETE /{id})
> - backend/.../controllers/SalaController.java (POST/DELETE de salas)
> - backend/.../controllers/EvaluacionJuradoController.java (POST /guardar)
>
> 1. Para cada endpoint de la lista, agrega @PreAuthorize("hasAnyRole('ADMIN',
>    'COORDINADOR')") o el rol que corresponda según quién debería poder hacer esa
>    acción en el negocio real (por ejemplo, /guardar de EvaluacionJuradoController
>    probablemente debería permitir también 'DOCENTE' si un jurado docente registra
>    su propia nota -- revisa el flujo real antes de decidir y dime qué encontraste
>    si tienes dudas en algún caso).
> 2. Sigue exactamente el mismo patrón que ya usan otros endpoints protegidos del
>    mismo proyecto (por ejemplo JuradoController.java linea ~114,
>    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')") sobre asignar tutor) para
>    mantener consistencia de estilo.
> 3. En el frontend, Frontend/src/app/app.routes.ts solo protege la ruta
>    admin/usuarios con canActivate: [roleGuard(['ADMIN'])]. Agrega el mismo
>    roleGuard (con los roles que correspondan: ADMIN/COORDINADOR para las rutas
>    bajo admin/*, DOCENTE para las rutas bajo docente/* y jurado/*) a TODAS las
>    demás rutas de coordinador/docente que hoy solo tienen authGuard, para que un
>    estudiante autenticado no pueda cargar esas pantallas escribiendo la URL a mano.
> 4. Después de aplicar esto, corre los tests de backend existentes
>    (mvnw test) para confirmar que no rompiste ningún flujo legítimo que dependía
>    de que esos endpoints fueran accesibles sin restricción de rol.
> ```

---

## ERR-07 — Errores silenciados por rol (no bloquean la demo, pero generan confusión)

Errores reales, más una lista de UX que de bugs bloqueantes — el patrón común es un `catch`/`subscribe` que apaga el spinner de carga pero **no le dice nada al usuario** cuando algo falla:

**Estudiante:**
- `mi-horario.component.ts:35` — si falla la carga del horario, la lista queda vacía sin ningún aviso (parece que "no tiene horario" en vez de "falló la carga").
- `mis-notas.component.ts:36` — mismo patrón con las notas.

**Docente / Jurado:**
- `evaluar-rubrica.component.ts:86-95, 121-128` — si falla la carga de la rúbrica o del tribunal, el formulario de calificar se queda sin criterios, sin ningún mensaje — justo la pantalla central para un jurado en la demo.
- `mis-asignaciones.component.ts:39-43, 53` — mismo patrón.
- `detalle-tutoria.component.ts:235-297` — si falla la recarga después de subir un PDF o aprobar una fase, la pantalla puede quedar desactualizada sin avisar.

**Coordinador:**
- `revisar-solicitudes.component.ts:75` — `cargarConteos()` tiene un catch **completamente vacío** (`error: () => {}`) — los contadores de las pestañas (Todas, Por revisar, etc.) se quedan en 0 o desactualizados sin ningún aviso.
- `programar-cronograma.component.ts:67` — si falla la carga de salas, el desplegable de salas queda vacío sin explicación, bloqueando silenciosamente la programación — justo la pantalla que usarías para agendar una pre-sustentación en la demo.
- `programar-cronograma.component.ts:190-193` — `eliminarCronograma()` tampoco tiene manejo de error.

**Nota aparte (no es un bug):** el archivo `Frontend/src/app/interceptors/auth.interceptor.ts`, que apareció modificado en tu `git status` al inicio de la sesión, sí es un cambio intencional y terminado — agrega manejo global de sesión expirada (401): limpia el `localStorage` y redirige a `/login` con un aviso, evitando redirecciones dobles. No es un cambio a medio hacer, no requiere acción.

> ### 🔧 Prompt para corregir ERR-07
> ```
> En el proyecto PFC-Presustentaciones-2026 (frontend Angular), varios componentes
> tienen llamadas .subscribe(...) cuyo manejador de error, o no existe, o solo
> apaga el spinner de carga sin avisarle nada al usuario. Corrige cada uno de estos,
> agregando una llamada al servicio de notificaciones ya existente en el proyecto
> (revisa cómo lo usan otros componentes que sí manejan bien el error, por ejemplo
> buscando notification.error( o NotificationService en el frontend, y sigue ese
> mismo patrón) con un mensaje claro para el usuario:
>
> - Frontend/src/app/components/horario/mi-horario.component.ts linea 35
> - Frontend/src/app/components/notas/mis-notas.component.ts linea 36
> - Frontend/src/app/components/jurado/evaluar-rubrica/evaluar-rubrica.component.ts
>   lineas 86-95 y 121-128 (cargarRubrica y cargarTribunal -- hoy no tienen ni
>   siquiera el callback de error en el subscribe, solo 'next')
> - Frontend/src/app/components/jurado/mis-asignaciones.component.ts lineas 39-43 y 53
> - Frontend/src/app/components/tutorias/detalle-tutoria/detalle-tutoria.component.ts
>   lineas 235-245 y 275-297 (el forkJoin de recarga silenciosa tras subir PDF o
>   aprobar una fase)
> - Frontend/src/app/components/admin/revisar-solicitudes/revisar-solicitudes.component.ts
>   linea 75 (cargarConteos tiene error: () => {} -- vacio del todo)
> - Frontend/src/app/components/admin/cronograma/programar-cronograma.component.ts
>   linea 67 (falta el callback de error completo en el subscribe de salaService.listar())
>   y lineas 190-193 (eliminarCronograma sin manejo de error)
>
> Para cada uno: agrega un mensaje de error visible al usuario explicando qué
> falló (ej. "No se pudo cargar el horario, intenta de nuevo" en vez de dejar la
> lista vacía en silencio). No cambies la lógica de negocio de estos componentes,
> solo el manejo de error -- es un cambio quirúrgico, no una reescritura.
> ```

---

## ERR-08 — Subir/reemplazar PDF de anteproyecto sin validar dueño ni rol

**Encontrado al revisar cómo se almacenan los PDFs (no estaba en el informe original).**

**Causa raíz confirmada:** `AnteproyectoController.java`, endpoint `POST /api/anteproyectos/enviar/{solicitudId}` (líneas 33-37), **no tiene `@PreAuthorize`**, a diferencia de `/aprobar/{id}` y `/rechazar/{id}` en el mismo archivo, que sí lo tienen (`hasAnyRole('ADMIN', 'COORDINADOR', 'DOCENTE')`). Tampoco verifica en el service (`AnteproyectoServiceImpl.enviarAnteproyecto()`) que el usuario autenticado sea el estudiante dueño de esa `solicitudId`.

**Riesgo concreto:** cualquier usuario autenticado (incluido otro estudiante) podría subir o reemplazar el PDF del anteproyecto de **cualquier** solicitud ajena, con solo conocer o adivinar su `solicitudId` (un entero secuencial, fácil de enumerar).

> ### 🔧 Prompt para corregir ERR-08
> ```
> En el proyecto PFC-Presustentaciones-2026, backend/src/main/java/ec/edu/uteq/
> presustentaciones/controllers/AnteproyectoController.java, el endpoint
> POST /enviar/{solicitudId} (lineas 33-37) no tiene @PreAuthorize ni valida que
> el usuario autenticado sea el dueño (estudiante) de esa solicitud. A diferencia
> de /aprobar/{id} y /rechazar/{id} en el mismo archivo, que sí exigen
> @PreAuthorize("hasAnyRole('ADMIN', 'COORDINADOR', 'DOCENTE')"), /enviar/{id}
> puede ser llamado por cualquier usuario autenticado sobre cualquier solicitud
> ajena, con solo adivinar el id (es un entero secuencial).
>
> 1. Agrega @PreAuthorize("hasRole('ESTUDIANTE')") al endpoint /enviar/{solicitudId}
>    (revisa primero si ADMIN también debería poder subirlo en nombre de un
>    estudiante, por soporte/casos especiales, y dime qué prefieres).
> 2. Además del rol, agrega una verificación de PROPIEDAD en
>    AnteproyectoServiceImpl.enviarAnteproyecto(): antes de guardar el archivo,
>    confirma que solicitud.getEstudiante().getUsuario().getId() coincide con el
>    id del usuario autenticado (obtenlo del SecurityContext, sigue el mismo
>    patrón que ya use el resto del backend para saber "quién es el usuario
>    actual" -- búscalo en otro service que ya lo haga). Si no coincide, lanza un
>    error 403, no un RuntimeException genérico.
> 3. Prueba que un estudiante SÍ pueda seguir subiendo el PDF de su propia
>    solicitud después del cambio (no debe romper el flujo legítimo), y que
>    falle con 403 si intenta subirlo a una solicitud de otro estudiante.
> ```

---

## Recomendación de prioridad para el tiempo que te queda

1. **ERR-03 (roles de tribunal)** — es el que más se nota en una demo en vivo, porque justo hoy armamos el flujo de asignar tribunal. Sin esto, Vocal 1 y Vocal 2 nunca se pueden asignar manualmente ni se ven en Evaluar.
2. **ERR-05 (Cronograma PDF vacío)** — es una sola palabra mal escrita (`"ACTIVO"` en vez de `"PROGRAMADO"`), corrección muy barata con impacto alto si el profesor pide ver ese reporte.
3. **ERR-04 (Ver/Descargar PDF)** — cambiar `fetch()` por `HttpClient` en esas dos funciones es un cambio acotado.
4. **ERR-02 (combobox)** — requiere agregar una librería o construir un combobox propio; más trabajo, menos urgente si en la demo usas pocos docentes de prueba.
5. **ERR-06 y ERR-08 (seguridad)** — importantes para el criterio de seguridad, pero no bloquean la demo funcional; son mecánicos una vez decidido qué rol debe tener cada endpoint.
6. **ERR-01 y ERR-07** — mejoras de calidad, no bloquean nada puntual.

Este documento es solo el diagnóstico y los prompts de corrección. Ningún código de los puntos ERR-01 a ERR-08 fue modificado todavía — cada prompt está listo para pegarse en una sesión nueva cuando decidas aplicar esa corrección.
