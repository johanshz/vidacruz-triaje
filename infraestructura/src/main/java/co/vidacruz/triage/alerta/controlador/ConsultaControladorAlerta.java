package co.vidacruz.triage.alerta.controlador;

import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import co.vidacruz.triage.alerta.puerto.dao.DaoAlerta;
import co.vidacruz.triage.alerta.puerto.repositorio.NotificadorAlerta;
import java.time.Clock;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ConsultaControladorAlerta {
    private final DaoAlerta dao;
    private final NotificadorAlerta notificador;
    private final Clock reloj;

    public ConsultaControladorAlerta(DaoAlerta dao, NotificadorAlerta notificador, Clock reloj) {
        this.dao = dao;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @GetMapping(value = "/triage/alertas/flujo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertaEscalamiento>> consultar() {
        Flux<AlertaEscalamiento> estadoInicial = dao.consultarEscaladosActivos(reloj.instant());
        ServerSentEvent<AlertaEscalamiento> conectado = ServerSentEvent.<AlertaEscalamiento>builder()
                .comment("conectado").build();
        return Flux.just(conectado).concatWith(Flux.mergeSequential(estadoInicial, notificador.nuevas())
                .map(alerta -> ServerSentEvent.builder(alerta).event("alerta").build()));
    }
}
