package co.vidacruz.asistente.urgencias.comando;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.servicio.ServicioActualizarUrgencias;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ManejadorActualizarUrgenciasTest {
    @Mock private ServicioActualizarUrgencias servicio;
    @InjectMocks private ManejadorActualizarUrgencias manejador;

    @Test
    void delegaElComandoAlServicio() {
        var comando = new ComandoActualizarUrgencias("paciente_registrado", "p-1", "URGENTE");
        when(servicio.actualizar(comando.tipo(), comando.idPaciente(), comando.nivel())).thenReturn(Mono.empty());
        StepVerifier.create(manejador.manejar(comando)).verifyComplete();
        verify(servicio).actualizar(comando.tipo(), comando.idPaciente(), comando.nivel());
    }
}
