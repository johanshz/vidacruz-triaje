package co.vidacruz.triage.evento.modelo.entidad;

import java.time.Instant;
import java.util.UUID;

public record EventoPaciente(UUID idEvento, String tipo, int version, Instant ocurridoEn, DatosEventoPaciente datos) {
    public static final int VERSION_ACTUAL = 1;

    public EventoPaciente {
        if (version != VERSION_ACTUAL) throw new IllegalArgumentException("La versión del evento debe ser " + VERSION_ACTUAL);
    }
}
