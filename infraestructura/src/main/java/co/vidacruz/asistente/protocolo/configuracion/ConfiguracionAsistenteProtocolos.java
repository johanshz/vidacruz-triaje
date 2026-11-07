package co.vidacruz.asistente.protocolo.configuracion;

import co.vidacruz.asistente.configuracion.PropiedadesIa;
import co.vidacruz.asistente.protocolo.adaptador.ia.AdaptadorAsistenteClaude;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "vidacruz.ia.corpus.cargar", havingValue = "true", matchIfMissing = true)
public class ConfiguracionAsistenteProtocolos {
    private static final String NOMBRE_CIRCUITO = "asistenteProtocolos";

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
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
    AsistenteProtocolos asistenteProtocolos(ChatClient chatClient, ObjectMapper mapper,
            CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
            MeterRegistry meterRegistry) {
        return new AdaptadorAsistenteClaude(chatClient, mapper,
                circuitBreakerRegistry.circuitBreaker(NOMBRE_CIRCUITO),
                timeLimiterRegistry.timeLimiter(NOMBRE_CIRCUITO), meterRegistry);
    }
}
