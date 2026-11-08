package co.vidacruz.triage.alerta.puerto.dao;

import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import co.vidacruz.triage.alerta.modelo.dto.DatosAlertaPaciente;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DaoAlerta {
    Mono<DatosAlertaPaciente> consultarPaciente(UUID idPaciente);
    Flux<AlertaEscalamiento> consultarEscaladosActivos(Instant ahora);
}
