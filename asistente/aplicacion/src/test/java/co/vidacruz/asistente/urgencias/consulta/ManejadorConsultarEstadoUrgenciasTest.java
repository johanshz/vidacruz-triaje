package co.vidacruz.asistente.urgencias.consulta;

import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import co.vidacruz.asistente.urgencias.puerto.dao.DaoUrgencias;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ManejadorConsultarEstadoUrgenciasTest {
    @Mock private DaoUrgencias dao;
    @InjectMocks private ManejadorConsultarEstadoUrgencias manejador;

    @Test
    void consultaPorElPuertoDeLectura() {
        var estado = new DtoEstadoUrgencias(0, Map.of(), Instant.parse("2026-11-08T10:00:00Z"));
        when(dao.consultarEstado()).thenReturn(Mono.just(estado));
        StepVerifier.create(manejador.manejar()).expectNext(estado).verifyComplete();
        verify(dao).consultarEstado();
    }
}
