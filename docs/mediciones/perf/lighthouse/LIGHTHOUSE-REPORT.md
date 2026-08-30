# ⚡ REPORTE DE AUDITORÍA LIGHTHOUSE — DATOS REALES (build de producción)

**Proyecto:** Frontend Sistema de Pre-Sustentaciones UTEQ
**URL evaluada:** `http://localhost:4300/` (build de producción servido estáticamente con `http-server`, no `ng serve`)
**Herramienta:** `npx lighthouse` (Lighthouse CLI real, Chrome headless local)
**Fecha:** 2026-08-17, re-corrido 2026-08-30 (auditoría de reproducibilidad, tras corregir 2 hallazgos reales de accesibilidad)
**Corridas:** 3 desktop + 3 mobile = 6 corridas independientes contra `ng build --configuration production`, más las 6 corridas del 17-08 conservadas en [`prod-runs/2026-08-17-PREVIOUS/`](prod-runs/2026-08-17-PREVIOUS/)

## Corrección real 2026-08-30: Accessibility 89 → 100

La corrida del 17-08 reportaba **Accessibility 89/100**, por debajo del umbral de 90 exigido por la guía. Se
investigaron los audits reales que fallaban (no se asumió nada):

1. **`color-contrast`** (peso 7, el mayor de los dos) — `.login-footer p` en
   [`login.component.css`](../../../../Frontend/src/app/components/auth/login/login.component.css) usaba
   `color: #94a3b8` (slate-400) sobre el fondo translúcido de `.glass-card`, insuficiente para el ratio AA
   mínimo de 4.5:1. Corregido a `#475569` (slate-600).
2. **`landmark-one-main`** (peso 3) — el documento no tenía ningún elemento `<main>`; toda la estructura
   usaba `<div>`. Corregido cambiando el `<div class="dubai-wrapper">` raíz de
   [`login.component.html`](../../../../Frontend/src/app/components/auth/login/login.component.html) a
   `<main class="dubai-wrapper">` (selector CSS es por clase, no por tipo de elemento, así que el cambio no
   afecta el layout).

Verificado real: tras `ng build --configuration production` y re-correr Lighthouse, **ambos audits pasan
(`score: 1`) y Accessibility sube a 100/100 en las 6 corridas nuevas**, sin ningún audit fallando.

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

## Puntajes reales (promedio de 3 corridas por perfil) — 2026-08-30

| Categoría | Desktop (avg. 3 corridas) | Mobile (avg. 3 corridas) | Umbral guía |
|---|---|---|---|
| 🚀 Performance | **65 / 100** | **61 / 100** | ≥80 — ❌ no cumplido |
| ♿ Accessibility | **100 / 100** | **100 / 100** | ≥90 — ✅ (era 89, corregido) |
| 🛡️ Best Practices | 100 / 100 | 100 / 100 | ≥90 — ✅ |
| 🔍 SEO | 91 / 100 | 91 / 100 | ≥90 — ✅ |

Accessibility, Best Practices y SEO ya cumplen los 4 umbrales que exige la guía salvo Performance.

## Corridas individuales (evidencia cruda, 2026-08-30)

| Corrida | Perfil | Performance | Accessibility | FCP | LCP | TBT | CLS | Speed Index |
|---|---|---|---|---|---|---|---|---|
| desktop-run1 | Desktop | 64 | 100 | 2.9 s | 3.6 s | 0 ms | 0.01 | 2.9 s |
| desktop-run2 | Desktop | 65 | 100 | 2.9 s | 3.6 s | 0 ms | 0.008 | 2.9 s |
| desktop-run3 | Desktop | 65 | 100 | 2.8 s | 3.6 s | 0 ms | 0.01 | 2.8 s |
| mobile-run1  | Mobile  | 61 | 100 | 5.9 s | 7.6 s | 0 ms | 0.03 | 5.9 s |
| mobile-run2  | Mobile  | 61 | 100 | 6.1 s | 7.8 s | 0 ms | 0.03 | 6.1 s |
| mobile-run3  | Mobile  | 61 | 100 | 6.0 s | 7.7 s | 0 ms | 0.03 | 6.0 s |

JSON crudo de cada corrida en [`prod-runs/`](prod-runs/); la corrida del 17-08 (Accessibility 89) se conserva
sin modificar en [`prod-runs/2026-08-17-PREVIOUS/`](prod-runs/2026-08-17-PREVIOUS/).

## Por qué Performance no llega a 80 todavía — nota metodológica honesta, con experimento real (2026-08-30)

El **Total Blocking Time es 0ms, el CLS es ~0.01-0.03, `server-response-time`/`bootup-time`/`mainthread-work-breakdown`
puntúan perfecto (score 1)** — ninguno de los diagnósticos que miden trabajo real del código de la aplicación
señala un problema. El cuello de botella es un **First Contentful Paint más alto de lo esperable** (2.8-2.9s
desktop, 5.9-6.1s mobile) para un bundle servido en localhost sin latencia de red real.

La hipótesis de la corrida del 17-08 era contención de CPU por aplicaciones de escritorio (Discord, Steam,
Brave). Esta vez se probó esa hipótesis en vez de solo repetirla: se confirmó con `Get-Process | Sort CPU
-Descending` que Brave, Discord, Epic Games Launcher (+ su overlay `EOSOverlayRenderer`) y otras apps
efectivamente estaban corriendo, **se cerraron**, y se volvió a medir. **Resultado: el promedio de Performance
prácticamente no cambió** (64→65 desktop, 61→61 mobile) — la hipótesis original queda refutada por un
experimento controlado, no confirmada. La causa real del FCP/LCP elevado no se identificó en esta ronda (no es
JS bloqueante, no es respuesta de servidor, no es layout shift); queda declarada como pendiente de
investigación en vez de atribuida a una causa no verificada. Candidatos razonables para la próxima iteración:
overhead de Chrome headless en Windows, o interferencia de Windows Defender/Docker Desktop (no se probó
apagarlos porque afectaría el resto del entorno de desarrollo activo en esta misma máquina).

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
