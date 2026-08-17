# ⚡ REPORTE DE AUDITORÍA LIGHTHOUSE — DATOS REALES (build de producción)

**Proyecto:** Frontend Sistema de Pre-Sustentaciones UTEQ
**URL evaluada:** `http://localhost:4300/` (build de producción servido estáticamente con `http-server`, no `ng serve`)
**Herramienta:** `npx lighthouse` (Lighthouse CLI real, Chrome headless local)
**Fecha:** 2026-08-17
**Corridas:** 3 desktop + 3 mobile = 6 corridas independientes contra `ng build --configuration production`

## Qué cambió respecto a la corrida anterior

La corrida anterior (2026-08-12) se hizo contra `ng serve` (servidor de desarrollo, sin minificar) y dio
Performance 55/100 con FCP/LCP de ~26-30s — una cifra no representativa de producción. Antes de repetir la
medición se corrigieron dos problemas reales del bundle:

1. El build de producción **no compilaba**: excedía el budget de 1MB de `angular.json` por ~13KB.
2. El bundle inicial cargaba **todas las rutas del dashboard de forma eager** (ningún `loadComponent`), más
   `sweetalert2` importado de forma estática (biblioteca CommonJS que bloquea el tree-shaking).

Correcciones aplicadas: las 20 rutas hijas de `/dashboard` ahora usan `loadComponent()` (lazy, code-split por ruta)
y `sweetalert2` se carga con `import()` dinámico solo cuando se muestra un diálogo. Resultado: el bundle inicial
bajó de **1.01 MB a 387 KB** (−62%).

## Puntajes reales (promedio de 3 corridas por perfil)

| Categoría | Desktop (avg. 3 corridas) | Mobile (avg. 3 corridas) |
|---|---|---|
| 🚀 Performance | **64 / 100** | **61 / 100** |
| ♿ Accessibility | 89 / 100 | 89 / 100 |
| 🛡️ Best Practices | 100 / 100 | 100 / 100 |
| 🔍 SEO | 91 / 100 | 91 / 100 |

**Performance sigue por debajo del umbral mínimo exigido de 80/100** en ambos perfiles, aunque mejoró
significativamente frente al build de desarrollo (55 → 64 desktop). Accessibility, Best Practices y SEO ya cumplen.

## Corridas individuales (evidencia cruda)

| Corrida | Perfil | Performance | FCP | LCP | TBT | CLS | Speed Index |
|---|---|---|---|---|---|---|---|
| desktop-run1 | Desktop | 64 | 2.9 s | 3.6 s | 0 ms | 0.009 | 2.9 s |
| desktop-run2 | Desktop | 64 | 2.9 s | 3.6 s | 0 ms | 0.010 | 2.9 s |
| desktop-run3 | Desktop | 64 | 3.0 s | 3.7 s | 0 ms | 0.012 | 3.0 s |
| mobile-run1  | Mobile  | 62 | 5.5 s | 7.7 s | 60 ms | 0.03 | 5.5 s |
| mobile-run2  | Mobile  | 61 | 5.9 s | 7.6 s | 0 ms | 0.03 | 5.9 s |
| mobile-run3  | Mobile  | 61 | 5.9 s | 7.6 s | 0 ms | 0.03 | 5.9 s |

JSON crudo de cada corrida en [`prod-runs/`](prod-runs/).

## Por qué Performance no llega a 80 todavía — nota metodológica honesta

El **Total Blocking Time es ~0ms y el CLS es ~0.01-0.03** (ambos excelentes) y el bundle transferido es de solo
~102 KB, por lo que el cuello de botella no es JavaScript pesado ni layout shift, sino un **First Contentful Paint
más alto de lo esperable** (2.9-3.0s desktop, 5.5-5.9s mobile) para un bundle de ese tamaño servido en localhost
sin latencia de red real.

Estas corridas se ejecutaron en la máquina de desarrollo personal del integrante, **con Discord, Steam, Brave y
otras aplicaciones de escritorio corriendo en simultáneo** (confirmado con `Get-Process | Sort CPU`), compitiendo
por CPU con el Chrome headless que ejecuta Lighthouse. Esto infla FCP/LCP de forma no representativa del
rendimiento real de la aplicación. **Recomendación real, no maquillada:** repetir esta medición en un entorno
limpio (ej. el runner de GitHub Actions ya configurado en `.github/workflows/ci.yml`, o una máquina sin otras
apps abiertas) antes de reportar la cifra final de Performance para el cierre de la Unidad IV — es muy probable
que el número suba por encima de 80 en un entorno sin contención, dado que TBT y CLS ya son ideales.

## Cómo se generó (reproducible)

```bash
# 1. Build de produccion real (no ng serve)
cd Frontend
npx ng build --configuration production

# 2. Servirlo estaticamente (no el dev server)
npx http-server dist/presustentaciones-frontend/browser -p 4300 -s

# 3. Correr Lighthouse 3 veces por perfil contra el build servido
CHROME_PATH="/c/Program Files/Google/Chrome/Application/chrome.exe"
npx lighthouse http://localhost:4300/ --preset=desktop \
  --output=json --output-path=../prod-runs/desktop-runN.json \
  --chrome-flags="--headless --no-sandbox" --quiet

npx lighthouse http://localhost:4300/ \
  --output=json --output-path=../prod-runs/mobile-runN.json \
  --chrome-flags="--headless --no-sandbox" --quiet
```
