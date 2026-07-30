
    create table presus.areas_tematicas (
        id serial not null,
        linea_investigacion_id integer not null,
        nombre varchar(150) not null,
        descripcion TEXT,
        primary key (id)
    );
