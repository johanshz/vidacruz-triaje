package co.vidacruz.triage.evento.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import java.time.Duration;

@Configuration
public class ConfiguracionErroresKafka {
    private static final int REINTENTOS_MAXIMOS = 3;
    private static final Duration ESPERA_INICIAL_REINTENTO = Duration.ofMillis(500);
    private static final double MULTIPLICADOR_REINTENTO = 2.0;
    private static final Duration ESPERA_MAXIMA_REINTENTO = Duration.ofSeconds(5);

    @Bean
    DefaultErrorHandler manejadorErroresKafka(KafkaTemplate<String, String> kafka) {
        var recuperador = new DeadLetterPublishingRecoverer(kafka);
        var espera = new ExponentialBackOffWithMaxRetries(REINTENTOS_MAXIMOS);
        espera.setInitialInterval(ESPERA_INICIAL_REINTENTO.toMillis());
        espera.setMultiplier(MULTIPLICADOR_REINTENTO);
        espera.setMaxInterval(ESPERA_MAXIMA_REINTENTO.toMillis());
        return new DefaultErrorHandler(recuperador, espera);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumidores, DefaultErrorHandler manejadorErroresKafka) {
        var fabrica = new ConcurrentKafkaListenerContainerFactory<String, String>();
        fabrica.setConsumerFactory(consumidores);
        fabrica.setCommonErrorHandler(manejadorErroresKafka);
        fabrica.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        fabrica.getContainerProperties().setAsyncAcks(true);
        return fabrica;
    }
}
