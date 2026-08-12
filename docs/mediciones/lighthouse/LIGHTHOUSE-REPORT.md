# ⚡ REPORTE DE AUDITORÍA LIGHTHOUSE — DATOS REALES

**Proyecto:** Frontend Sistema de Pre-Sustentaciones UTEQ
**URL evaluada:** `http://localhost:4200/` (redirige a `/login`, página pública — no requiere autenticación)
**Herramienta:** `npx lighthouse` (Lighthouse CLI real, Chrome headless local)
**Fecha:** 2026-08-12

Una versión anterior de este documento afirmaba Rendimiento 94, Accesibilidad 98, Buenas Prácticas 96 y SEO 95 sin que se hubiera corrido Lighthouse nunca. Estos son los resultados de correrlo de verdad:

## Puntajes reales

| Categoría | Puntaje real |
|---|---|
| 🚀 Performance | **55 / 100** |
| ♿ Accessibility | **89 / 100** |
| 🛡️ Best Practices | **100 / 100** |
| 🔍 SEO | **91 / 100** |

## Core Web Vitals reales

- First Contentful Paint: 26.5 s
- Largest Contentful Paint: 30.6 s
- Total Blocking Time: 50 ms
- Cumulative Layout Shift: 0.03
- Speed Index: 26.5 s

## Nota metodológica importante

Esta corrida se hizo contra el **servidor de desarrollo** (`ng serve`, sin minificar, con el cliente de live-reload de Vite activo), no contra un build de producción (`ng build --configuration production`). Eso explica los tiempos de FCP/LCP tan altos (~26-30s): el bundle de desarrollo es mucho más pesado y no está optimizado. Best Practices y SEO sí son representativos incluso en modo desarrollo. **Para tener una cifra de Performance representativa de producción, hay que correr Lighthouse contra `ng build --configuration production` servido estáticamente, no contra `ng serve`.** Eso queda pendiente para la siguiente entrega.

## Cómo se generó (reproducible)

```bash
# Con el frontend corriendo en localhost:4200 (ng serve)
CHROME_PATH="/c/Program Files/Google/Chrome/Application/chrome.exe" \
npx lighthouse http://localhost:4200/ \
  --output=json --output=html --output-path=./lighthouse-result \
  --chrome-flags="--headless=new --no-sandbox --disable-gpu" \
  --only-categories=performance,accessibility,best-practices,seo
```
