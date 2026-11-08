package co.vidacruz.asistente.urgencias.consumidor;

import co.vidacruz.asistente.urgencias.comando.ComandoActualizarUrgencias;
import co.vidacruz.asistente.urgencias.comando.manejador.ManejadorActualizarUrgencias;
import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.modelo.dto.EventoTriaje;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import java.util.Set;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.SerializationUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Component
public class ConsumidorEventosTriaje {
    private static final String TOPIC_DLT = "triaje.eventos.DLT";
    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorEventosTriaje.class);
    private static final Propagator.Getter<Headers> LECTOR_ENCABEZADOS = (encabezados, clave) -> {
        Header encabezado = encabezados.lastHeader(clave);
        return encabezado == null ? "" : new String(encabezado.value(), java.nio.charset.StandardCharsets.UTF_8);
    };
    private static final Set<String> TIPOS_CONOCIDOS = Set.of(
            "paciente_registrado", "paciente_en_consulta", "paciente_alta", "paciente_escalado");
    private final ManejadorActualizarUrgencias manejador;
    private final Tracer tracer;
    private final Propagator propagator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RetryBackoffSpec reintentos;

    public ConsumidorEventosTriaje(
            ManejadorActualizarUrgencias manejador,
            Tracer tracer,
            Propagator propagator,
            KafkaTemplate<String, Object> kafkaTemplate,
            RetryBackoffSpec reintentos) {
        this.manejador = manejador;
        this.tracer = tracer;
        this.propagator = propagator;
        this.kafkaTemplate = kafkaTemplate;
        this.reintentos = reintentos;
    }

    @KafkaListener(topics = "triaje.eventos", containerFactory = "fabricaContenedoresKafka")
    public Mono<Void> consumir(ConsumerRecord<String, EventoTriaje> registro) {
        if (registro == null) {
            throw new ExcepcionEventoInvalido("Evento vacío");
        }
        if (registro.value() == null
                || registro.headers().lastHeader(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER) != null) {
            throw new ExcepcionEventoInvalido("Evento vacío");
        }
        return consumir(registro.value(), registro);
    }

    public Mono<Void> consumir(EventoTriaje evento) {
        return consumir(evento, null);
    }

    private Mono<Void> consumir(EventoTriaje evento, ConsumerRecord<String, EventoTriaje> registro) {
        if (evento == null) {
            return Mono.error(new ExcepcionEventoInvalido("Evento vacío"));
        }
        if (evento.version() == null || evento.version() != 1
                || (evento.tipo() != null && !evento.tipo().isBlank() && !TIPOS_CONOCIDOS.contains(evento.tipo()))) {
            LOG.warn("Evento de triaje ignorado: idEvento={}, tipo={}, version={}",
                    evento.idEvento(), evento.tipo(), evento.version());
            return Mono.empty();
        }
        registrarEvento(evento, Optional.ofNullable(registro));
        String idPaciente = evento.datos() == null ? null : evento.datos().idPaciente();
        String nivel = evento.datos() == null ? null : evento.datos().nivel();
        Mono<Void> procesamiento = Mono.defer(() -> manejador.manejar(
                new ComandoActualizarUrgencias(evento.tipo(), idPaciente, nivel)));
        if (registro == null) {
            return procesamiento;
        }
        return procesamiento
                .retryWhen(reintentos
                        .filter(error -> !(error instanceof ExcepcionEventoInvalido))
                        .onRetryExhaustedThrow((configuracion, senal) -> senal.failure()))
                .onErrorResume(error -> publicarEnDlt(registro, error));
    }

    private Mono<Void> publicarEnDlt(ConsumerRecord<String, EventoTriaje> origen, Throwable error) {
        ProducerRecord<String, Object> fallido = new ProducerRecord<>(
                TOPIC_DLT, origen.partition(), origen.key(), origen.value());
        for (Header encabezado : origen.headers()) {
            fallido.headers().add(new RecordHeader(encabezado.key(), encabezado.value()));
        }
        LOG.warn("Evento enviado a la DLT tras fallar su procesamiento: topic={}, partition={}, offset={}",
                origen.topic(), origen.partition(), origen.offset(), error);
        return Mono.fromFuture(kafkaTemplate.send(fallido)).then();
    }

    private void registrarEvento(EventoTriaje evento, Optional<ConsumerRecord<String, EventoTriaje>> registro) {
        Span span = registro.isEmpty()
                ? tracer.currentSpan()
                : propagator.extract(registro.orElseThrow().headers(), LECTOR_ENCABEZADOS)
                        .name("consumir triaje.eventos")
                        .kind(Span.Kind.CONSUMER)
                        .start();
        String traceId = traceIdDesdeEncabezado(registro).or(() -> Optional.ofNullable(span)
                .map(Span::context).map(contexto -> contexto.traceId())).orElse("desconocido");
        if (span == null) {
            LOG.info("Evento de triaje recibido: tipo={}, traceId={}", evento.tipo(),
                    traceId == null ? "desconocido" : traceId);
            return;
        }
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            LOG.info("Evento de triaje recibido: tipo={}, traceId={}", evento.tipo(), traceId);
        } finally {
            if (registro.isPresent()) span.end();
        }
    }

    private Optional<String> traceIdDesdeEncabezado(Optional<ConsumerRecord<String, EventoTriaje>> registro) {
        return registro.flatMap(origen -> Optional.ofNullable(origen.headers().lastHeader("traceparent")))
                .map(traceparent -> new String(traceparent.value(), java.nio.charset.StandardCharsets.UTF_8).split("-"))
                .filter(partes -> partes.length == 4 && partes[1].matches("[0-9a-fA-F]{32}"))
                .map(partes -> partes[1]);
    }
}
