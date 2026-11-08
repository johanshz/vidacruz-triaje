package co.vidacruz.asistente.urgencias.adaptador.dao;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionUrgenciasNoDisponible;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DaoUrgenciasRedisTest {
    @Mock private ReactiveStringRedisTemplate redis;
    @Mock private ReactiveSetOperations<String, String> conjuntos;

    @Test
    void traduceFallaRedisAExcepcionDeDisponibilidad() {
        when(redis.opsForSet()).thenReturn(conjuntos);
        when(conjuntos.size(anyString())).thenReturn(Mono.error(new IllegalStateException("redis caído")));
        StepVerifier.create(new DaoUrgenciasRedis(redis).consultarEstado())
                .expectError(ExcepcionUrgenciasNoDisponible.class)
                .verify();
    }

    @Test
    void convierteCincoConteosConCeros() {
        when(redis.opsForSet()).thenReturn(conjuntos);
        when(conjuntos.size(anyString())).thenReturn(Mono.just(0L));
        StepVerifier.create(new DaoUrgenciasRedis(redis).consultarEstado())
                .assertNext(estado -> {
                    assertEquals(5, estado.porNivel().size());
                    assertEquals(0L, estado.totalActivos());
                    assertEquals(0L, estado.porNivel().get(co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias.CRITICO));
                })
                .verifyComplete();
    }
}
