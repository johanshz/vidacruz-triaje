package co.vidacruz.asistente.urgencias.adaptador.repositorio;

import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import co.vidacruz.asistente.urgencias.puerto.repositorio.RepositorioUrgencias;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RepositorioUrgenciasRedis implements RepositorioUrgencias {
    private static final String PREFIJO_CLAVE = "urgencias:nivel:";
    private final ReactiveStringRedisTemplate redis;

    public RepositorioUrgenciasRedis(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> agregar(NivelUrgencias nivel, String idPaciente) {
        return redis.opsForSet().add(clave(nivel), idPaciente).then();
    }

    @Override
    public Mono<Void> quitarDeTodosLosNiveles(String idPaciente) {
        return reactor.core.publisher.Flux.fromIterable(NivelUrgencias.enOrden())
                .concatMap(nivel -> redis.opsForSet().remove(clave(nivel), idPaciente).then())
                .then();
    }

    private String clave(NivelUrgencias nivel) {
        return PREFIJO_CLAVE + nivel.name();
    }
}
