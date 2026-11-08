package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.vidacruz.triage.paciente.excepcion.ExcepcionTransicionInvalida;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import org.junit.jupiter.api.Test;

class EstadoPacienteTest {
    @Test void permiteSoloLasTransicionesDefinidas() {
        assertDoesNotThrow(() -> EstadoPaciente.REGISTRADO.validarTransicion(EstadoPaciente.EN_CONSULTA));
        assertDoesNotThrow(() -> EstadoPaciente.REGISTRADO.validarTransicion(EstadoPaciente.ALTA));
        assertDoesNotThrow(() -> EstadoPaciente.EN_CONSULTA.validarTransicion(EstadoPaciente.ALTA));
    }

    @Test void rechazaTodaTransicionFueraDeLaMatrizConMensajeContractual() {
        EstadoPaciente[][] invalidas = {
                {EstadoPaciente.REGISTRADO, EstadoPaciente.REGISTRADO},
                {EstadoPaciente.EN_CONSULTA, EstadoPaciente.REGISTRADO},
                {EstadoPaciente.EN_CONSULTA, EstadoPaciente.EN_CONSULTA},
                {EstadoPaciente.ALTA, EstadoPaciente.REGISTRADO},
                {EstadoPaciente.ALTA, EstadoPaciente.EN_CONSULTA},
                {EstadoPaciente.ALTA, EstadoPaciente.ALTA}
        };

        for (EstadoPaciente[] transicion : invalidas) {
            var error = assertThrows(ExcepcionTransicionInvalida.class,
                    () -> transicion[0].validarTransicion(transicion[1]));
            assertEquals("No se puede pasar de " + transicion[0] + " a " + transicion[1], error.getMessage());
        }
    }
}
