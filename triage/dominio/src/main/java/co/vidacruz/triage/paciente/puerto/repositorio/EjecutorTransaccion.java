package co.vidacruz.triage.paciente.puerto.repositorio;

import java.util.function.Supplier;
import reactor.core.publisher.Mono;

public interface EjecutorTransaccion {
    <T> Mono<T> ejecutar(Supplier<Mono<T>> trabajo);
}
