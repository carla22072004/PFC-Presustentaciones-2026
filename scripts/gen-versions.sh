#!/usr/bin/env bash
# Genera docs/entorno/versions.txt corriendo --version contra las herramientas realmente
# instaladas en la máquina donde se ejecuta -- no copia cifras de una corrida anterior. Pensado
# para correr tanto localmente como en el pipeline de CI (.github/workflows/ci.yml), que lo
# publica como artefacto en cada push (Fase 10, Criterio R1: reproducibilidad del entorno).
#
# Uso:
#   ./scripts/gen-versions.sh > docs/entorno/versions.txt
# o, dentro de CI, sin redirigir (el job lo publica con upload-artifact).

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

get() { command -v "$1" >/dev/null 2>&1 && eval "$2" 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | head -n 1 || echo "no instalado en esta máquina"; }

echo "# Versiones exactas del entorno usado para generar/verificar la evidencia del proyecto"
echo "# Generado automáticamente por scripts/gen-versions.sh el $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "# Host: $(uname -a 2>/dev/null || echo 'desconocido')"
echo "#"
echo "# Nota: el backend declara Java 17 como target de compilacion (maven.compiler.source/target"
echo "# en backend/pom.xml); si el JDK de esta maquina es una version mayor, Maven compila en modo"
echo "# --release 17 sobre ese JDK, lo cual es compatible y soportado -- no es una discrepancia."
echo ""
echo "Docker Engine:          $(get docker 'docker --version')"
echo "Docker Compose:         $(get docker 'docker compose version --short')"
echo "JDK (runtime):          $(get java 'java -version')"
echo "Apache Maven (wrapper):  $(cd "$REPO_ROOT/backend" && get ./mvnw './mvnw --version | head -n 1')"
echo "Node.js:                $(get node 'node --version')"
echo "npm:                    $(get npm 'npm --version')"
echo "Angular CLI:             $(cd "$REPO_ROOT/Frontend" && get npx 'npx ng version 2>/dev/null | grep "Angular CLI"')"
echo "k6:                     $(get k6 'k6 version')"
echo "Sistema operativo:      $(uname -s 2>/dev/null || echo 'desconocido') $(uname -r 2>/dev/null || true)"
echo "PostgreSQL (contenedor):15-alpine (ver docker-compose.yml)"
echo "Redis (contenedor):     7-alpine (ver docker-compose.yml)"
echo "nginx (contenedor):     alpine (ver docker-compose.yml)"
