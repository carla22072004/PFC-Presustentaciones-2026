-- Deja UNA sola "Ana Medina" docente: la de usuario41006@uteq.edu.ec (docente_id = 3,
-- usuario_id = 92439). Los otros 245 docentes homónimos NO se pueden borrar -- están
-- referenciados por 769 tutores, 768 evaluadores y 1155 miembros_tribunal del volumen
-- sintético. Se les cambia el nombre a combinaciones variadas de los mismos arreglos que
-- usó scripts/generar-volumen-datos.sql, evitando de forma explícita el par ('Ana','Medina').

BEGIN;

DO $$
DECLARE
  nombres   TEXT[] := ARRAY['Juan','María','Carlos','Ana','Luis','Sofía','Pedro','Laura','Diego','Valentina',
                             'Miguel','Camila','Andrés','Daniela','José','Gabriela','Fernando','Paula','Ricardo','Isabella',
                             'Jorge','Mariana','Alberto','Carolina','Eduardo','Andrea','Francisco','Patricia','Manuel','Verónica',
                             'Rafael','Cristina','Sergio','Alejandra','Roberto','Natalia','Óscar','Valeria','Iván','Lucía'];
  apellidos TEXT[] := ARRAY['González','Rodríguez','Pérez','López','Martínez','Sánchez','Ramírez','Torres','Flores','Rivera',
                             'Gómez','Díaz','Reyes','Morales','Cruz','Ortiz','Gutiérrez','Chávez','Ramos','Vargas',
                             'Castro','Jiménez','Romero','Álvarez','Mendoza','Ruiz','Herrera','Medina','Aguilar','Vega',
                             'Castillo','Guerrero','Silva','Rojas','Núñez','Delgado','Peña','Cabrera','Salazar','Paredes'];
  r          RECORD;
  ni         INT;
  ai         INT;
  nuevo_nom  TEXT;
  nuevo_ape  TEXT;
  n_cambiados INT := 0;
BEGIN
  FOR r IN
    SELECT d.id AS docente_id, d.usuario_id
    FROM presus.docente d
    JOIN presus.usuarios u ON u.id = d.usuario_id
    WHERE u.nombre = 'Ana' AND u.apellido = 'Medina'
      AND d.id <> 3                       -- conservar la de usuario41006@
    ORDER BY d.id
  LOOP
    -- multiplicadores coprimos con 40 -> buena dispersión, determinista por id
    ni := (r.docente_id * 7)  % 40;
    ai := (r.docente_id * 13) % 40;
    nuevo_nom := nombres[ni + 1];
    nuevo_ape := apellidos[ai + 1];

    -- nunca volver a generar exactamente "Ana Medina"
    IF nuevo_nom = 'Ana' AND nuevo_ape = 'Medina' THEN
      nuevo_ape := apellidos[((ai + 1) % 40) + 1];
    END IF;

    UPDATE presus.usuarios
       SET nombre = nuevo_nom, apellido = nuevo_ape
     WHERE id = r.usuario_id;

    n_cambiados := n_cambiados + 1;
  END LOOP;

  RAISE NOTICE 'Docentes renombrados: %', n_cambiados;
END $$;

-- Verificación: debe quedar exactamente 1
SELECT d.id AS docente_id, d.usuario_id, u.email, u.nombre, u.apellido, u.activo, d.disponible
FROM presus.docente d
JOIN presus.usuarios u ON u.id = d.usuario_id
WHERE u.nombre = 'Ana' AND u.apellido = 'Medina';

COMMIT;
