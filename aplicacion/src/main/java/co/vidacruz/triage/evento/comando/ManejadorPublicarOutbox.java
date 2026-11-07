package co.vidacruz.triage.evento.comando;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ManejadorPublicarOutbox {
    private static final Logger LOG = Logger.getLogger(ManejadorPublicarOutbox.class.getName());
    private final RepositorioOutbox outbox;
    private final PublicadorEventos publicador;
    private final EjecutorTransaccion transaccion;
    private final ContadorEventosPendientes contador;
    private final int lote;

    public ManejadorPublicarOutbox(RepositorioOutbox outbox, PublicadorEventos publicador,
            EjecutorTransaccion transaccion, ContadorEventosPendientes contador, int lote) {
        this.outbox = outbox; this.publicador = publicador; this.transaccion = transaccion;
        this.contador = contador; this.lote = lote;
    }

    public Mono<Void> ejecutar() {
        return transaccion.ejecutar(() -> outbox.tomarCandadoPublicacion().flatMap(obtenido -> {
            if (!obtenido) return Mono.empty();
            return outbox.pendientes(lote).flatMapMany(Flux::fromIterable)
                    .concatMap(this::publicarYMarcar).then()
                    .onErrorResume(FalloKafkaPublicacion.class, error -> {
                        LOG.log(Level.WARNING, "Falló Kafka al publicar idEvento=" + error.idEvento, error);
                        return Mono.empty();
                    });
        }).then(outbox.contarPendientes()).doOnNext(contador::actualizar).then())
                .doOnError(error -> {
                    LOG.log(Level.SEVERE, "Falló PostgreSQL durante el ciclo del outbox", error);
                })
                .onErrorResume(error -> Mono.empty());
    }

    private Mono<Void> publicarYMarcar(EventoOutbox evento) {
        return publicador.publicar(evento)
                .onErrorMap(error -> new FalloKafkaPublicacion(evento.id().toString(), error))
                .then(outbox.marcarEnviado(evento));
    }

    private static final class FalloKafkaPublicacion extends RuntimeException {
        private final String idEvento;
        private FalloKafkaPublicacion(String idEvento, Throwable causa) {
            super(causa);
            this.idEvento = idEvento;
        }
    }
}
