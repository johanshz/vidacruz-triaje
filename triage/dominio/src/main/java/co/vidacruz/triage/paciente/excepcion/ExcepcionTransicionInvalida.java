package co.vidacruz.triage.paciente.excepcion;

import co.vidacruz.triage.comun.excepcion.ExcepcionConflicto;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;

public final class ExcepcionTransicionInvalida extends ExcepcionConflicto {
    public ExcepcionTransicionInvalida(EstadoPaciente anterior, EstadoPaciente nuevo) {
        super("No se puede pasar de " + anterior + " a " + nuevo);
    }
}
