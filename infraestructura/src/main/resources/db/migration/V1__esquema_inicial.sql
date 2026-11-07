CREATE TABLE paciente (
    id UUID PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    edad SMALLINT NOT NULL,
    frecuencia_cardiaca SMALLINT NOT NULL,
    presion_sistolica SMALLINT NOT NULL,
    presion_diastolica SMALLINT NOT NULL,
    spo2 SMALLINT NOT NULL,
    temperatura NUMERIC(3, 1) NOT NULL,
    dolor SMALLINT NOT NULL,
    gcs SMALLINT NOT NULL,
    queja_principal VARCHAR(500) NOT NULL,
    nivel VARCHAR(20) NOT NULL CHECK (nivel IN ('CRITICO', 'URGENTE', 'URGENTE_2', 'NO_URGENTE', 'NO_URGENTE_2')),
    sla_minutos INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('REGISTRADO', 'EN_CONSULTA', 'ALTA')),
    escalado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_registro TIMESTAMPTZ NOT NULL,
    fecha_estado TIMESTAMPTZ NOT NULL,
    fecha_primera_consulta TIMESTAMPTZ
);

CREATE INDEX idx_paciente_estado_escalado_fecha ON paciente (estado, escalado, fecha_registro);
CREATE INDEX idx_paciente_fecha_registro ON paciente (fecha_registro);

CREATE TABLE auditoria (
    id UUID PRIMARY KEY,
    paciente_id UUID NOT NULL REFERENCES paciente (id),
    tipo_evento VARCHAR(30) NOT NULL CHECK (tipo_evento IN ('paciente_registrado', 'paciente_en_consulta', 'paciente_alta', 'paciente_escalado')),
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20),
    ocurrido_en TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auditoria_paciente_id ON auditoria (paciente_id);

CREATE FUNCTION impedir_modificacion_auditoria() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'auditoria es inmutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_inmutable_filas
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_auditoria();

CREATE TRIGGER auditoria_inmutable_truncate
    BEFORE TRUNCATE ON auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION impedir_modificacion_auditoria();

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    secuencia BIGINT GENERATED ALWAYS AS IDENTITY,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('paciente_registrado', 'paciente_en_consulta', 'paciente_alta', 'paciente_escalado')),
    llave VARCHAR(36) NOT NULL,
    carga JSONB NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    enviado_en TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pendiente_secuencia ON outbox (secuencia) WHERE enviado_en IS NULL;
