package co.vidacruz.asistente.protocolo.configuracion;

import co.vidacruz.asistente.configuracion.PropiedadesIa;
import co.vidacruz.asistente.protocolo.adaptador.ia.AdaptadorAsistenteClaude;
import co.vidacruz.asistente.protocolo.adaptador.ia.DependenciasAsistenteClaude;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = CondicionesProtocolo.PROPIEDAD_CARGAR_CORPUS, havingValue = "true", matchIfMissing = true)
public class ConfiguracionAsistenteProtocolos {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    static BeanPostProcessor quitarTemperaturaAnthropic() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String nombre) {
                if (bean instanceof AnthropicChatProperties propiedades) {
                    propiedades.getOptions().setTemperature(null);
                }
                return bean;
            }
        };
    }

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(PropiedadesIa propiedades) {
        PropiedadesIa.Resiliencia resiliencia = propiedades.resiliencia();
        CircuitBreakerConfig configuracion = CircuitBreakerConfig.custom()
                .slidingWindowSize(resiliencia.ventana())
                .minimumNumberOfCalls(resiliencia.minimoLlamadas())
                .failureRateThreshold(resiliencia.porcentajeFallos())
                .waitDurationInOpenState(resiliencia.esperaAbierto())
                .permittedNumberOfCallsInHalfOpenState(resiliencia.llamadasSemiabierto())
                .build();
        return CircuitBreakerRegistry.of(configuracion);
    }

    @Bean
    TimeLimiterRegistry timeLimiterRegistry(PropiedadesIa propiedades) {
        Duration timeout = propiedades.resiliencia().timeout();
        return TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    DependenciasAsistenteClaude dependenciasAsistenteClaude(
            CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
            MeterRegistry meterRegistry) {
        return new DependenciasAsistenteClaude(
                circuitBreakerRegistry.circuitBreaker(CondicionesProtocolo.NOMBRE_CIRCUITO),
                timeLimiterRegistry.timeLimiter(CondicionesProtocolo.NOMBRE_CIRCUITO), meterRegistry);
    }

    @Bean
    AsistenteProtocolos asistenteProtocolos(ChatClient chatClient, ObjectMapper mapper,
            DependenciasAsistenteClaude dependencias) {
        return new AdaptadorAsistenteClaude(chatClient, mapper, dependencias);
    }
}
