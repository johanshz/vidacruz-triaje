package co.vidacruz.asistente.protocolo.controlador;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.configuracion.ManejadorGlobalErrores;
import co.vidacruz.asistente.protocolo.comando.ManejadorConsultarProtocolo;
import co.vidacruz.asistente.protocolo.excepcion.ExcepcionIaNoDisponible;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class ConsultaControladorProtocoloTest {
    @Test
    void consultaValidaRespondeYRegistraResultadoSinExponerPregunta() {
        var manejador = mock(ManejadorConsultarProtocolo.class);
        var metricas = new SimpleMeterRegistry();
        when(manejador.manejar(any())).thenReturn(Mono.just(new ResultadoConsulta(
                new RespuestaConsulta("Acción protocolizada", List.of()), ResultadoConsulta.Estado.RESPONDIDA,
                19, List.of("PROTO-001"))));
        var cliente = cliente(manejador, metricas);

        cliente.post().uri("/asistente/consulta").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":\"pregunta confidencial\"}").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.respuesta").isEqualTo("Acción protocolizada");

        verify(manejador).manejar("pregunta confidencial");
        assertEquals(1.0, metricas.get("asistente.consultas")
                .tag("resultado", "respondida").counter().count());
    }

    @Test
    void entradaNoTextualYJsonInvalidoResponden400Validacion() {
        var cliente = cliente(mock(ManejadorConsultarProtocolo.class), new SimpleMeterRegistry());

        cliente.post().uri("/asistente/consulta").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":123}").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.campo").isEqualTo("pregunta");

        cliente.post().uri("/asistente/consulta").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.campo").isEqualTo("pregunta");

        cliente.post().uri("/asistente/consulta").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.campo").isEqualTo("pregunta");
    }

    @Test
    void falloDelProveedorSeTraduceA503YContadorNoDisponible() {
        var manejador = mock(ManejadorConsultarProtocolo.class);
        var metricas = new SimpleMeterRegistry();
        when(manejador.manejar(any())).thenReturn(Mono.error(new ExcepcionIaNoDisponible(new IllegalStateException())));

        cliente(manejador, metricas).post().uri("/asistente/consulta").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":\"pregunta válida\"}").exchange()
                .expectStatus().isEqualTo(503).expectBody().jsonPath("$.codigo").isEqualTo("IA_NO_DISPONIBLE");

        assertEquals(1.0, metricas.get("asistente.consultas")
                .tag("resultado", "no_disponible").counter().count());
    }

    private WebTestClient cliente(ManejadorConsultarProtocolo manejador, SimpleMeterRegistry metricas) {
        return WebTestClient.bindToController(new ConsultaControladorProtocolo(manejador, metricas))
                .controllerAdvice(new ManejadorGlobalErrores()).build();
    }
}
