package co.vidacruz.asistente.urgencias.adaptador.dao;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionUrgenciasNoDisponible;
import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import co.vidacruz.asistente.urgencias.puerto.dao.DaoUrgencias;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.EnumMap;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DaoUrgenciasRedis implements DaoUrgencias {
    private static final String PREFIJO_CLAVE = "urgencias:nivel:";
    private final ReactiveStringRedisTemplate redis;

    public DaoUrgenciasRedis(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<DtoEstadoUrgencias> consultarEstado() {
        return Flux.fromIterable(NivelUrgencias.enOrden())
                .concatMap(nivel -> redis.opsForSet().size(PREFIJO_CLAVE + nivel.name())
                        .defaultIfEmpty(0L)
                        .map(cantidad -> new AbstractMap.SimpleImmutableEntry<>(nivel, cantidad)))
                .collect(() -> new EnumMap<NivelUrgencias, Long>(NivelUrgencias.class),
                        (conteos, entrada) -> conteos.put(entrada.getKey(), entrada.getValue()))
                .map(conteos -> new DtoEstadoUrgencias(0, conteos, Instant.now()))
                .onErrorMap(error -> new ExcepcionUrgenciasNoDisponible(error));
    }
}
