# ⚖️ DOCUMENTO ÉTICO Y PROTOCOLO DE CONSENTIMIENTO INFORMADO

**Proyecto:** Sistema de Gestión de Pre-Sustentaciones UTEQ  
**Documento:** `docs/etica/ETHICS.md`  
**Comité de Ética:** Comisión de Bioética e Investigación Académica UTEQ  

---

## 📌 1. Declaración de Principios Éticos

El desarrollo del **Sistema de Gestión de Pre-Sustentaciones UTEQ** se rige rigurosamente por los principios bioéticos de la **Declaración de Helsinki** y la normativa de protección de datos personales de la República del Ecuador:

1. **Autonomía y Voluntariedad:** ningún estudiante o docente será obligado a participar en las pruebas de usabilidad ni en la recolección de métricas cuando estas se realicen con personas reales.
2. **Confidencialidad y Anonimización:** el protocolo (plantilla en [`consentimientos/plantilla-consentimiento.md`](consentimientos/plantilla-consentimiento.md)) prevé anonimizar con códigos alfanuméricos (`E1`, `E2`, ...) a cualquier participante real. **Nota de integridad (2026-08-17):** una versión anterior de este documento y de `docs/mediciones/sus/SUS-RESULTS.md` afirmaba que ya se habían aplicado 10 evaluaciones SUS anonimizadas como `E1`-`E10` — esos datos eran **fabricados**, nunca hubo participantes reales. Ver la corrección en `SUS-RESULTS.md`. El protocolo de anonimización descrito aquí sigue siendo el que se usará cuando se apliquen evaluaciones reales.
3. **No Maleficencia:** las pruebas sintéticas de carga y seguridad se ejecutan exclusivamente en entornos aislados de desarrollo local, sin afectar servidores ni bases de datos de producción institucionales.

---

## 📋 2. Plantilla del Formulario de Consentimiento Informado

La plantilla completa vive en [`consentimientos/plantilla-consentimiento.md`](consentimientos/plantilla-consentimiento.md) (movida a su propio archivo en la Fase 6 para coincidir con la ruta `docs/etica/consentimientos/` exigida por la guía). Cubre: propósito de la evaluación, procedimiento, riesgos/beneficios, confidencialidad, y declaración firmada del participante.

---

## 📑 3. Registro de Autorizaciones y Custodia de Documentos

**Estado real (2026-08-17):** no existe todavía ningún formulario firmado — el instrumento SUS está listo pero **no se ha aplicado a participantes reales** (ver [`../mediciones/sus/SUS-RESULTS.md`](../mediciones/sus/SUS-RESULTS.md)). Una versión anterior de este documento afirmaba que 10 formularios firmados reposaban en la secretaría académica de la Facultad de Ciencias de la Ingeniería de la UTEQ; esa afirmación era falsa y se retira aquí explícitamente, en vez de dejarla sin corregir en un documento de ética.

Cuando se apliquen evaluaciones reales, los formularios firmados (físicos o digitales, usando la plantilla de [`consentimientos/plantilla-consentimiento.md`](consentimientos/plantilla-consentimiento.md)) deberán conservarse por un periodo mínimo de 2 años con fines de auditoría, en la secretaría académica de la Facultad de Ciencias de la Ingeniería de la UTEQ o en un repositorio digital con control de acceso equivalente.
