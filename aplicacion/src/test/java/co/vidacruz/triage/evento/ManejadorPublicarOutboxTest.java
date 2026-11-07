package co.vidacruz.triage.evento;

import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorPublicarOutboxTest {
    @Test void detieneElLoteEnElPrimerFalloYDejaEventosPosterioresPendientes() {
        var primero = evento(1);
        var fallido = evento(2);
        var tercero = evento(3);
        var marcados = new ArrayList<EventoOutbox>();
        var enviados = new ArrayList<EventoOutbox>();
        RepositorioOutbox outbox = new RepositorioOutbox() {
            public Mono<Void> agregar(co.vidacruz.triage.evento.modelo.entidad.EventoPaciente evento, String llave) { return Mono.empty(); }
            public Mono<List<EventoOutbox>> pendientes(int limite) { return Mono.just(List.of(primero, fallido, tercero)); }
            public Mono<Boolean> tomarCandadoPublicacion() { return Mono.just(true); }
            public Mono<Void> marcarEnviado(EventoOutbox evento) { return Mono.fromRunnable(() -> marcados.add(evento)); }
            public Mono<Long> contarPendientes() { return Mono.just(2L); }
        };
        PublicadorEventos kafka = evento -> {
            enviados.add(evento);
            return evento.equals(fallido) ? Mono.error(new IllegalStateException("Kafka caído")) : Mono.empty();
        };
        AtomicInteger pendientes = new AtomicInteger();
        ContadorEventosPendientes contador = cantidad -> pendientes.set((int) cantidad);
        EjecutorTransaccion tx = new EjecutorTransaccion() {
            public <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo) { return Mono.defer(trabajo); }
        };
        StepVerifier.create(new ManejadorPublicarOutbox(outbox, kafka, tx, contador, 100).ejecutar()).verifyComplete();
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero), marcados);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero, fallido), enviados);
        org.junit.jupiter.api.Assertions.assertEquals(2, pendientes.get());
    }

    private EventoOutbox evento(long secuencia) {
        return new EventoOutbox(UUID.randomUUID(), secuencia, "paciente_registrado", UUID.randomUUID(), "{}");
    }
}
