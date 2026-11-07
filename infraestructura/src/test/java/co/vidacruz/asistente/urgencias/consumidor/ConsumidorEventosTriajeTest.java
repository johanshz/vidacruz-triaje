package co.vidacruz.asistente.urgencias.consumidor;

import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.comando.ComandoActualizarUrgencias;
import co.vidacruz.asistente.urgencias.comando.ManejadorActualizarUrgencias;
import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.modelo.dto.DatosEventoTriaje;
import co.vidacruz.asistente.urgencias.modelo.dto.EventoTriaje;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.RetryBackoffSpec;

@ExtendWith(MockitoExtension.class)
class ConsumidorEventosTriajeTest {
    @Mock private ManejadorActualizarUrgencias manejador;
    @Mock private Tracer tracer;
    @Mock private Propagator propagator;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RetryBackoffSpec reintentos;
    @InjectMocks private ConsumidorEventosTriaje consumidor;

    @Test
    void ignoraVersionOTipoDesconocido() {
        StepVerifier.create(consumidor.consumir(evento("nuevo", 1, "p-1", "CRITICO"))).verifyComplete();
        StepVerifier.create(consumidor.consumir(evento("paciente_registrado", 2, "p-1", "CRITICO"))).verifyComplete();
        verifyNoInteractions(manejador);
    }

    @Test
    void reenviaEventosConocidosOIncompletosAlDominio() {
        var comando = new ComandoActualizarUrgencias("paciente_registrado", "p-1", "URGENTE");
        when(manejador.manejar(comando)).thenReturn(Mono.empty());
        StepVerifier.create(consumidor.consumir(evento("paciente_registrado", 1, "p-1", "URGENTE"))).verifyComplete();
        verify(manejador).manejar(comando);

        var incompleto = new EventoTriaje("ev-2", null, 1, Instant.now(), new DatosEventoTriaje(null, null, null, null, null));
        when(manejador.manejar(new ComandoActualizarUrgencias(null, null, null)))
                .thenReturn(Mono.error(new ExcepcionEventoInvalido("Falta tipo")));
        StepVerifier.create(consumidor.consumir(incompleto)).expectError(ExcepcionEventoInvalido.class).verify();
    }

    private EventoTriaje evento(String tipo, int version, String id, String nivel) {
        return new EventoTriaje("ev-1", tipo, version, Instant.now(), new DatosEventoTriaje(id, nivel, null, null, false));
    }
}
