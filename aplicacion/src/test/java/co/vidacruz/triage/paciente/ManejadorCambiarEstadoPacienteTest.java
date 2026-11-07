package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.triage.paciente.comando.ComandoCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.DependenciasCambioEstadoPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.RegistrarCambioPaciente;
import co.vidacruz.triage.paciente.excepcion.ExcepcionPacienteNoEncontrado;
import co.vidacruz.triage.paciente.excepcion.ExcepcionTransicionInvalida;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorCambiarEstadoPacienteTest {
    private static final UUID ID = UUID.randomUUID();
    private static final Instant AHORA = Instant.parse("2026-11-06T10:07:00Z");
    private final DaoPaciente dao = mock(DaoPaciente.class);
    private final RepositorioPaciente pacientes = mock(RepositorioPaciente.class);
    private final RegistrarCambioPaciente registrarCambio = mock(RegistrarCambioPaciente.class);

    @Test void cambiaEstadoYRegistraElEventoDespuesDeActualizarLaFicha() {
        var actual = dto(EstadoPaciente.REGISTRADO, Instant.parse("2026-11-06T10:00:00Z"), null);
        var actualizado = dto(EstadoPaciente.EN_CONSULTA, AHORA, AHORA);
        when(dao.consultar(eq(ID), any())).thenReturn(Mono.just(actual), Mono.just(actualizado));
        when(pacientes.cambiarEstado(eq(ID), any(), eq("REGISTRADO"))).thenReturn(Mono.just(true));
        when(registrarCambio.registrar(any(), any(), eq(EstadoPaciente.REGISTRADO))).thenReturn(Mono.empty());

        StepVerifier.create(manejador().ejecutar(new ComandoCambiarEstadoPaciente(ID, EstadoPaciente.EN_CONSULTA)))
                .assertNext(resultado -> assertEquals(EstadoPaciente.EN_CONSULTA, resultado.estado()))
                .verifyComplete();
        verify(registrarCambio).registrar(any(), any(), eq(EstadoPaciente.REGISTRADO));
    }

    @Test void ausenciaDePacienteUsaLaExcepcionContractual() {
        when(dao.consultar(eq(ID), any())).thenReturn(Mono.empty());

        StepVerifier.create(manejador().ejecutar(new ComandoCambiarEstadoPaciente(ID, EstadoPaciente.ALTA)))
                .expectError(ExcepcionPacienteNoEncontrado.class).verify();
    }

    @Test void estadoTerminalRechazaLaTransicionAntesDeEscribir() {
        when(dao.consultar(eq(ID), any())).thenReturn(Mono.just(dto(EstadoPaciente.ALTA, AHORA, null)));

        StepVerifier.create(manejador().ejecutar(new ComandoCambiarEstadoPaciente(ID, EstadoPaciente.EN_CONSULTA)))
                .expectErrorMatches(error -> error instanceof ExcepcionTransicionInvalida
                        && error.getMessage().equals("No se puede pasar de ALTA a EN_CONSULTA"))
                .verify();
        verify(pacientes, never()).cambiarEstado(any(), any(), anyString());
        verify(registrarCambio, never()).registrar(any(), any(), any());
    }

    @Test void conflictoOptimistaProduceExcepcionDeTransicionContractual() {
        when(dao.consultar(eq(ID), any())).thenReturn(Mono.just(dto(EstadoPaciente.REGISTRADO, AHORA, null)));
        when(pacientes.cambiarEstado(eq(ID), any(), eq("REGISTRADO"))).thenReturn(Mono.just(false));

        StepVerifier.create(manejador().ejecutar(new ComandoCambiarEstadoPaciente(ID, EstadoPaciente.EN_CONSULTA)))
                .expectErrorMatches(error -> error instanceof ExcepcionTransicionInvalida
                        && error.getMessage().equals("No se puede pasar de REGISTRADO a EN_CONSULTA"))
                .verify();
        verify(registrarCambio, never()).registrar(any(), any(), any());
    }

    private ManejadorCambiarEstadoPaciente manejador() {
        EjecutorTransaccion transaccion = new EjecutorTransaccion() {
            @Override
            public <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo) {
                return Mono.defer(trabajo);
            }
        };
        var dependencias = new DependenciasCambioEstadoPaciente(dao, pacientes, registrarCambio, transaccion,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        return new ManejadorCambiarEstadoPaciente(dependencias);
    }

    private DtoPaciente dto(EstadoPaciente estado, Instant fechaEstado, Instant primeraConsulta) {
        return new DtoPaciente(ID, "Paciente", 40, 70, 120, 80, 98, new BigDecimal("36.5"), 2, 15,
                "Dolor", NivelTriage.NO_URGENTE, 60, estado, false,
                Instant.parse("2026-11-06T10:00:00Z"), fechaEstado, primeraConsulta, 0);
    }
}
