package co.vidacruz.triage.paciente.controlador;

import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.paciente.comando.ComandoCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.ComandoRegistrarPaciente;
import co.vidacruz.triage.paciente.comando.manejador.ManejadorCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.manejador.ManejadorRegistrarPaciente;
import co.vidacruz.triage.paciente.consulta.ManejadorConsultarPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/triage/pacientes")
public class ControladorPaciente {
    private final ManejadorRegistrarPaciente registrar;
    private final ManejadorConsultarPaciente consultar;
    private final ManejadorCambiarEstadoPaciente cambiar;

    public ControladorPaciente(ManejadorRegistrarPaciente registrar, ManejadorConsultarPaciente consultar,
            ManejadorCambiarEstadoPaciente cambiar) {
        this.registrar = registrar; this.consultar = consultar; this.cambiar = cambiar;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RespuestaRegistroPaciente> registrar(@RequestBody SolicitudRegistroPaciente solicitud) {
        if (solicitud == null) throw new ExcepcionValidacion("cuerpo", "El cuerpo es obligatorio");
        return registrar.ejecutar(new ComandoRegistrarPaciente(solicitud.aDatos())).map(RespuestaRegistroPaciente::desde);
    }

    @GetMapping("/{id}")
    public Mono<RespuestaPaciente> consultar(@PathVariable String id) {
        return consultar.ejecutar(uuid(id)).map(RespuestaPaciente::desde);
    }

    @PatchMapping("/{id}/estado")
    public Mono<RespuestaPaciente> cambiar(@PathVariable String id, @RequestBody SolicitudCambioEstado solicitud) {
        if (solicitud == null || solicitud.estado() == null) throw new ExcepcionValidacion("estado", "El campo es obligatorio");
        EstadoPaciente destino;
        try { destino = EstadoPaciente.valueOf(solicitud.estado()); }
        catch (IllegalArgumentException error) { throw new ExcepcionValidacion("estado", "Estado desconocido"); }
        return cambiar.ejecutar(new ComandoCambiarEstadoPaciente(uuid(id), destino)).map(RespuestaPaciente::desde);
    }

    private UUID uuid(String id) {
        try { return UUID.fromString(id); }
        catch (IllegalArgumentException error) { throw new ExcepcionValidacion("id", "El identificador debe ser UUID"); }
    }
}

