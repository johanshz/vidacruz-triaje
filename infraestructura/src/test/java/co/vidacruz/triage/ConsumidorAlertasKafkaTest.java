package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.alerta.comando.ComandoEmitirAlerta;
import co.vidacruz.triage.alerta.comando.ManejadorEmitirAlerta;
import co.vidacruz.triage.alerta.consumidor.ConsumidorAlertasKafka;
import co.vidacruz.triage.alerta.consumidor.MemoriaIdsEventoProcesados;
import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConsumidorAlertasKafkaTest {
    private final ManejadorEmitirAlerta manejador = mock(ManejadorEmitirAlerta.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ConsumidorAlertasKafka consumidor = new ConsumidorAlertasKafka(mapper, manejador,
            new MemoriaIdsEventoProcesados(10), mock(KafkaTemplate.class), java.time.Duration.ofSeconds(10));
    private final UUID idEvento = UUID.randomUUID();
    private final UUID idPaciente = UUID.randomUUID();
    private final Instant ocurridoEn = Instant.parse("2026-11-06T10:00:00Z");

    @BeforeEach
    void prepararManejador() {
        when(manejador.ejecutar(any())).thenReturn(Mono.just(new AlertaEscalamiento(idPaciente,
                "Ana", "CRITICO", 5, ocurridoEn)));
    }

    @Test
    void ignoraEventosDeOtroTipo() throws Exception {
        StepVerifier.create(consumidor.procesar(json("paciente_registrado"))).verifyComplete();

        verify(manejador, never()).ejecutar(any());
    }

    @Test
    void convierteElPayloadEnComandoDeAlerta() throws Exception {
        StepVerifier.create(consumidor.procesar(json("paciente_escalado"))).verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(ComandoEmitirAlerta.class);
        verify(manejador).ejecutar(captor.capture());
        assertEquals(idEvento, captor.getValue().idEvento());
        assertEquals(idPaciente, captor.getValue().idPaciente());
        assertEquals("CRITICO", captor.getValue().nivel());
        assertEquals(ocurridoEn, captor.getValue().escaladoEn());
    }

    @Test
    void descartaUnIdEventoRepetido() throws Exception {
        String payload = json("paciente_escalado");
        StepVerifier.create(consumidor.procesar(payload)).verifyComplete();
        StepVerifier.create(consumidor.procesar(payload)).verifyComplete();

        verify(manejador, times(1)).ejecutar(any());
    }

    @Test
    void pacienteInexistenteSeDescartaSinError() throws Exception {
        when(manejador.ejecutar(any())).thenReturn(Mono.empty());

        StepVerifier.create(consumidor.procesar(json("paciente_escalado"))).verifyComplete();
    }

    @Test
    void jsonMalFormadoSePropagaParaElManejadorDeErroresKafka() {
        StepVerifier.create(consumidor.procesar("{")).expectError(IllegalArgumentException.class).verify();
    }

    private String json(String tipo) throws Exception {
        var evento = new EventoPaciente(idEvento, tipo, EventoPaciente.VERSION_ACTUAL, ocurridoEn,
                new DatosEventoPaciente(idPaciente, "CRITICO", "REGISTRADO", "REGISTRADO", true));
        return mapper.writeValueAsString(evento);
    }
}
