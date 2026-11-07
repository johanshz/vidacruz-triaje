package co.vidacruz.triage.alerta.consumidor;

import co.vidacruz.triage.alerta.comando.ComandoEmitirAlerta;
import co.vidacruz.triage.alerta.comando.ManejadorEmitirAlerta;
import co.vidacruz.triage.evento.configuracion.ConfiguracionKafka;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
public class ConsumidorAlertasKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumidorAlertasKafka.class);
    private final ObjectMapper mapper;
    private final ManejadorEmitirAlerta manejador;
    private final MemoriaIdsEventoProcesados idsProcesados;
    private final KafkaTemplate<String, String> kafka;
    private final Duration timeoutDlt;

    public ConsumidorAlertasKafka(ObjectMapper mapper, ManejadorEmitirAlerta manejador,
            MemoriaIdsEventoProcesados idsProcesados, KafkaTemplate<String, String> kafka,
            @Value("${vidacruz.kafka.timeout-dlt:PT10S}") Duration timeoutDlt) {
        this.mapper = mapper;
        this.manejador = manejador;
        this.idsProcesados = idsProcesados;
        this.kafka = kafka;
        this.timeoutDlt = timeoutDlt;
    }

    @KafkaListener(topics = ConfiguracionKafka.TOPIC_EVENTOS,
            groupId = "triage-alertas-#{@propiedadesAlerta.instancia()}",
            properties = "auto.offset.reset=latest")
    public CompletableFuture<Void> consumir(ConsumerRecord<String, String> registro) {
        return Mono.defer(() -> procesar(registro.value()))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)).maxBackoff(Duration.ofSeconds(5)).jitter(0))
                .onErrorResume(error -> Mono.fromCallable(() -> kafka.send(ConfiguracionKafka.TOPIC_EVENTOS_DLT,
                                registro.partition(), registro.key(), registro.value()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(Mono::fromFuture)
                        .timeout(timeoutDlt)
                        .doOnSuccess(ignorado -> LOGGER.error("Evento enviado a DLT tras agotar reintentos", error))
                        .then())
                .toFuture();
    }

    public Mono<Void> procesar(String carga) {
        return Mono.fromCallable(() -> decodificar(carga)).flatMap(evento -> {
            if (!"paciente_escalado".equals(evento.tipo())) {
                return Mono.empty();
            }
            if (evento.datos().idPaciente() == null) {
                return Mono.error(new IllegalArgumentException(
                        "El evento paciente_escalado debe incluir idPaciente"));
            }
            UUID idPaciente = evento.datos().idPaciente();
            if (!idsProcesados.registrar(evento.idEvento())) {
                LOGGER.warn("Evento de alerta duplicado descartado: {}", evento.idEvento());
                return Mono.empty();
            }
            return manejador.ejecutar(new ComandoEmitirAlerta(evento.idEvento(), idPaciente,
                    evento.datos().nivel(), evento.ocurridoEn()))
                    .doOnError(error -> idsProcesados.eliminar(evento.idEvento()))
                    .doOnNext(alerta -> LOGGER.info("ALERTA_ESCALAMIENTO idPaciente={} nivel={}",
                            idPaciente, evento.datos().nivel()))
                    .switchIfEmpty(Mono.fromRunnable(() -> LOGGER.warn(
                            "Evento de alerta descartado: paciente inexistente {}", idPaciente)))
                    .then();
        });
    }

    private EventoPaciente decodificar(String carga) {
        try {
            EventoPaciente evento = mapper.readValue(carga, EventoPaciente.class);
            if (evento.idEvento() == null || evento.tipo() == null || evento.datos() == null) {
                throw new IllegalArgumentException("El evento requiere idEvento, tipo y datos");
            }
            return evento;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El evento Kafka no contiene JSON válido", exception);
        }
    }
}
