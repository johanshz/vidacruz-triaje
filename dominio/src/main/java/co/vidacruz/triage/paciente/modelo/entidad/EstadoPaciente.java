package co.vidacruz.triage.paciente.modelo.entidad;

import co.vidacruz.triage.paciente.excepcion.ExcepcionTransicionInvalida;

public enum EstadoPaciente {
    REGISTRADO, EN_CONSULTA, ALTA;

    public void validarTransicion(EstadoPaciente destino) {
        boolean permitida = (this == REGISTRADO && (destino == EN_CONSULTA || destino == ALTA))
                || (this == EN_CONSULTA && destino == ALTA);
        if (!permitida) throw new ExcepcionTransicionInvalida(this, destino);
    }
}
