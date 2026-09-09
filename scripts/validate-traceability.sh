#!/usr/bin/env bash
# =============================================================================
# validate-traceability.sh -- Validador de trazabilidad del SRS v2.0.0
#
# Comprueba que el SRS (docs/requisitos/SRS-v1.0.0.tex), la matriz
# (docs/trazabilidad/matriz.csv) y el arbol de archivos del repositorio
# describen EL MISMO sistema. No infiere nada: todo lo busca literalmente en
# disco.
#
# Las 8 comprobaciones estan especificadas en la seccion 9.1 del SRS:
#   V1  Toda fila tiene exactamente el numero de columnas de la cabecera.
#   V2  La columna Estado solo contiene el vocabulario cerrado de 4 valores.
#   V3  Todo ID del SRS esta en la matriz y viceversa.
#   V4  Todo endpoint citado resuelve contra un controlador real (RUTA
#       COMPLETA: metodo + camino, no solo el primer segmento).
#   V5  Toda clase de prueba citada existe en disco.
#   V6  Toda historia de usuario o caso de uso citado existe en disco.
#   V7  Ningun requisito Verificado carece de prueba o de evidencia.
#   V8  Ningun requisito Planificado cita un endpoint que ya existe.
#
# Por que V1 y V4 son nuevas: el validador anterior leia el CSV con IFS=','
# (se rompia con campos entrecomillados que contienen comas) y comprobaba el
# endpoint quedandose con el PRIMER SEGMENTO de la ruta. Por eso 9 de los 15
# endpoints de la matriz v1.0.0 llevaban un /v1 que su controlador no tiene y
# el validador los daba por buenos.
#
# Uso:   ./scripts/validate-traceability.sh
# Salida: 0 si todo es consistente, 1 si hay algun [FALLO].
# =============================================================================
set -uo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

command -v python3 >/dev/null 2>&1 || { echo "ERROR: se requiere python3"; exit 1; }

python3 - <<'PYEOF'
import csv, os, re, sys, glob

ROOT = os.environ["REPO_ROOT"]
MATRIZ = os.path.join(ROOT, "docs/trazabilidad/matriz.csv")
SRS    = os.path.join(ROOT, "docs/requisitos/SRS-v1.0.0.tex")
CTRL   = os.path.join(ROOT, "backend/src/main/java")
TESTS  = os.path.join(ROOT, "backend/src/test")

ESTADOS = {"Verificado", "Implementado", "Planificado", "No cumplido"}
fallos, avisos = [], []
def fallo(v, msg): fallos.append(f"[FALLO] {v}: {msg}")
def aviso(v, msg): avisos.append(f"[AVISO] {v}: {msg}")

for p, nombre in ((MATRIZ, "matriz.csv"), (SRS, "SRS-v1.0.0.tex")):
    if not os.path.isfile(p):
        print(f"ERROR: no se encontro {nombre} en {p}"); sys.exit(1)

# ---- Endpoints reales del backend -------------------------------------------
VAR = re.compile(r"\{[^}]*\}")
reales = set()
for f in glob.glob(os.path.join(CTRL, "**", "*Controller.java"), recursive=True):
    src = open(f, encoding="utf-8", errors="replace").read()
    m = re.search(r'@RequestMapping\("([^"]+)"\)', src)
    base = m.group(1) if m else ""
    for verbo, camino in re.findall(
            r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)")?', src):
        ruta = (base + camino).rstrip("/") or "/"
        reales.add(f"{verbo.upper()} {VAR.sub('{}', ruta)}")

# ---- Clases de prueba en disco ----------------------------------------------
tests = {os.path.basename(p) for p in glob.glob(os.path.join(TESTS, "**", "*.java"), recursive=True)}

# ---- IDs declarados en el SRS -----------------------------------------------
srs_txt = open(SRS, encoding="utf-8", errors="replace").read()
ids_srs = set(re.findall(r"\\REQ\{(RF-\d+|RNF-\d+)\}", srs_txt))

# ---- Lectura de la matriz (V1) ----------------------------------------------
with open(MATRIZ, encoding="utf-8", newline="") as fh:
    filas = list(csv.reader(fh))
cab, datos = filas[0], filas[1:]
ncols = len(cab)
for i, fila in enumerate(datos, start=2):
    if len(fila) != ncols:
        fallo("V1", f"linea {i}: {len(fila)} columnas, se esperaban {ncols} "
                    f"(coma sin comillas dentro de un campo?)")
if fallos:
    print("\n".join(fallos)); print("\nAbortado: la matriz no se puede leer por columnas."); sys.exit(1)

