package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioAuditoria;
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
    private final RepositorioAuditoria auditoria;
    private final RepositorioOutbox outbox;
    private final EjecutorTransaccion transaccion;
    private final ContadorClasificaciones contador;
    private final Clock reloj;

    public ManejadorRegistrarPaciente(ServicioClasificacionPaciente clasificacion, FabricaPaciente fabrica,
            RepositorioPaciente pacientes, RepositorioAuditoria auditoria, RepositorioOutbox outbox,
            EjecutorTransaccion transaccion, ContadorClasificaciones contador, Clock reloj) {
        this.clasificacion = clasificacion; this.fabrica = fabrica; this.pacientes = pacientes;
        this.auditoria = auditoria; this.outbox = outbox; this.transaccion = transaccion;
        this.contador = contador; this.reloj = reloj;
    }

    public Mono<Paciente> ejecutar(ComandoRegistrarPaciente comando) {
        return Mono.defer(() -> {
            var nivel = clasificacion.clasificar(comando.datos());
            Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
            UUID id = UUID.randomUUID();
            Paciente paciente = fabrica.registrado(comando.datos(), nivel, id, ahora);
            var evento = fabrica.eventoRegistrado(paciente, UUID.randomUUID(), ahora);
            return transaccion.ejecutar(() -> pacientes.guardar(paciente)
                    .then(auditoria.registrar(paciente, evento.tipo(), null, paciente.estado().name(), ahora))
                    .then(outbox.agregar(evento, paciente.id().toString()))
                    .thenReturn(paciente)).doOnSuccess(guardado -> contador.incrementar(nivel));
        });
    }
}
