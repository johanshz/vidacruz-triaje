package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.modelo.dto.EventoTriaje;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configuration
public class ConfiguracionKafka {
    @Bean
    ProducerFactory<String, Object> fabricaProductores(KafkaProperties propiedades) {
        Map<String, Object> configuracion = new HashMap<>(propiedades.buildProducerProperties(null));
        configuracion.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>> serializadores = new LinkedHashMap<>();
        serializadores.put(byte[].class, new ByteArraySerializer());
        serializadores.put(EventoTriaje.class, new JsonSerializer<>());
        serializadores.put(Object.class, new JsonSerializer<>());
        var valor = new DelegatingByTypeSerializer(serializadores);
        return new DefaultKafkaProducerFactory<String, Object>(configuracion, new StringSerializer(), valor);
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> fabricaProductores) {
        return new KafkaTemplate<>(fabricaProductores);
    }

    @Bean
    ConsumerFactory<String, EventoTriaje> fabricaConsumidores(KafkaProperties propiedades,
            @Value("${vidacruz.kafka.grupo:asistente-urgencias}") String grupo,
            @Value("${vidacruz.kafka.max-poll-records:50}") int maxPollRecords) {
        Map<String, Object> configuracion = new HashMap<>(propiedades.buildConsumerProperties(null));
        configuracion.put(ConsumerConfig.GROUP_ID_CONFIG, grupo);
        configuracion.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configuracion.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configuracion.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configuracion.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configuracion.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configuracion.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        configuracion.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventoTriaje.class.getName());
        configuracion.put(JsonDeserializer.TRUSTED_PACKAGES, "co.vidacruz.asistente.urgencias.modelo.dto");
        configuracion.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(configuracion);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, EventoTriaje> fabricaContenedoresKafka(
            ConsumerFactory<String, EventoTriaje> fabricaConsumidores,
            DefaultErrorHandler manejadorErrores,
            @Value("${vidacruz.kafka.concurrencia:3}") int concurrencia) {
        var fabrica = new ConcurrentKafkaListenerContainerFactory<String, EventoTriaje>();
        fabrica.setConsumerFactory(fabricaConsumidores);
        fabrica.setConcurrency(concurrencia);
        fabrica.setCommonErrorHandler(manejadorErrores);
        fabrica.getContainerProperties().setObservationEnabled(true);
        fabrica.getContainerProperties().setMissingTopicsFatal(false);
        return fabrica;
    }

    @Bean
    DefaultErrorHandler manejadorErrores(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${vidacruz.kafka.reintentos.intervalo-inicial:1000}") long intervaloInicial,
            @Value("${vidacruz.kafka.reintentos.intervalo-maximo:4000}") long intervaloMaximo,
            @Value("${vidacruz.kafka.reintentos.multiplicador:2.0}") double multiplicador) {
        var recuperador = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (registro, excepcion) -> new TopicPartition("triaje.eventos.DLT", registro.partition()));
        var espera = new ExponentialBackOffWithMaxRetries(3);
        espera.setInitialInterval(intervaloInicial);
        espera.setMultiplier(multiplicador);
        espera.setMaxInterval(intervaloMaximo);
        var manejador = new DefaultErrorHandler(recuperador, espera);
        manejador.addNotRetryableExceptions(ExcepcionEventoInvalido.class,
                org.springframework.kafka.support.serializer.DeserializationException.class);
        manejador.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> registro,
                    Exception excepcion, int intento) {
                // La política de reintentos permanece visible en los logs del framework.
            }

            @Override
            public void recovered(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> registro, Exception excepcion) {
                org.slf4j.LoggerFactory.getLogger(ConfiguracionKafka.class).warn(
                        "Evento enviado a la DLT: topic={}, partition={}, offset={}",
                        registro.topic(), registro.partition(), registro.offset(), excepcion);
            }
        });
        return manejador;
    }

    @Bean
    NewTopic topicDeadLetter() {
        return new NewTopic("triaje.eventos.DLT", 3, (short) 1);
    }

    @Bean
    RetryBackoffSpec reintentosReactivosKafka(
            @Value("${vidacruz.kafka.reintentos.intervalo-inicial:1000}") long intervaloInicial,
            @Value("${vidacruz.kafka.reintentos.intervalo-maximo:4000}") long intervaloMaximo,
            @Value("${vidacruz.kafka.reintentos.multiplicador:2.0}") double multiplicador) {
        return Retry.backoff(3, Duration.ofMillis(intervaloInicial))
                .maxBackoff(Duration.ofMillis(intervaloMaximo))
                .multiplier(multiplicador)
                .jitter(0);
    }

}
