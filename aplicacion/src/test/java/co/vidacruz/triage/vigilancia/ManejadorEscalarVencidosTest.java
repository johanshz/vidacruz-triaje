package co.vidacruz.triage.vigilancia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.vigilancia.comando.ComandoEscalarVencidos;
import co.vidacruz.triage.vigilancia.comando.DependenciasEscalamiento;
import co.vidacruz.triage.vigilancia.comando.ManejadorEscalarVencidos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.ContadorEscalamientos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.vigilancia.servicio.ServicioEscalamiento;
import co.vidacruz.triage.vigilancia.modelo.entidad.PacienteEscalado;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorEscalarVencidosTest {
    private static final Instant AHORA = Instant.parse("2026-11-06T10:01:00Z");
    private final RepositorioEscalamiento repositorio = mock(RepositorioEscalamiento.class);
    private final RepositorioAuditoria auditoria = mock(RepositorioAuditoria.class);
    private final RepositorioOutbox outbox = mock(RepositorioOutbox.class);
    private final AtomicInteger incrementos = new AtomicInteger();

    @Test
    void registraAuditoriaYEventoConElMismoInstante() {
        UUID id = UUID.randomUUID();
        when(repositorio.escalarVencidos(AHORA)).thenReturn(Flux.just(new PacienteEscalado(id, NivelTriage.CRITICO)));
        when(auditoria.registrar(any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(manejador().ejecutar(new ComandoEscalarVencidos())).verifyComplete();

        var auditoriaCaptor = org.mockito.ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).registrar(auditoriaCaptor.capture());
        assertEquals(AHORA, auditoriaCaptor.getValue().ocurridoEn());
        assertEquals("REGISTRADO", auditoriaCaptor.getValue().estadoAnterior());
        assertEquals("REGISTRADO", auditoriaCaptor.getValue().estadoNuevo());
        var eventoCaptor = org.mockito.ArgumentCaptor.forClass(EventoPaciente.class);
        verify(outbox).agregar(eventoCaptor.capture(), org.mockito.ArgumentMatchers.eq(id.toString()));
        assertEquals("paciente_escalado", eventoCaptor.getValue().tipo());
        assertEquals(AHORA, eventoCaptor.getValue().ocurridoEn());
        assertTrue(eventoCaptor.getValue().datos().escalado());
        assertEquals(1, incrementos.get());
    }

    @Test
    void falloDeOutboxPropagaYNoIncrementaElContador() {
        when(repositorio.escalarVencidos(AHORA)).thenReturn(Flux.just(
                new PacienteEscalado(UUID.randomUUID(), NivelTriage.URGENTE)));
        when(auditoria.registrar(any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.error(new IllegalStateException("fallo")));

        StepVerifier.create(manejador().ejecutar(new ComandoEscalarVencidos()))
                .expectError(IllegalStateException.class).verify();

        assertEquals(0, incrementos.get());
    }

    @Test
    void cicloSinPacientesNoEscribeNiIncrementaContador() {
        when(repositorio.escalarVencidos(AHORA)).thenReturn(Flux.empty());

        StepVerifier.create(manejador().ejecutar(new ComandoEscalarVencidos())).verifyComplete();

        verify(auditoria, never()).registrar(any());
        verify(outbox, never()).agregar(any(), anyString());
        assertEquals(0, incrementos.get());
    }

    private ManejadorEscalarVencidos manejador() {
        EjecutorTransaccion transaccion = new EjecutorTransaccion() {
            @Override
            public <T> Mono<T> ejecutar(java.util.function.Supplier<Mono<T>> trabajo) {
                return Mono.defer(trabajo);
            }
        };
        ContadorEscalamientos contador = incrementos::incrementAndGet;
        var contexto = new DependenciasEscalamiento.ContextoEscalamiento(new ServicioEscalamiento(), contador,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        return new ManejadorEscalarVencidos(new DependenciasEscalamiento(repositorio, auditoria, outbox,
                transaccion, contexto));
    }
}
