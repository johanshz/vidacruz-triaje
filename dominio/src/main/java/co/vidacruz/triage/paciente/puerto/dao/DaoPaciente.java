package co.vidacruz.triage.paciente.puerto.dao;

import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DaoPaciente {
    Mono<DtoPaciente> consultar(UUID id, Instant ahora);
}
