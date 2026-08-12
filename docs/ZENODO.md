# 🌐 REGISTRO DE IDENTIFICADOR PERSISTENTE DOI EN ZENODO

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ
**Estado:** ⏳ Pendiente de archivo — este repositorio **todavía no tiene un DOI real asignado**.

---

## 📌 Por qué no hay un DOI todavía

Una versión anterior de este documento afirmaba un DOI de Zenodo (`10.5281/zenodo.14892026`) como si el proyecto ya estuviera archivado. Ese identificador no corresponde a este repositorio, así que se retiró de aquí, de `CITATION.cff` y del badge en `README.md` en vez de reemplazarlo por otro número inventado.

## ✅ Cómo obtener un DOI real (proceso real, ~10 minutos)

1. Entrar a [zenodo.org](https://zenodo.org) e iniciar sesión con la cuenta de GitHub de uno de los integrantes.
2. En [zenodo.org/account/settings/github/](https://zenodo.org/account/settings/github/), activar el toggle para este repositorio (`carla22072004/PFC-Presustentaciones-2026`).
3. En GitHub, publicar un **Release** nuevo (por ejemplo con el tag `v0.9.0-rc` o `v1.0.0`).
4. Zenodo detecta el release automáticamente y genera un DOI real y permanente para esa versión exacta del código.
5. Una vez generado, actualizar `CITATION.cff` (campo `doi:`), el badge de `README.md` y este archivo con el DOI real que Zenodo entregue.

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
