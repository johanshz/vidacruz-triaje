package co.vidacruz.triage.evento.adaptador.kafka;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AdaptadorKafkaEventos implements PublicadorEventos {
    private final KafkaTemplate<String, String> kafka;
    public AdaptadorKafkaEventos(KafkaTemplate<String, String> kafka) { this.kafka = kafka; }

    @Override
    public Mono<Void> publicar(EventoOutbox evento) {
        return Mono.fromCallable(() -> kafka.send("triaje.eventos", evento.idPaciente().toString(), evento.carga()))
                .subscribeOn(Schedulers.boundedElastic()).flatMap(Mono::fromFuture).then();
    }
}
