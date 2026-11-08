package co.vidacruz.triage;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vidacruz.vigilancia.intervalo=PT1H", "vidacruz.outbox.intervalo=PT1H",
                "spring.kafka.listener.auto-startup=false"})
@AutoConfigureWebTestClient
class ReporteOperacionHttpIntegracionTest extends SoporteIntegracionTriage {
    @Autowired private WebTestClient cliente;
    @MockitoBean private DaoReporteOperacion dao;

    @Test
    void parametrosInvalidosInformanCampoYNoConsultanDao() {
        verificarInvalido(null, null, "desde");
        verificarInvalido(null, "2026-11-09T00:00:00Z", "desde");
        verificarInvalido("2026-11-08", "2026-11-09T00:00:00Z", "desde");
        verificarInvalido("2026-11-08T00:00:00Z", null, "hasta");
        verificarInvalido("2026-11-08T00:00:00Z", "fecha", "hasta");
        verificarInvalido("2026-11-09T00:00:00Z", "2026-11-09T00:00:00Z", "hasta");
        verificarInvalido("2026-11-01T00:00:00Z", "2026-12-02T00:00:01Z", "hasta");

        verifyNoInteractions(dao);
    }

    @Test
    void sinApiKeyResponde401YConApiKeyEntregaRespuestaVacia() {
        cliente.get().uri(uri -> uri.path("/triage/reportes/operacion")
                        .queryParam("desde", "2026-11-08T00:00:00Z")
                        .queryParam("hasta", "2026-11-09T00:00:00Z").build())
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");

        when(dao.consultar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Flux.empty());
        cliente.get().uri(uri -> uri.path("/triage/reportes/operacion")
                        .queryParam("desde", "2026-11-08T00:00:00Z")
                        .queryParam("hasta", "2026-11-09T00:00:00Z").build())
                .header("X-API-Key", "clave-prueba").accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.desde").isEqualTo("2026-11-08T00:00:00Z")
                .jsonPath("$.hasta").isEqualTo("2026-11-09T00:00:00Z")
                .jsonPath("$.zonaHoraria").isEqualTo("America/Bogota")
                .jsonPath("$.horas").isEmpty();
    }

    private void verificarInvalido(String desde, String hasta, String campo) {
        var uri = cliente.get().uri(builder -> {
            builder.path("/triage/reportes/operacion");
            if (desde != null) builder.queryParam("desde", desde);
            if (hasta != null) builder.queryParam("hasta", hasta);
            return builder.build();
        });
        uri.header("X-API-Key", "clave-prueba").exchange().expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.campo").isEqualTo(campo);
    }
}
