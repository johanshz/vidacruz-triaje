package co.vidacruz.asistente.protocolo.comando;

import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import co.vidacruz.asistente.protocolo.servicio.ServicioConsultarProtocolo;
import reactor.core.publisher.Mono;

public final class ManejadorConsultarProtocolo {
    private final FabricaConsultaProtocolo fabrica;
    private final ServicioConsultarProtocolo servicio;

    public ManejadorConsultarProtocolo(FabricaConsultaProtocolo fabrica,
            ServicioConsultarProtocolo servicio) {
        this.fabrica = fabrica;
        this.servicio = servicio;
    }

    public Mono<ResultadoConsulta> manejar(String pregunta) {
        return servicio.consultar(fabrica.crear(pregunta).pregunta());
    }
}
