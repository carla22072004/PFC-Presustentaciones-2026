# Casos de Uso finales (v1.0.0) — plantilla de Cockburn

**Hallazgo de partida:** antes de la Fase 7, **no existía ningún caso de uso formal en el repositorio**
(0 de 0), pese a que `README.md` y `AUTOEVALUACION-PRESUS.md` afirmaban "15 CUs". Los 12 casos de uso
de este directorio se escriben desde cero, en formato "fully dressed" de Alistair Cockburn (*Writing
Effective Use Cases*, 2001), con **nivel de meta explícito** según su notación de altitud (4 niveles
usados aquí, se omite deliberadamente el nivel "Almeja"/demasiado-bajo que el propio Cockburn
desaconseja):

| Símbolo | Nivel | Significa |
|---|---|---|
| ☁️ Nube | Muy alto / resumen estratégico | Varias metas de usuario combinadas en un objetivo de negocio |
| 🪁 Cometa | Resumen | Una meta compuesta por varios objetivos de usuario relacionados |
| 🌊 Mar | Meta de usuario (nivel primario) | Una sola sesión de un actor completando un objetivo — la mayoría de los CU están aquí |
| 🐟 Pez | Subfunción | Un paso que da soporte a un caso de uso de nivel Mar, no tiene valor de negocio por sí solo |

## Índice

| CU | Nivel | Requisito | Título |
|---|---|---|---|
| [CU-01](CU-01.md) | 🌊 Mar | RF-01 | Autenticarse en el sistema |
| [CU-02](CU-02.md) | 🌊 Mar | RF-02 | Registrar solicitud de pre-sustentación |
| [CU-03](CU-03.md) | 🌊 Mar | RF-03 | Asignar jurados a una solicitud |
| [CU-04](CU-04.md) | 🌊 Mar | RF-04 | Programar cronograma de defensa |
| [CU-05](CU-05.md) | 🌊 Mar | RF-05 | Evaluar por rúbrica |
| [CU-06](CU-06.md) | 🌊 Mar | RF-06 | Generar acta de pre-sustentación |
| [CU-07](CU-07.md) | 🐟 Pez | RF-07 | Firmar acta digitalmente (subfunción de CU-06) |
| [CU-08](CU-08.md) | 🐟 Pez | RF-08 | Notificar cambio de estado (subfunción disparada por CU-02..CU-06) |
| [CU-09](CU-09.md) | 🪁 Cometa | RF-09 | Generar reportes de gestión (agrega datos de varios CU de nivel Mar) |
| [CU-10](CU-10.md) | 🌊 Mar | RF-10 | Gestionar catálogo de salas |
| [CU-11](CU-11.md) | 🌊 Mar | RF-11 | Gestionar usuarios del sistema |
| [CU-12](CU-12.md) | 🌊 Mar | RF-12 | Cargar y verificar anteproyecto |

## ☁️ Caso de uso nivel Nube (resumen estratégico)

**"Tramitar una pre-sustentación de titulación de principio a fin"** es la meta de negocio que agrupa
los casos de uso de nivel Mar CU-02 → CU-12 → CU-03 → CU-04 → CU-05 → CU-06 (→ CU-07), en ese orden
secuencial típico, con CU-08 y CU-09 como soporte transversal. No se documenta como un archivo
`CU-00.md` separado porque no añade una meta de usuario distinta — es la composición de los 6 casos de
uso Mar del flujo principal, ya documentados individualmente.

## Nota sobre trazabilidad a diagramas de secuencia y pruebas de integración

Cada `CU-XX.md` incluye un diagrama de secuencia en Mermaid (nuevo en esta fase — antes no existía
ningún diagrama de secuencia en el repositorio, solo el modelo C4 en
[`../../arquitectura/README.md`](../../arquitectura/README.md)) grounded en el código real
(controlador → servicio → repositorio/procedimiento almacenado).

**Sobre pruebas de integración real:** el proyecto **no tiene ninguna prueba `@SpringBootTest` con base
de datos real** hoy — los tests existentes son unitarios (Mockito) o slice tests `@WebMvcTest`
(`AuthControllerIntegrationTest`, cuyo nombre sugiere integración pero técnicamente no lo es). Se
declara esto explícitamente en vez de forzar una trazabilidad falsa. Ver el detalle por caso de uso en
cada archivo individual.
