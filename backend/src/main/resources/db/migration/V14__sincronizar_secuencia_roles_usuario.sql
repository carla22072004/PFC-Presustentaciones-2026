-- =============================================================================
-- V14__sincronizar_secuencia_roles_usuario.sql
-- RolUsuario.id usa @GeneratedValue(strategy = GenerationType.AUTO), que en Postgres
-- resuelve a una secuencia propia (roles_usuario_seq) -- pero los 4 roles base se
-- insertan con id explícito (1-4) vía SQL crudo (V13 y PreSustentacionesApplication),
-- sin pasar por Hibernate, así que la secuencia nunca se enteró y seguía en 1. El
-- primer POST /api/roles (crear un rol nuevo desde "Gestionar Roles") fallaba con
-- "duplicate key value violates unique constraint roles_usuario_pkey (id)=(1)".
-- Se sincroniza la secuencia al primer id libre después del máximo existente.
-- =============================================================================

SELECT setval('presus.roles_usuario_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM presus.roles_usuario), false);
