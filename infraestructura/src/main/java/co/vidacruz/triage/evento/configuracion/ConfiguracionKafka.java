package co.vidacruz.triage.evento.configuracion;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class ConfiguracionKafka {
    public static final String TOPIC_EVENTOS = "triaje.eventos";
    public static final String TOPIC_EVENTOS_DLT = "triaje.eventos.DLT";

    @Bean
    NewTopic topicEventosTriage() {
        return TopicBuilder.name(TOPIC_EVENTOS).partitions(3).replicas(1).build();
    }

    @Bean
    static BeanPostProcessor kafkaAdminNoFatalAlIniciar() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof KafkaAdmin kafkaAdmin) {
                    kafkaAdmin.setFatalIfBrokerNotAvailable(false);
                }
                return bean;
            }
        };
    }
}
