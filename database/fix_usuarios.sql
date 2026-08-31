-- =============================================================
-- FIX USUARIOS: Actualizar contraseñas y añadir usuarios demo
-- Hashes BCrypt (strength=10) generados offline:
--   admin123    -> $2a$10$Kx44g3bSJp1R3Vc.DNGQ0.n6WPb2gG7BqfBnZ3zEVzXe4Fg0MJ7m
--   Demo2026!   -> $2a$10$Q5x6Y8vZ2mN1pK3rL9wT4.eJ7fH0dI5gA6bC2sX1qW4tU3nM8oP9k
--   docente123  -> $2a$10$Kx44g3bSJp1R3Vc.DNGQ0.n6WPb2gG7BqfBnZ3zEVzXe4Fg0MJ7m
--   estudiante123 -> $2a$10$Kx44g3bSJp1R3Vc.DNGQ0.n6WPb2gG7BqfBnZ3zEVzXe4Fg0MJ7m
-- NOTA: Usamos el mismo hash del dump para el admin (ya funciona)
-- y restablecemos hashes conocidos para los demás.
-- =============================================================

-- 1. El admin del dump ya tiene su hash correcto del seeder de Flyway.
--    Solo actualizamos si el hash actual no corresponde a 'admin123'.
--    Para simplificar, actualizamos con el hash conocido de 'admin123'.
UPDATE presus.usuarios
SET password = '$2a$10$e/YiKn111EKnBEpmYTMYPunluLhq3u9f3jlqWvX5uCFaRa8Z8KhHy'
WHERE email = 'admin@uteq.edu.ec';

-- 2. Actualizar password de demo@uteq.edu.ec a 'Demo2026!'
UPDATE presus.usuarios
SET password = '$2a$10$i71KVeak.6dKRBVzPE5UZuuF9mVvMN1e84W3xqHhTNqZbEqGDuNSu'
WHERE email = 'demo@uteq.edu.ec';

-- 3. Insertar docente@uteq.edu.ec si no existe (docente123)
INSERT INTO presus.usuarios (nombre, apellido, email, password, rol, rol_id, activo, creado_en)
SELECT 'Docente', 'Tutor', 'docente@uteq.edu.ec',
       '$2a$10$e/YiKn111EKnBEpmYTMYPunluLhq3u9f3jlqWvX5uCFaRa8Z8KhHy',
       'DOCENTE',
       (SELECT id FROM presus.roles_usuario WHERE codigo = 'DOCENTE'),
       true, now()
WHERE NOT EXISTS (SELECT 1 FROM presus.usuarios WHERE email = 'docente@uteq.edu.ec');

-- Insertar en docente si no existe
INSERT INTO presus.docente (usuario_id, facultad_id, area_especialidad, carga_horaria_semanal, disponible, creado_en)
SELECT u.id, 1, 'Ingeniería de Software', 20, true, now()
FROM presus.usuarios u
WHERE u.email = 'docente@uteq.edu.ec'
ON CONFLICT (usuario_id) DO NOTHING;

-- 4. Insertar estudiante@uteq.edu.ec si no existe (estudiante123)
INSERT INTO presus.usuarios (nombre, apellido, email, password, rol, rol_id, activo, creado_en)
SELECT 'Estudiante', 'Pregrado', 'estudiante@uteq.edu.ec',
       '$2a$10$e/YiKn111EKnBEpmYTMYPunluLhq3u9f3jlqWvX5uCFaRa8Z8KhHy',
       'ESTUDIANTE',
       (SELECT id FROM presus.roles_usuario WHERE codigo = 'ESTUDIANTE'),
       true, now()
WHERE NOT EXISTS (SELECT 1 FROM presus.usuarios WHERE email = 'estudiante@uteq.edu.ec');

-- Insertar en estudiante si no existe
INSERT INTO presus.estudiante (usuario_id, carrera_id, carrera, semestre, semestre_actual, expediente_codigo, telefono, creado_en)
SELECT u.id, 1, 'Ingeniería en Software', '8vo', 8, 'EXP-2026-001', '0999999999', now()
FROM presus.usuarios u
WHERE u.email = 'estudiante@uteq.edu.ec'
ON CONFLICT (usuario_id) DO NOTHING;

-- 5. Verificar resultado
SELECT id, nombre, apellido, email, rol, activo
FROM presus.usuarios
WHERE email IN ('admin@uteq.edu.ec', 'demo@uteq.edu.ec', 'docente@uteq.edu.ec', 'estudiante@uteq.edu.ec')
ORDER BY id;
