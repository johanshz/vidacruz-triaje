package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import java.time.Instant;
import java.util.UUID;

public class FabricaPaciente {
    public static Paciente desde(DtoPaciente dto) {
        var datos = new DatosRegistroPaciente(dto.nombreCompleto(), dto.edad(), dto.frecuenciaCardiaca(),
                dto.presionSistolica(), dto.presionDiastolica(), dto.spo2(), dto.temperatura(),
                dto.dolor(), dto.gcs(), dto.quejaPrincipal());
        return new Paciente(dto.id(), datos, dto.nivel(), dto.estado(), dto.escalado(), dto.fechaRegistro(),
                dto.fechaEstado(), dto.fechaPrimeraConsulta());
    }

    public Paciente registrado(DatosRegistroPaciente datos, NivelTriage nivel, UUID id, Instant ahora) {
        DatosRegistroPaciente recortados = new DatosRegistroPaciente(datos.nombreCompleto().trim(), datos.edad(),
                datos.frecuenciaCardiaca(), datos.presionSistolica(), datos.presionDiastolica(), datos.spo2(),
                datos.temperatura(), datos.dolor(), datos.gcs(), datos.quejaPrincipal().trim());
        return new Paciente(id, recortados, nivel, EstadoPaciente.REGISTRADO, false, ahora, ahora, null);
    }

    public EventoPaciente eventoRegistrado(Paciente paciente, UUID idEvento, Instant ahora) {
        return new EventoPaciente(idEvento, TipoEventoPaciente.PACIENTE_REGISTRADO.valor(), EventoPaciente.VERSION_ACTUAL, ahora,
                new DatosEventoPaciente(paciente.id(), paciente.nivel().name(), null, paciente.estado().name(), paciente.escalado()));
    }
}
