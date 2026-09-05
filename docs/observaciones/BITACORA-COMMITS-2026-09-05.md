# Bitácora: dos commits con mensaje autogenerado (2026-09-05)

**Commits afectados:** `73cf9cb` ("T2982") y `42f15dc` ("U74202")
**Autor:** Jean30042 <jeanalavaalavarado@gmail.com>
**Fecha:** 2026-09-05, 12:00:31 y 12:00:59 (-05:00)
**Padre del primero:** `99a6636`

## Qué pasó

Durante la ronda de correcciones del 2026-09-05, el trabajo en curso quedó registrado en dos
commits cuyos mensajes son cadenas autogeneradas sin significado (`T2982`, `U74202`), producto de
un guardado automático del entorno de desarrollo y no de un `git commit` escrito a mano. El
**contenido de ambos es correcto y forma parte del trabajo planificado**; lo que falta en ellos es
el mensaje que explique qué cambiaron y por qué.

Se documenta aquí en vez de reescribir la historia porque ambos commits **ya estaban publicados en
`origin/main`** cuando se detectó el problema: corregirlos habría exigido un `push --force` sobre la
rama compartida, que rompe la copia local de cualquier integrante que ya hubiera hecho `pull`. Se
prefiere un historial con dos mensajes pobres y una nota que los explique, antes que un historial
"limpio" a costa de romperle la rama al resto del equipo.

Esta nota existe además porque la guía de la Entrega Final evalúa explícitamente la calidad del
historial y señala como defecto los mensajes que son "una palabra suelta, un código sin significado
o la palabra update". Se reconoce el defecto en vez de dejarlo pasar sin explicación.

## Qué contiene realmente cada commit

### `73cf9cb` — mensaje real que le correspondía

> `docs(api): documentar controladores de catálogo, auditoría y usuarios + evidencia de CI`

10 archivos, +1.165 / -14 líneas:

| Archivo | Qué cambió |
|---|---|
| `controllers/AuditoriaController.java` | Javadoc de los 2 métodos públicos (filtros del historial y catálogo de tablas auditadas) |
| `controllers/CatalogoController.java` | Javadoc de los 20 métodos del CRUD de facultades, carreras, modalidades y períodos |
| `controllers/EstadoTiempoRealController.java` | Javadoc de los 2 endpoints de polling |
| `controllers/ExternalApiController.java` | Javadoc del endpoint de universidades (API externa cacheada en Redis) |
| `controllers/ObservacionesController.java` | Javadoc del endpoint de observaciones por solicitud |
| `controllers/ProgresoTitulacionController.java` | Javadoc de los 2 endpoints de la ruta de titulación |
| `controllers/UsuarioController.java` | Javadoc de los 11 métodos públicos + el helper de propiedad del recurso |
| `docs/evidencias/ci/README.md` | Documento nuevo: evidencia de corridas verdes de CI y cómo reproducirla |
| `docs/evidencias/ci/run-33944802465-jobs.json` | Respuesta cruda de la API de GitHub para esa corrida |
| `docs/evidencias/ci/run-33949588592-jobs.json` | Respuesta cruda de la API de GitHub para esa corrida |

### `42f15dc` — mensaje real que le correspondía

> `docs(informe): reemplazar la brecha del Anexo C por evidencia real de CI`

4 archivos, +2.226 / -12 líneas:

| Archivo | Qué cambió |
|---|---|
| `Informe-Final/secciones/17-anexos.tex` | El Anexo C dejó de declarar una brecha ("faltan las capturas") y pasa a documentar tres corridas verdes reales, con el comando de reproducción y una tabla de run IDs |
| `Informe-Final/secciones/09-implementacion.tex` | Se actualizó la frase que remitía al Anexo C como brecha pendiente |
| `docs/evidencias/ci/run-33978578357-jobs.json` | Respuesta cruda de la API de GitHub para la tercera corrida |
| `docs/evidencias/ci/runs-listado.json` | Listado crudo de las últimas corridas del workflow |

## Cómo verificar que esta nota es fiel

Los mensajes de arriba son una reconstrucción documentada, no una reescritura: los commits siguen
tal cual en la historia. Cualquiera puede contrastar esta bitácora contra el repositorio:

```bash
git show --stat 73cf9cb
git show --stat 42f15dc
```

El diff de esos dos commits debe coincidir con las tablas de este documento.
