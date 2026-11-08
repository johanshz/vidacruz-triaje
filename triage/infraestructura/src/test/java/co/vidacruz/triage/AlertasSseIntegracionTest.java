package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import co.vidacruz.triage.alerta.adaptador.sse.AdaptadorSseAlerta;
import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.core.publisher.BaseSubscriber;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vidacruz.vigilancia.intervalo=PT1H", "vidacruz.outbox.intervalo=PT1H",
                "spring.kafka.listener.auto-startup=false", "vidacruz.alerta.buffer=1"})
@AutoConfigureWebTestClient
class AlertasSseIntegracionTest extends SoporteIntegracionTriage {
    @Autowired private AdaptadorSseAlerta adaptador;
    @Autowired private DatabaseClient db;

    @Test
    void enviaPrimeroElEstadoInicialEscaladoQueSigueActivo() {
        UUID id = UUID.randomUUID();
        insertarEscalado(id, "EN_CONSULTA");

        FluxExchangeResult<ServerSentEvent<AlertaEscalamiento>> resultado = webTestClient.get()
                .uri("/triage/alertas/flujo").header("X-API-Key", "clave-prueba")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<>() { });

        StepVerifier.create(resultado.getResponseBody().filter(evento -> "alerta".equals(evento.event())).take(1))
                .assertNext(evento -> {
                    assertEquals("alerta", evento.event());
                    assertEquals(id, evento.data().idPaciente());
                    assertEquals("CRITICO", evento.data().nivel());
                }).verifyComplete();
    }

    @Test
    void requiereApiKeyYEntregaAlertasNuevasHastaQueSeDesconecta() {
        webTestClient.get().uri("/triage/alertas/flujo").accept(MediaType.TEXT_EVENT_STREAM)
                .exchange().expectStatus().isUnauthorized().expectBody().jsonPath("$.codigo")
                .isEqualTo("NO_AUTORIZADO");

        var resultado = webTestClient.get().uri("/triage/alertas/flujo").header("X-API-Key", "clave-prueba")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<AlertaEscalamiento>>() { });
        UUID id = UUID.randomUUID();
        var alerta = new AlertaEscalamiento(id, "Paciente nuevo", "URGENTE", 2, Instant.now());

        StepVerifier.create(resultado.getResponseBody().filter(evento -> "alerta".equals(evento.event())).take(1))
                .then(() -> Mono.delay(Duration.ofMillis(200)).then(adaptador.notificar(alerta)).subscribe())
                .assertNext(evento -> {
                    assertEquals("alerta", evento.event());
                    assertEquals(id, evento.data().idPaciente());
                }).verifyComplete();
    }

    @Test
    void suscriptorLentoNoFrenaAlSuscriptorQueTieneDemanda() {
        BaseSubscriber<AlertaEscalamiento> suscriptorLento = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(org.reactivestreams.Subscription subscription) { }
        };
        adaptador.nuevas().subscribe(suscriptorLento);
        UUID id = UUID.randomUUID();
        var primera = new AlertaEscalamiento(id, "Paciente", "CRITICO", 1, Instant.now());
        var segunda = new AlertaEscalamiento(id, "Paciente", "CRITICO", 2, Instant.now());

        StepVerifier.create(adaptador.nuevas().take(2))
                .then(() -> StepVerifier.create(Mono.when(adaptador.notificar(primera),
                        adaptador.notificar(segunda))).verifyComplete())
                .expectNext(primera, segunda).verifyComplete();
        suscriptorLento.cancel();
    }

    private void insertarEscalado(UUID id, String estado) {
        Instant registro = Instant.now().minusSeconds(120);
        StepVerifier.create(db.sql("""
                INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel,
                    sla_minutos, estado, escalado, fecha_registro, fecha_estado)
                VALUES (:id, 'Paciente inicial', 30, 80, 120, 80, 98, 36.5, 0, 15, 'Dolor',
                    'CRITICO', 1, :estado, true, :registro, :registro)
                """).bind("id", id).bind("estado", estado).bind("registro", registro)
                .fetch().rowsUpdated()).expectNext(1L).verifyComplete();
        StepVerifier.create(db.sql("""
                INSERT INTO auditoria (id, paciente_id, tipo_evento, estado_anterior, estado_nuevo, ocurrido_en)
                VALUES (:auditoria, :id, 'paciente_escalado', 'REGISTRADO', 'REGISTRADO', :fecha)
                """).bind("auditoria", UUID.randomUUID()).bind("id", id).bind("fecha", registro.plusSeconds(60))
                .fetch().rowsUpdated()).expectNext(1L).verifyComplete();
    }
}
