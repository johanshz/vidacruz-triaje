package co.vidacruz.triage.vigilancia.comando;

import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.vigilancia.puerto.repositorio.ContadorEscalamientos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.vigilancia.servicio.ServicioEscalamiento;
import java.time.Clock;
import java.time.Instant;
import reactor.core.publisher.Mono;

public class ManejadorEscalarVencidos {
    private final RepositorioEscalamiento repositorio;
    private final RepositorioAuditoria auditoria;
    private final RepositorioOutbox outbox;
    private final EjecutorTransaccion transaccion;
    private final ServicioEscalamiento servicio;
    private final ContadorEscalamientos contador;
    private final Clock reloj;

    public ManejadorEscalarVencidos(DependenciasEscalamiento dependencias) {
        repositorio = dependencias.repositorio();
        auditoria = dependencias.auditoria();
        outbox = dependencias.outbox();
        transaccion = dependencias.transaccion();
        servicio = dependencias.servicio();
        contador = dependencias.contador();
        reloj = dependencias.reloj();
    }

    public Mono<Void> ejecutar(ComandoEscalarVencidos comando) {
        Instant ahora = reloj.instant();
        return transaccion.ejecutar(() -> repositorio.escalarVencidos(ahora)
                .concatMap(paciente -> {
                    var cambio = servicio.crearCambio(paciente, ahora);
                    return auditoria.registrar(cambio.auditoria())
                            .then(outbox.agregar(cambio.evento(), paciente.id().toString()))
                            .thenReturn(paciente);
                })
                .collectList())
                .doOnNext(escalados -> escalados.forEach(ignorado -> contador.incrementar()))
                .then();
    }
}
