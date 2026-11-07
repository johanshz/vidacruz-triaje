package co.vidacruz.asistente.urgencias.adaptador.repositorio;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RepositorioUrgenciasRedisTest {
    @Mock private ReactiveStringRedisTemplate redis;
    @Mock private ReactiveSetOperations<String, String> conjuntos;

    @Test
    void agregaEnElConjuntoDelNivel() {
        when(redis.opsForSet()).thenReturn(conjuntos);
        when(conjuntos.add("urgencias:nivel:URGENTE", "p-1")).thenReturn(Mono.just(1L));
        StepVerifier.create(new RepositorioUrgenciasRedis(redis).agregar(NivelUrgencias.URGENTE, "p-1")).verifyComplete();
        verify(conjuntos).add("urgencias:nivel:URGENTE", "p-1");
    }

    @Test
    void quitaPacienteDeLosCincoConjuntos() {
        when(redis.opsForSet()).thenReturn(conjuntos);
        when(conjuntos.remove("urgencias:nivel:CRITICO", "p-1")).thenReturn(Mono.just(1L));
        when(conjuntos.remove("urgencias:nivel:URGENTE", "p-1")).thenReturn(Mono.just(1L));
        when(conjuntos.remove("urgencias:nivel:URGENTE_2", "p-1")).thenReturn(Mono.just(1L));
        when(conjuntos.remove("urgencias:nivel:NO_URGENTE", "p-1")).thenReturn(Mono.just(1L));
        when(conjuntos.remove("urgencias:nivel:NO_URGENTE_2", "p-1")).thenReturn(Mono.just(1L));
        StepVerifier.create(new RepositorioUrgenciasRedis(redis).quitarDeTodosLosNiveles("p-1")).verifyComplete();
        for (NivelUrgencias nivel : NivelUrgencias.enOrden()) verify(conjuntos).remove("urgencias:nivel:" + nivel, "p-1");
    }
}
