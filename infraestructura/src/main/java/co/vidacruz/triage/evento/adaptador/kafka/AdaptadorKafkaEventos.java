package co.vidacruz.triage.evento.adaptador.kafka;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.configuracion.ConfiguracionKafka;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class AdaptadorKafkaEventos implements PublicadorEventos {
    static final String CABECERA_TRACEPARENT = "traceparent";
    private final KafkaTemplate<String, String> kafka;
    private final Duration timeout;

    public AdaptadorKafkaEventos(KafkaTemplate<String, String> kafka,
            @Value("${vidacruz.outbox.timeout-envio:PT10S}") Duration timeout) {
        this.kafka = kafka;
        this.timeout = timeout;
    }

    @Override
    public Mono<Void> publicar(EventoOutbox evento) {
        return Mono.fromCallable(() -> kafka.send(crearRegistro(evento)))
                .subscribeOn(Schedulers.boundedElastic()).flatMap(Mono::fromFuture).timeout(timeout).then();
    }

    private ProducerRecord<String, String> crearRegistro(EventoOutbox evento) {
        var registro = new ProducerRecord<String, String>(ConfiguracionKafka.TOPIC_EVENTOS,
                evento.idPaciente().toString(), evento.carga());
        if (evento.traceparent() != null) {
            registro.headers().add(CABECERA_TRACEPARENT, evento.traceparent().getBytes(StandardCharsets.UTF_8));
        }
        return registro;
    }
}
