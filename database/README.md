# database/ — Respaldo completo de la base de datos (1M+ registros)

## Qué es este archivo

`presusDb_full_1M.dump` es un respaldo **completo** de `BdPresustentaciones` (esquema `presus`),
generado con `pg_dump -Fc -Z 9` (formato custom, comprimido) contra el volumen real de datos del
proyecto: **1,005,144 registros** en 41 tablas, ~179 MB sin comprimir → **12.8 MB comprimido**.

Ver [`docs/basedatos/VOLUMEN-DATOS.md`](../docs/basedatos/VOLUMEN-DATOS.md) para el desglose exacto
por tabla y cómo se generó ese volumen (script reproducible en
[`scripts/generar-volumen-datos.sql`](../scripts/generar-volumen-datos.sql)).

Generado: 24/08/2026.

## Cómo restaurarlo

**1. Crear una base vacía:**

```bash
createdb -U postgres BdPresustentaciones
```

**2. Restaurar el dump:**

```bash
pg_restore -U postgres -d BdPresustentaciones --no-owner -j 4 database/presusDb_full_1M.dump
```

`-j 4` paraleliza la restauración en 4 procesos (ajustable según CPU disponible).

**3. Verificar:**

```sql
SELECT SUM(x.filas) AS total_registros
FROM (
  SELECT r.tablename,
    (xpath('/row/c/text()', query_to_xml(
       format('SELECT count(*) AS c FROM presus.%I', r.tablename), false, true, ''
    )))[1]::text::bigint AS filas
  FROM pg_tables r WHERE r.schemaname = 'presus'
) x;
-- Debe devolver 1,005,144 (o el total vigente si este archivo se regenera después)
```

## Por qué un `.dump` binario y no un `.sql` plano

Un dump en texto plano de este mismo volumen pesa varios cientos de MB sin comprimir — con
formato `custom` comprimido (`-Fc -Z 9`) se mantiene muy por debajo del límite de 100 MB por
archivo de GitHub sin necesitar Git LFS. La contrapartida es que solo se restaura con
`pg_restore`, no con `psql < archivo.sql`.
