package co.vidacruz.triage.evento;

import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import co.vidacruz.triage.evento.comando.DependenciasPublicacionOutbox;
import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
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
        var debeFallar = new AtomicBoolean(true);
        RepositorioOutbox outbox = new RepositorioOutbox() {
            public Mono<Void> agregar(co.vidacruz.triage.evento.modelo.entidad.EventoPaciente evento, String llave) { return Mono.empty(); }
            public Mono<List<EventoOutbox>> pendientes(int limite) {
                return Mono.just(List.of(primero, fallido, tercero).stream().filter(evento -> !marcados.contains(evento)).toList());
            }
            public Mono<Void> marcarEnviado(EventoOutbox evento) { return Mono.fromRunnable(() -> marcados.add(evento)); }
            public Mono<Long> contarPendientes() { return Mono.fromSupplier(() -> (long) (3 - marcados.size())); }
        };
        PublicadorEventos kafka = evento -> {
            enviados.add(evento);
            return evento.equals(fallido) && debeFallar.get()
                    ? Mono.error(new IllegalStateException("Kafka caído")) : Mono.empty();
        };
        AtomicInteger pendientes = new AtomicInteger();
        ContadorEventosPendientes contador = cantidad -> pendientes.set((int) cantidad);
        EjecutorTransaccion tx = new EjecutorTransaccion() {
            public <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo) { return Mono.defer(trabajo); }
        };
        var dependencias = new DependenciasPublicacionOutbox(outbox, () -> Mono.just(true), kafka, tx, contador, 100);
        StepVerifier.create(new ManejadorPublicarOutbox(dependencias).ejecutar()).verifyComplete();
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero), marcados);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero, fallido), enviados);
        org.junit.jupiter.api.Assertions.assertEquals(2, pendientes.get());

        debeFallar.set(false);
        StepVerifier.create(new ManejadorPublicarOutbox(dependencias).ejecutar()).verifyComplete();
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero, fallido, tercero), marcados);
        org.junit.jupiter.api.Assertions.assertEquals(List.of(primero, fallido, fallido, tercero), enviados);
        org.junit.jupiter.api.Assertions.assertEquals(0, pendientes.get());
    }

    @Test void dosCiclosConcurrentesPublicanElLoteUnaSolaVez() {
        var evento = evento(1);
        var candadoTomado = new AtomicBoolean();
        var envios = new AtomicInteger();
        var marcados = new AtomicInteger();
        RepositorioOutbox outbox = new RepositorioOutbox() {
            @Override
            public Mono<Void> agregar(co.vidacruz.triage.evento.modelo.entidad.EventoPaciente evento, String llave) {
                return Mono.empty();
            }

            @Override
            public Mono<List<EventoOutbox>> pendientes(int limite) {
                return Mono.just(List.of(evento));
            }

            @Override
            public Mono<Void> marcarEnviado(EventoOutbox pendiente) {
                return Mono.fromRunnable(marcados::incrementAndGet);
            }

            @Override
            public Mono<Long> contarPendientes() {
                return Mono.just(0L);
            }
        };
        PublicadorEventos publicador = pendiente -> Mono.fromRunnable(envios::incrementAndGet);
        EjecutorTransaccion transaccion = new EjecutorTransaccion() {
            @Override
            public <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo) {
                return Mono.defer(trabajo);
            }
        };
        var contador = (ContadorEventosPendientes) cantidad -> { };
        var candado = (co.vidacruz.triage.evento.puerto.repositorio.CandadoPublicacion)
                () -> Mono.fromSupplier(() -> candadoTomado.compareAndSet(false, true));
        var dependencias = new DependenciasPublicacionOutbox(outbox, candado, publicador, transaccion, contador, 10);
        var publicadorOutbox = new ManejadorPublicarOutbox(dependencias);

        StepVerifier.create(Mono.when(publicadorOutbox.ejecutar(), publicadorOutbox.ejecutar())).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, envios.get());
        org.junit.jupiter.api.Assertions.assertEquals(1, marcados.get());
    }

    private EventoOutbox evento(long secuencia) {
        return new EventoOutbox(UUID.randomUUID(), secuencia, "paciente_registrado", UUID.randomUUID(), "{}", null);
    }
}
