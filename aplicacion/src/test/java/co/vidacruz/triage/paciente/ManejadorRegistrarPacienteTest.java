package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.comando.ComandoRegistrarPaciente;
import co.vidacruz.triage.paciente.comando.FabricaPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorRegistrarPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorRegistrarPacienteTest {
    private final RepositorioPaciente pacientes = mock(RepositorioPaciente.class);
    private final RepositorioAuditoria auditoria = mock(RepositorioAuditoria.class);
    private final RepositorioOutbox outbox = mock(RepositorioOutbox.class);
    private final AtomicInteger incrementos = new AtomicInteger();

    @Test void escribeLasTresPiezasYCuentaTrasElExito() {
        when(pacientes.guardar(any())).thenReturn(Mono.empty());
        when(auditoria.registrar(any(), anyString(), any(), anyString(), any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.empty());
        var manejador = manejador(error -> Mono.empty());
        StepVerifier.create(manejador.ejecutar(new ComandoRegistrarPaciente(datos())))
                .assertNext(paciente -> assertEquals(NivelTriage.NO_URGENTE, paciente.nivel())).verifyComplete();
        assertEquals(1, incrementos.get());
        InOrder orden = inOrder(pacientes, auditoria, outbox);
        orden.verify(pacientes).guardar(any(Paciente.class));
        orden.verify(auditoria).registrar(any(Paciente.class), anyString(), any(), anyString(), any());
        orden.verify(outbox).agregar(any(), anyString());
    }

    @Test void falloDeOutboxPropagaYNoIncrementaMetrica() {
        when(pacientes.guardar(any())).thenReturn(Mono.empty());
        when(auditoria.registrar(any(), anyString(), any(), anyString(), any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.error(new IllegalStateException("fallo")));
        StepVerifier.create(manejador(error -> Mono.empty()).ejecutar(new ComandoRegistrarPaciente(datos())))
                .expectError(IllegalStateException.class).verify();
        assertEquals(0, incrementos.get());
    }

    private ManejadorRegistrarPaciente manejador(java.util.function.Function<Throwable, Mono<Void>> ignorado) {
        EjecutorTransaccion transaccion = new EjecutorTransaccion() {
            public <T> Mono<T> ejecutar(java.util.function.Supplier<Mono<T>> trabajo) { return Mono.defer(trabajo); }
        };
        ContadorClasificaciones contador = nivel -> incrementos.incrementAndGet();
        return new ManejadorRegistrarPaciente(new ServicioClasificacionPaciente(), new FabricaPaciente(), pacientes,
                auditoria, outbox, transaccion, contador, Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC));
    }

    private DatosRegistroPaciente datos() {
        return new DatosRegistroPaciente(" Ana ", 40, 70, 120, 80, 98, new BigDecimal("36.5"), 2, 15, " Dolor ");
    }
}
