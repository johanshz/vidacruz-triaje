package co.vidacruz.triage.evento.puerto.repositorio;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import java.util.List;
import reactor.core.publisher.Mono;

public interface RepositorioOutbox {
    Mono<Void> agregar(EventoPaciente evento, String llave);
    Mono<List<EventoOutbox>> pendientes(int limite);
    Mono<Boolean> tomarCandadoPublicacion();
    Mono<Void> marcarEnviado(EventoOutbox evento);
    Mono<Long> contarPendientes();
}
