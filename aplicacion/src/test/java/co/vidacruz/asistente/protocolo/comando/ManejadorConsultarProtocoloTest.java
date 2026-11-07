package co.vidacruz.asistente.protocolo.comando;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import co.vidacruz.asistente.protocolo.servicio.ServicioConsultarProtocolo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ManejadorConsultarProtocoloTest {
    @Mock private BuscadorProtocolos buscador;
    @Mock private AsistenteProtocolos asistente;

    @Test
    void fabricaComandoYEjecutaServicioConPuertos() {
        ServicioConsultarProtocolo servicio = new ServicioConsultarProtocolo(buscador, asistente, 0.75, 3);
        FabricaConsultaProtocolo fabrica = new FabricaConsultaProtocolo();
        ManejadorConsultarProtocolo manejador = new ManejadorConsultarProtocolo(fabrica, servicio);
        when(buscador.buscar("sin cobertura", 0.75, 3)).thenReturn(Flux.empty());
        ResultadoConsulta esperado = new ResultadoConsulta(
                new RespuestaConsulta(ServicioConsultarProtocolo.AVISO_SIN_COBERTURA, List.of()),
                "sin_cobertura", 13, List.of());

        StepVerifier.create(manejador.manejar("sin cobertura"))
                .assertNext(resultado -> assertEquals(esperado, resultado))
                .verifyComplete();

        verify(buscador).buscar("sin cobertura", 0.75, 3);
    }
}
