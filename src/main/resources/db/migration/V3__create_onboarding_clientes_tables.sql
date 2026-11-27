-- =========================================================================================
-- Migración V3: Tablas para el Onboarding de Clientes, Cuentaas, Saldos,
-- Login Seguro (BCypt + Biometría facial) y Control de Sesiones
-- =========================================================================================

-- 1. Tabla Clientes
CREATE TABLE IF NOT EXISTS clientes (
    id_cliente              BIGSERIAL PRIMARY KEY,
    nombre                  TEXT NOT NULL,
    segundo_nombre          TEXT,
    apellido_paterno        TEXT NOT NULL,
    apellido_materno        TEXT NOT NULL,
    fecha_nacimiento        DATE NOT NULL,
    curp                    TEXT NOT NULL,
    rfc                     TEXT NOT NULL,
    sexo                    TEXT NOT NULL,
    nacionalidad            TEXT NOT NULL,
    estado_civil            TEXT NOT NULL,
    correo_electronico      TEXT NOT NULL,
    telefono_movil          TEXT NOT NULL,
    telefono_alternativo    TEXT,
    ocupacion               TEXT NOT NULL,
    empresa                 TEXT NOT NULL,
    ingreso_mensual         DOUBLE PRECISION NOT NULL,
    activo                  BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_modificacion      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico) 
);

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes (curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes (rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes (correo_electronico);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes (activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion ON clientes (fecha_creacion);

-- 2. Tabla Domicilios
CREATE TABLE IF NOT EXISTS domicilios (
    id_domicilio            BIGSERIAL PRIMARY KEY,
    id_cliente              BIGINT NOT NULL,
    calle                   TEXT NOT NULL,
    numero_exterior         TEXT NOT NULL,
    numero_interior         TEXT,
    colonia                 TEXT NOT NULL,
    municipio               TEXT NOT NULL,
    estado                  TEXT NOT NULL,
    codigo_postal           TEXT NOT NULL,
    pais                    TEXT NOT NULL,
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (id_cliente) 
        REFERENCES clientes (id_cliente) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_domicilios_id_cliente ON domicilios (id_cliente);

-- 3. Tabla Cuentas
CREATE TABLE IF NOT EXISTS cuentas (
    id_cuenta               BIGSERIAL PRIMARY KEY,
    id_cliente              BIGINT NOT NULL,
    numero_cuenta           TEXT NOT NULL,
    tipo_cuenta             TEXT NOT NULL DEFAULT 'DEBITO',
    estatus                 TEXT NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura          TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_modificacion      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (id_cliente) 
        REFERENCES clientes (id_cliente) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas (numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_id_cliente ON cuentas (id_cliente);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus ON cuentas (estatus);

-- 4. Tabla Saldos
CREATE TABLE IF NOT EXISTS saldos (
    id_saldo                    BIGSERIAL PRIMARY KEY,
    id_cuenta                   BIGINT NOT NULL,
    saldo_disponible            DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    saldo_contable              DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    fecha_ultima_actualizacion  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saldos_id_cuenta UNIQUE (id_cuenta),
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (id_cuenta) 
        REFERENCES cuentas (id_cuenta) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_saldos_id_cuenta ON saldos (id_cuenta);

-- 5. Tabla Usuarios Login (Contraseña cifrada + Biometría Facial)
CREATE TABLE IF NOT EXISTS usuarios_login (
    id_usuario                      BIGSERIAL PRIMARY KEY,
    id_cliente                      BIGINT NOT NULL,
    username                        TEXT NOT NULL,
    password_hash                   TEXT NOT NULL,
    biometrico_facial_embedding     TEXT,
    activo                          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion                  TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion             TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_id_cliente UNIQUE (id_cliente),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (id_cliente) 
        REFERENCES clientes (id_cliente) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios_login (username);
CREATE INDEX IF NOT EXISTS idx_usuarios_id_cliente ON usuarios_login (id_cliente);

-- 6. Tabla Sesiones Login (Bandera activa + Timeout de inactividad 5 min)
CREATE TABLE IF NOT EXISTS sesiones_login (
    id_sesion               BIGSERIAL PRIMARY KEY,
    id_usuario              BIGINT NOT NULL,
    token_sesion            TEXT NOT NULL,
    activa                  BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso           TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_inicio            TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_expiracion        TIMESTAMP NOT NULL,
    CONSTRAINT uq_sesiones_token UNIQUE (token_sesion),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (id_usuario) 
        REFERENCES usuarios_login (id_usuario) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_sesiones_token ON sesiones_login (token_sesion);
CREATE INDEX IF NOT EXISTS idx_sesiones_id_usuario ON sesiones_login (id_usuario);
CREATE INDEX IF NOT EXISTS idx_sesiones_activa ON sesiones_login (activa);
