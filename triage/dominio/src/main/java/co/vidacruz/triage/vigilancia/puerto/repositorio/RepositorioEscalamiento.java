package co.vidacruz.triage.vigilancia.puerto.repositorio;

import co.vidacruz.triage.vigilancia.modelo.entidad.PacienteEscalado;
import java.time.Instant;
import reactor.core.publisher.Flux;

public interface RepositorioEscalamiento {
    Flux<PacienteEscalado> escalarVencidos(Instant ahora);
}
