package co.vidacruz.asistente.urgencias.controlador;

import co.vidacruz.asistente.urgencias.consulta.ManejadorConsultarEstadoUrgencias;
import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ConsultaControladorUrgencias {
    private final ManejadorConsultarEstadoUrgencias manejador;

    public ConsultaControladorUrgencias(ManejadorConsultarEstadoUrgencias manejador) {
        this.manejador = manejador;
    }

    @GetMapping("/asistente/estado-urgencias")
    public Mono<DtoEstadoUrgencias> consultar() {
        return manejador.manejar();
    }
}
