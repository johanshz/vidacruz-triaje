package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import java.time.Instant;
import java.util.Objects;

public class FabricaPaciente {
    public static Paciente desde(DtoPaciente dto) {
        var datos = new DatosRegistroPaciente(dto.nombreCompleto(), dto.edad(), dto.frecuenciaCardiaca(),
                dto.presionSistolica(), dto.presionDiastolica(), dto.spo2(), dto.temperatura(),
                dto.dolor(), dto.gcs(), dto.quejaPrincipal());
        return new Paciente(dto.id(), datos, dto.nivel(), dto.slaMinutos(), dto.estado(), dto.escalado(), dto.fechaRegistro(),
                dto.fechaEstado(), dto.fechaPrimeraConsulta());
    }

    public static Paciente registrado(EntradaPacienteRegistrado entrada) {
        Objects.requireNonNull(entrada, "entrada");
        DatosRegistroPaciente datos = Objects.requireNonNull(entrada.datos(), "datos");
        String nombre = textoRequerido(datos.nombreCompleto(), "nombreCompleto");
        String queja = textoRequerido(datos.quejaPrincipal(), "quejaPrincipal");
        DatosRegistroPaciente recortados = new DatosRegistroPaciente(nombre, datos.edad(),
                datos.frecuenciaCardiaca(), datos.presionSistolica(), datos.presionDiastolica(), datos.spo2(),
                datos.temperatura(), datos.dolor(), datos.gcs(), queja);
        return Paciente.registrado(entrada.id(), recortados, entrada.nivel(), entrada.slaMinutos(), entrada.ahora());
    }

    public static EventoPaciente eventoRegistrado(Paciente paciente, java.util.UUID idEvento, Instant ahora) {
        return new EventoPaciente(idEvento, TipoEventoPaciente.PACIENTE_REGISTRADO.valor(), EventoPaciente.VERSION_ACTUAL, ahora,
                new DatosEventoPaciente(paciente.id(), paciente.nivel().name(), null, paciente.estado().name(), paciente.escalado()));
    }

    private static String textoRequerido(String valor, String campo) {
        if (valor == null) {
            throw new ExcepcionValidacion(campo, "El campo es obligatorio");
        }
        return valor.trim();
    }
}
