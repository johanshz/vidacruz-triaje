package co.vidacruz.triage.evento.adaptador.kafka;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.configuracion.ConfiguracionKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AdaptadorKafkaEventos implements PublicadorEventos {
    private final KafkaTemplate<String, String> kafka;
    private final Duration timeout;

    public AdaptadorKafkaEventos(KafkaTemplate<String, String> kafka,
            @Value("${vidacruz.outbox.timeout-envio:PT10S}") Duration timeout) {
        this.kafka = kafka;
        this.timeout = timeout;
    }

    @Override
    public Mono<Void> publicar(EventoOutbox evento) {
        return Mono.fromCallable(() -> kafka.send(ConfiguracionKafka.TOPIC_EVENTOS, evento.idPaciente().toString(), evento.carga()))
                .subscribeOn(Schedulers.boundedElastic()).flatMap(Mono::fromFuture).timeout(timeout).then();
    }
}
