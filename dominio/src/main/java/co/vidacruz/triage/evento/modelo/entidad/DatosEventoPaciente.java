package co.vidacruz.triage.evento.modelo.entidad;

import java.util.UUID;

public record DatosEventoPaciente(UUID idPaciente, String nivel, String estadoAnterior,
        String estadoNuevo, boolean escalado) { }
