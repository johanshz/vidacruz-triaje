package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import reactor.core.publisher.Mono;

public class ManejadorRegistrarPaciente {
    private final ServicioClasificacionPaciente clasificacion;
    private final FabricaPaciente fabrica;
    private final RepositorioPaciente pacientes;
    private final RegistrarCambioPaciente registrarCambio;
    private final EjecutorTransaccion transaccion;
    private final ContadorClasificaciones contador;
    private final Clock reloj;
    private final ConfiguracionSla configuracionSla;

    public ManejadorRegistrarPaciente(DependenciasRegistroPaciente dependencias) {
        this.clasificacion = dependencias.clasificacion();
        this.fabrica = dependencias.fabrica();
        this.pacientes = dependencias.pacientes();
        this.registrarCambio = dependencias.registrarCambio();
        this.transaccion = dependencias.transaccion();
        this.contador = dependencias.contador();
        this.reloj = dependencias.reloj();
        this.configuracionSla = dependencias.configuracionSla();
    }

    public Mono<Paciente> ejecutar(ComandoRegistrarPaciente comando) {
        return Mono.defer(() -> {
            clasificacion.validar(comando.datos());
            var nivel = clasificacion.clasificar(comando.datos());
            Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
            UUID id = UUID.randomUUID();
            Paciente paciente = fabrica.registrado(comando.datos(), nivel, configuracionSla.minutos(nivel), id, ahora);
            var evento = fabrica.eventoRegistrado(paciente, UUID.randomUUID(), ahora);
            return transaccion.ejecutar(() -> pacientes.guardar(paciente)
                    .then(registrarCambio.registrar(paciente, evento, null))
                    .thenReturn(paciente)).doOnSuccess(guardado -> contador.incrementar(nivel));
        });
    }
}
