package co.vidacruz.triage.reporte.controlador;

import co.vidacruz.triage.reporte.consulta.ManejadorConsultarReporteOperacion;
import co.vidacruz.triage.reporte.modelo.dto.DtoReporteOperacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ConsultaControladorReporte {
    private final ManejadorConsultarReporteOperacion manejador;

    public ConsultaControladorReporte(ManejadorConsultarReporteOperacion manejador) {
        this.manejador = manejador;
    }

    @GetMapping("/triage/reportes/operacion")
    public Mono<DtoReporteOperacion> consultar(
            @RequestParam(name = "desde", required = false) String desde,
            @RequestParam(name = "hasta", required = false) String hasta) {
        return manejador.ejecutar(desde, hasta);
    }
}
