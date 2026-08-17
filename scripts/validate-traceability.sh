#!/usr/bin/env bash
# Valida que cada fila de docs/trazabilidad/matriz.csv (schema v1.0.0, Fase 7) apunte a algo
# que realmente existe en el repositorio -- no infiere evidencia del contenido de otros
# documentos, la busca literalmente en el arbol de archivos. Ademas cruza el campo
# Estado_Verificacion declarado contra lo que realmente se encuentra en disco, para detectar
# si en el futuro alguien marca VERIFICADO sin que exista el test real (el mismo tipo de
# discrepancia que motivo esta Fase 7: matriz.csv v0.9.0-rc citaba 5 archivos de test
# inexistentes y 3 citas de evidencia empirica incorrectas).
#
# Columnas esperadas (cabecera):
#   ID_Requisito,Historia_Usuario,Modulo_Backend,Endpoint_REST,Prioridad_MoSCoW,
#   Caso_Prueba_Unitario_Real,Estado_Verificacion,Evidencia_Empirica_Real
#
# Uso:
#   ./scripts/validate-traceability.sh
# Exit code: 0 si todas las filas son consistentes, 1 si alguna no lo es.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MATRIZ="$REPO_ROOT/docs/trazabilidad/matriz.csv"
BACKEND_SRC="$REPO_ROOT/backend/src"

if [ ! -f "$MATRIZ" ]; then
    echo "ERROR: no se encontro $MATRIZ"
    exit 1
fi

fails=0
total=0
must_total=0
must_verificado=0

while IFS=',' read -r id_req hu modulo endpoint prioridad caso_prueba estado evidencia; do
    total=$((total + 1))
    row_ok=1

    if [ "$prioridad" = "Must" ]; then
        must_total=$((must_total + 1))
    fi

    # --- Determinar si REALMENTE existe un archivo de test para esta fila ---
    test_real_existe=0
    for archivo in $(echo "$caso_prueba" | tr ';' ' '); do
        if [[ "$archivo" == *.java ]] && find "$BACKEND_SRC/test" -name "$archivo" 2>/dev/null | grep -q .; then
            test_real_existe=1
        fi
    done

    # --- Cruzar Estado_Verificacion declarado contra la realidad ---
    if [ "$estado" = "VERIFICADO" ] && [ "$test_real_existe" -eq 0 ]; then
        echo "[FALLO] $id_req: declarado VERIFICADO pero ningun archivo de '$caso_prueba' existe realmente"
        row_ok=0
    elif [ "$estado" = "NO_VERIFICADO" ] && [ "$test_real_existe" -eq 1 ]; then
        echo "[AVISO] $id_req: declarado NO_VERIFICADO pero SI existe un test real ('$caso_prueba') -- actualizar matriz.csv, ahora hay mas cobertura de la declarada"
    fi

    if [ "$prioridad" = "Must" ] && [ "$estado" = "VERIFICADO" ] && [ "$test_real_existe" -eq 1 ]; then
        must_verificado=$((must_verificado + 1))
    fi

    # --- Verificar Endpoint_REST (ruta base, sin variables de path) ---
    ruta="${endpoint#* }"
    ruta_base="$(echo "$ruta" | sed -E 's#\{[^}]*\}##g; s#/$##')"
    segmento="$(echo "$ruta_base" | sed -E 's#^/api(/v1)?/##' | cut -d/ -f1)"
    if [ -n "$segmento" ] && ! grep -rq "\"/api/$segmento\"\|\"/api/v1/$segmento\"" "$BACKEND_SRC/main/java" 2>/dev/null; then
        echo "[FALLO] $id_req: no se encontro un @RequestMapping que contenga '/api/$segmento' en los controladores"
        row_ok=0
    fi

    if [ "$row_ok" -eq 1 ]; then
        echo "[OK]    $id_req ($hu, $prioridad) -> $estado, $endpoint"
    else
        fails=$((fails + 1))
    fi
done < <(tail -n +2 "$MATRIZ")

echo ""
echo "=== Resumen: $((total - fails))/$total filas consistentes ==="
if [ "$must_total" -gt 0 ]; then
    pct=$(( must_verificado * 100 / must_total ))
    echo "=== Requisitos Must verificados con test real: $must_verificado/$must_total (${pct}%) ==="
fi
if [ "$fails" -gt 0 ]; then
    echo "$fails fila(s) inconsistentes -- ver [FALLO] arriba."
    exit 1
fi
exit 0
