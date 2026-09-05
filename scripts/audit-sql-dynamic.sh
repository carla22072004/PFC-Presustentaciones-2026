#!/usr/bin/env bash
# Corre el analisis estatico de seguridad (SpotBugs + find-sec-bugs), filtrado a la
# categoria SECURITY, que incluye la regla SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE
# (deteccion de SQL dinamico / inyeccion SQL). Es el mismo paso que corre
# .github/workflows/ci.yml en el job `backend`, expuesto aqui como script standalone
# para correrlo localmente sin depender de un push.
#
# Uso:
#   ./scripts/audit-sql-dynamic.sh
# Salida: backend/target/spotbugsXml.xml (reporte completo)
#         imprime en pantalla especificamente cualquier hallazgo de tipo SQL_*

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT/backend"

echo "Compilando y corriendo SpotBugs + find-sec-bugs (categoria SECURITY)..."
./mvnw -q compile com.github.spotbugs:spotbugs-maven-plugin:4.8.6.4:spotbugs

REPORT="target/spotbugsXml.xml"
if [ ! -f "$REPORT" ]; then
    echo "ERROR: no se genero $REPORT"
    exit 1
fi

echo ""
echo "=== Hallazgos de SQL dinamico / inyeccion (SQL_*) ==="
# El XML de SpotBugs se genera en una sola linea larga: grep -c cuenta LINEAS que
# matchean (siempre 1 o 0 aqui), no ocurrencias -- se usa grep -o + wc -l para
# contar ocurrencias reales dentro de esa unica linea.
sql_findings=$( (grep -o '<BugInstance type="SQL_[^"]*"' "$REPORT" || true) | wc -l | tr -d ' ')

if [ "$sql_findings" -eq 0 ]; then
    echo "0 hallazgos de tipo SQL_* (incluye SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE)."
    echo "Confirma que no se detecto SQL dinamico/no parametrizado en el codigo compilado."
else
    echo "$sql_findings hallazgo(s) de tipo SQL_* encontrados -- revisar $REPORT"
    grep -o '<BugInstance type="SQL_[^"]*"' "$REPORT" | sort | uniq -c
fi

echo ""
total_findings=$( (grep -o '<BugInstance ' "$REPORT" || true) | wc -l | tr -d ' ')
echo "Total de hallazgos de seguridad (todas las reglas): $total_findings"
echo "Reporte completo: $REPORT"
echo "Ver docs/mediciones/sec/static-analysis/STATIC-ANALYSIS.md para el desglose documentado por regla."

exit $([ "$sql_findings" -eq 0 ] && echo 0 || echo 1)
