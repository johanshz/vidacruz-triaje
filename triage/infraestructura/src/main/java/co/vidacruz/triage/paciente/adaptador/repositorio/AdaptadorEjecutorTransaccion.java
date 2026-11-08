package co.vidacruz.triage.paciente.adaptador.repositorio;

import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class AdaptadorEjecutorTransaccion implements EjecutorTransaccion {
    private final TransactionalOperator operador;
    public AdaptadorEjecutorTransaccion(TransactionalOperator operador) { this.operador = operador; }

    @Override
    public <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo) {
        return operador.transactional(Mono.defer(trabajo));
    }
}
