# 🌐 REGISTRO DE IDENTIFICADOR PERSISTENTE DOI EN ZENODO — DEPÓSITO DEL SOFTWARE

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Estado:** ⏳ Pendiente de archivo — este repositorio **todavía no tiene un DOI real asignado**.
**Alcance de este documento:** el DOI del **software** (el código de este repositorio). El
conjunto de datos de mediciones (k6, ZAP, Lighthouse, JaCoCo) se deposita por separado, con su
propia licencia CC-BY 4.0 y su propio DOI, siguiendo el principio de citación independiente entre
software y datos — ver [`ZENODO-DATASET.md`](ZENODO-DATASET.md).

---

## 📌 Por qué no hay un DOI todavía

Una versión anterior de este documento afirmaba un DOI de Zenodo (`10.5281/zenodo.14892026`) como si el proyecto ya estuviera archivado. Ese identificador no corresponde a este repositorio, así que se retiró de aquí, de `CITATION.cff` y del badge en `README.md` en vez de reemplazarlo por otro número inventado.

Tampoco existe todavía un **release ni un tag `v1.0.0`** en el repositorio (`git tag -l` solo muestra `v0.7.0`, `v0.7.1`, `v0.9.0-rc` al momento de escribir esto) — el SRS y varios documentos internos ya alcanzaron el estado "v1.0.0" en su numeración propia, pero el repositorio como un todo (y `CITATION.cff`) todavía declara `version: "0.9.0-rc"`. Este documento no adelanta esa decisión: publicar el release v1.0.0 es una acción visible (queda pública en GitHub y dispara el depósito automático en Zenodo) que le corresponde decidir y ejecutar al equipo, no algo para automatizar silenciosamente.

## ✅ Cómo obtener un DOI real para el estado v1.0.0 (proceso real, ~10-15 minutos)

1. Confirmar que el estado del repositorio que se quiere archivar está realmente completo (todas las fases del plan de corrección aplicadas, `make all` pasa limpio — ver Makefile) y hacer commit/push de todo lo pendiente.
2. Actualizar `CITATION.cff`: cambiar `version: "0.9.0-rc"` a `version: "1.0.0"` y `date-released` a la fecha real del release. Hacer commit de ese cambio.
3. Entrar a [zenodo.org](https://zenodo.org) e iniciar sesión con la cuenta de GitHub de uno de los integrantes.
4. En [zenodo.org/account/settings/github/](https://zenodo.org/account/settings/github/), activar el toggle para este repositorio (`carla22072004/PFC-Presustentaciones-2026`).
5. En GitHub, crear el tag `v1.0.0` y publicar un **Release** nuevo a partir de ese tag (Releases → Draft a new release).
6. Zenodo detecta el release automáticamente y genera un DOI real y permanente para esa versión exacta del código.
7. Una vez generado, actualizar `CITATION.cff` (descomentar y completar el campo `doi:`), el badge de `README.md` y este archivo con el DOI real que Zenodo entregue.

**Nota de alcance:** los pasos 3-7 requieren iniciar sesión en cuentas externas (GitHub, Zenodo) del equipo — no son algo que se pueda ejecutar por fuera del equipo. Este documento deja el proceso listo para que tome ~10-15 minutos cuando el equipo decida ejecutarlo.

## 🏷️ Metadatos listos para cuando se publique el release

- **Título del Registro:** Sistema de Gestión de Pre-Sustentaciones de Trabajos de Titulación UTEQ
- **Licencia:** MIT Open Source License
- **Repositorio GitHub:** `https://github.com/carla22072004/PFC-Presustentaciones-2026`
- **Autores CRediT:** Jean Pierre Alava Alvarado, Xavier Alejandro Moncayo Loor, Carla Esthefania Zamora Arias, Heider Dominick Barreto Rosado.
- **Formato de Cita BibTeX (sin DOI hasta que exista uno real):**

```bibtex
@software{alava_alvarado_2026_presustentaciones,
  author       = {Alava Alvarado, Jean Pierre and Moncayo Loor, Xavier Alejandro and Zamora Arias, Carla Esthefania and Barreto Rosado, Heider Dominick},
  title        = {Sistema de Gestión de Pre-Sustentaciones de Titulación UTEQ},
  year         = 2026,
  publisher    = {Zenodo},
  version      = {v0.9.0-rc},
  url          = {https://github.com/carla22072004/PFC-Presustentaciones-2026}
}
```
