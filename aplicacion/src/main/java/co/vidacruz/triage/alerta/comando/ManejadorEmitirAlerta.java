package co.vidacruz.triage.alerta.comando;

import co.vidacruz.triage.alerta.puerto.dao.DaoAlerta;
import co.vidacruz.triage.alerta.puerto.repositorio.NotificadorAlerta;
import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import java.time.Clock;
import java.time.Duration;
import reactor.core.publisher.Mono;

public class ManejadorEmitirAlerta {
    private final DaoAlerta dao;
    private final NotificadorAlerta notificador;
    private final Clock reloj;

    public ManejadorEmitirAlerta(DaoAlerta dao, NotificadorAlerta notificador, Clock reloj) {
        this.dao = dao;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    public Mono<AlertaEscalamiento> ejecutar(ComandoEmitirAlerta comando) {
        return dao.consultarPaciente(comando.idPaciente())
                .map(datos -> new AlertaEscalamiento(comando.idPaciente(), datos.nombre(), comando.nivel(),
                        Math.max(0, Duration.between(datos.fechaRegistro(), reloj.instant()).toMinutes()),
                        comando.escaladoEn()))
                .flatMap(alerta -> notificador.notificar(alerta).thenReturn(alerta));
    }
}
