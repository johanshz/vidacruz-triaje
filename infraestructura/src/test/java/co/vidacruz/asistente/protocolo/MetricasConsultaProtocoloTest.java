package co.vidacruz.asistente.protocolo;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.vidacruz.asistente.protocolo.comando.ManejadorConsultarProtocolo;
import co.vidacruz.asistente.protocolo.controlador.ConsultaControladorProtocolo;
import co.vidacruz.asistente.protocolo.modelo.dto.FuenteProtocolo;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class MetricasConsultaProtocoloTest {
    private static final String PREGUNTA_PRIVADA = "pregunta-clínica-confidencial";
    private static final String RESPUESTA_PRIVADA = "respuesta-clínica-confidencial";

    @Test
    void cuentaCadaResultadoYLosLogsNoIncluyenPreguntaNiRespuesta() {
        for (String resultado : List.of("respondida", "sin_cobertura", "barrera_rechazada")) {
            var estado = java.util.Arrays.stream(ResultadoConsulta.Estado.values())
                    .filter(valor -> valor.etiqueta().equals(resultado)).findFirst().orElseThrow();
            var manejador = mock(ManejadorConsultarProtocolo.class);
            var metricas = new SimpleMeterRegistry();
            when(manejador.manejar(anyString())).thenReturn(Mono.just(new ResultadoConsulta(
                    new RespuestaConsulta(RESPUESTA_PRIVADA,
                            List.of(new FuenteProtocolo("PROTO-001", "Medidas iniciales"))),
                    estado, PREGUNTA_PRIVADA.length(), List.of("PROTO-001"))));

            Logger logger = (Logger) LoggerFactory.getLogger(ConsultaControladorProtocolo.class);
            var appender = new ListAppender<ILoggingEvent>();
            appender.start();
            logger.addAppender(appender);
            try {
                WebTestClient.bindToController(new ConsultaControladorProtocolo(manejador, metricas))
                        .build().post().uri("/asistente/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"pregunta\":\"" + PREGUNTA_PRIVADA + "\"}")
                        .exchange().expectStatus().isOk();

                assertEquals(1.0, metricas.get("asistente.consultas").tag("resultado", resultado)
                        .counter().count());
                assertFalse(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                        .anyMatch(mensaje -> mensaje.contains(PREGUNTA_PRIVADA)
                                || mensaje.contains(RESPUESTA_PRIVADA)));
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        }
    }
}
