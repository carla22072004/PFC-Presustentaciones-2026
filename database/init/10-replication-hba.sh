#!/bin/sh
# Se ejecuta solo en la inicialización de un clúster nuevo (docker-entrypoint-initdb.d).
# Habilita conexiones de replicación desde cualquier host para que el backend pueda
# ejecutar pg_basebackup (respaldo físico base para PITR, ver Gestión de Respaldos > Fase 2).
# El pg_hba.conf por defecto de la imagen solo permite replicación desde localhost.
set -e
HBA="${PGDATA:-/var/lib/postgresql/data}/pg_hba.conf"
if ! grep -q "host[[:space:]]\+replication[[:space:]]\+all[[:space:]]\+all" "$HBA"; then
    echo "host replication all all scram-sha-256" >> "$HBA"
    echo "[init] pg_hba.conf: habilitada la replicación desde cualquier host (pg_basebackup)."
fi
