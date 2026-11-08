package co.vidacruz.asistente.protocolo.comando.manejador;

import co.vidacruz.asistente.protocolo.comando.ComandoConsultarProtocolo;
import co.vidacruz.asistente.protocolo.comando.fabrica.FabricaConsultaProtocolo;
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
        var comando = fabrica.crear(pregunta);
        return servicio.consultar(comando.pregunta());
    }
}
