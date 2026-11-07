package co.vidacruz.asistente.urgencias.controlador;

import static org.mockito.Mockito.when;

import co.vidacruz.asistente.configuracion.ManejadorGlobalErrores;
import co.vidacruz.asistente.urgencias.consulta.ManejadorConsultarEstadoUrgencias;
import co.vidacruz.asistente.urgencias.excepcion.ExcepcionUrgenciasNoDisponible;
import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class ConsultaControladorUrgenciasTest {
    @Test
    void respondeEstadoDevueltoPorManejador() {
        var manejador = org.mockito.Mockito.mock(ManejadorConsultarEstadoUrgencias.class);
        when(manejador.manejar()).thenReturn(Mono.just(new DtoEstadoUrgencias(0, Map.of(), Instant.parse("2026-11-08T10:00:00Z"))));
        WebTestClient cliente = WebTestClient.bindToController(new ConsultaControladorUrgencias(manejador)).build();
        cliente.get().uri("/asistente/estado-urgencias").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.totalActivos").isEqualTo(0)
                .jsonPath("$.porNivel.CRITICO").isEqualTo(0);
    }

    @Test
    void traduceRedisNoDisponibleA503YErrorInternoA500Generico() {
        var manejador = org.mockito.Mockito.mock(ManejadorConsultarEstadoUrgencias.class);
        when(manejador.manejar()).thenReturn(Mono.error(new ExcepcionUrgenciasNoDisponible(new IllegalStateException("redis"))));
        WebTestClient cliente = WebTestClient.bindToController(new ConsultaControladorUrgencias(manejador))
                .controllerAdvice(new ManejadorGlobalErrores()).build();
        cliente.get().uri("/asistente/estado-urgencias").exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.codigo").isEqualTo("URGENCIAS_NO_DISPONIBLE");

        when(manejador.manejar()).thenReturn(Mono.error(new IllegalStateException("secreto interno")));
        cliente.get().uri("/asistente/estado-urgencias").exchange().expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.codigo").isEqualTo("ERROR_INTERNO")
                .jsonPath("$.detail").isEqualTo("Ocurrió un error interno")
                .jsonPath("$.traceId").isEqualTo("desconocido");
    }

}