rows = [dict(zip(cab, f)) for f in datos]
ids_matriz = set()

for r in rows:
    rid  = r["ID_Requisito"].strip()
    ids_matriz.add(rid)
    est  = r["Estado"].strip()
    ep   = r["Endpoint_REST"].strip()
    caso = r["Caso_Prueba"].strip()
    evid = r["Evidencia"].strip()
    fuente = r["Fuente"].strip()

    # V2 -- vocabulario cerrado de estado
    if est not in ESTADOS:
        fallo("V2", f"{rid}: estado '{est}' fuera del vocabulario {sorted(ESTADOS)}")

    # V4 -- el endpoint resuelve, comparando la RUTA COMPLETA
    if ep and ep.lower() != "n/a":
        for uno in [e.strip() for e in ep.split("|") if e.strip()]:
            partes = uno.split(" ", 1)
            if len(partes) != 2:
                fallo("V4", f"{rid}: endpoint mal formado '{uno}' (falta metodo o ruta)"); continue
            verbo, ruta = partes[0].upper(), VAR.sub("{}", partes[1].rstrip("/"))
            if f"{verbo} {ruta}" not in reales:
                fallo("V4", f"{rid}: el endpoint '{uno}' no resuelve contra ningun controlador")

    # V5 -- las clases de prueba citadas existen
    for t in [t.strip() for t in caso.split(";") if t.strip()]:
        if t.endswith(".java") and t not in tests:
            fallo("V5", f"{rid}: la clase de prueba '{t}' no existe en disco")

    # V6 -- las historias y casos de uso citados existen
    for hu in re.findall(r"HU-\d+", fuente):
        if not os.path.isfile(os.path.join(ROOT, f"docs/requisitos/historias/{hu}.md")):
            fallo("V6", f"{rid}: la historia de usuario {hu} no existe en disco")
    for cu in re.findall(r"CU-\d+", fuente):
        if not os.path.isfile(os.path.join(ROOT, f"docs/requisitos/casos-de-uso/{cu}.md")):
            fallo("V6", f"{rid}: el caso de uso {cu} no existe en disco")

    # V7 -- Verificado exige prueba real o evidencia archivada
    if est == "Verificado":
        tiene_test = any(t.strip().endswith(".java") and t.strip() in tests
                         for t in caso.split(";"))
        if not tiene_test and not evid:
            fallo("V7", f"{rid}: declarado Verificado sin prueba automatizada ni evidencia")

    # V8 -- Planificado no puede citar un endpoint existente
    if est == "Planificado" and ep and ep.lower() != "n/a":
        fallo("V8", f"{rid}: declarado Planificado pero cita el endpoint '{ep}'; "
                    f"si el endpoint existe, el requisito esta al menos Implementado")

    # Aviso: Implementado con prueba real -> probablemente ya es Verificado
    if est == "Implementado" and any(t.strip().endswith(".java") and t.strip() in tests
                                     for t in caso.split(";")):
        aviso("V7", f"{rid}: declarado Implementado pero SI existe la prueba '{caso}'; "
                    f"revisar si corresponde Verificado")

# V3 -- correspondencia SRS <-> matriz, en ambos sentidos
for i in sorted(ids_srs - ids_matriz):
    fallo("V3", f"{i}: esta en el SRS y no en la matriz")
for i in sorted(ids_matriz - ids_srs):
    fallo("V3", f"{i}: esta en la matriz y no en el SRS")

# ---- Resumen ----------------------------------------------------------------
must = [r for r in rows if r["Prioridad_MoSCoW"].strip() == "Must"]
mv   = [r for r in must if r["Estado"].strip() == "Verificado"]
por_estado = {e: sum(1 for r in rows if r["Estado"].strip() == e) for e in sorted(ESTADOS)}

print(f"Requisitos en el SRS ....... {len(ids_srs)}")
print(f"Filas en la matriz ......... {len(rows)}")
print(f"Endpoints reales del backend {len(reales)}")
print(f"Clases de prueba en disco .. {len(tests)}")
print("Estados .................... " + ", ".join(f"{k}: {v}" for k, v in por_estado.items()))
print(f"Must verificados ........... {len(mv)}/{len(must)} "
      f"({(100*len(mv)//len(must)) if must else 0}%)")
print("")
for a in avisos: print(a)
if fallos:
    for f_ in fallos: print(f_)
    print(f"\n=== {len(fallos)} FALLO(S) -- la trazabilidad NO es consistente ===")
    sys.exit(1)
print("=== V1-V8: sin fallos. Trazabilidad consistente. ===")
sys.exit(0)
PYEOF
