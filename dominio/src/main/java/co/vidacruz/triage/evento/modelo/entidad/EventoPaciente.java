package co.vidacruz.triage.evento.modelo.entidad;

import java.time.Instant;
import java.util.UUID;

public record EventoPaciente(UUID idEvento, String tipo, int version, Instant ocurridoEn, DatosEventoPaciente datos) {
    public EventoPaciente {
        if (version != 1) throw new IllegalArgumentException("La versión del evento debe ser 1");
    }
}
