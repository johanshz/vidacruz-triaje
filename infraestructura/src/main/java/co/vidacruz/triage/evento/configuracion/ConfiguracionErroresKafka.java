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

@Configuration
public class ConfiguracionErroresKafka {
    @Bean
    DefaultErrorHandler manejadorErroresKafka(KafkaTemplate<String, String> kafka) {
        var recuperador = new DeadLetterPublishingRecoverer(kafka);
        var espera = new ExponentialBackOffWithMaxRetries(3);
        espera.setInitialInterval(500L);
        espera.setMultiplier(2.0);
        espera.setMaxInterval(5000L);
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
