package co.vidacruz.triage.paciente.excepcion;

import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;

public final class ExcepcionPacienteNoEncontrado extends ExcepcionRecursoNoEncontrado {
    public ExcepcionPacienteNoEncontrado() {
        super("Paciente no encontrado");
    }
}
