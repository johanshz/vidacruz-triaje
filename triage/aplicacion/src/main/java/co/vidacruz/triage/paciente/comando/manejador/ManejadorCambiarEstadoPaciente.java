package co.vidacruz.triage.paciente.comando.manejador;

import co.vidacruz.triage.paciente.comando.ComandoCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.fabrica.FabricaPaciente;

import co.vidacruz.triage.paciente.excepcion.ExcepcionTransicionInvalida;
import co.vidacruz.triage.paciente.excepcion.ExcepcionPacienteNoEncontrado;
import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import reactor.core.publisher.Mono;

public class ManejadorCambiarEstadoPaciente {
    private final DaoPaciente dao;
    private final RepositorioPaciente pacientes;
    private final RegistrarCambioPaciente registrarCambio;
    private final EjecutorTransaccion transaccion;
    private final Clock reloj;

    public ManejadorCambiarEstadoPaciente(DependenciasCambioEstadoPaciente dependencias) {
        this.dao = dependencias.dao();
        this.pacientes = dependencias.pacientes();
        this.registrarCambio = dependencias.registrarCambio();
        this.transaccion = dependencias.transaccion();
        this.reloj = dependencias.reloj();
    }

    public Mono<DtoPaciente> ejecutar(ComandoCambiarEstadoPaciente comando) {
        return transaccion.ejecutar(() -> dao.consultar(comando.id(), reloj.instant())
                .switchIfEmpty(Mono.error(new ExcepcionPacienteNoEncontrado()))
                .flatMap(actual -> {
                    EstadoPaciente anterior = actual.estado();
                    anterior.validarTransicion(comando.estado());
                    Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
                    var entidad = FabricaPaciente.desde(actual).cambiarEstado(comando.estado(), ahora);
                    TipoEventoPaciente tipoEvento = comando.estado() == EstadoPaciente.EN_CONSULTA
                            ? TipoEventoPaciente.PACIENTE_EN_CONSULTA : TipoEventoPaciente.PACIENTE_ALTA;
                    String tipo = tipoEvento.valor();
                    UUID idEvento = UUID.randomUUID();
                    var evento = new EventoPaciente(idEvento, tipo, EventoPaciente.VERSION_ACTUAL,
                            entidad.fechaEstado(), new DatosEventoPaciente(entidad.id(), entidad.nivel().name(),
                                    anterior.name(), entidad.estado().name(), entidad.escalado()));
                    return pacientes.cambiarEstado(entidad, anterior)
                            .flatMap(actualizado -> {
                                if (!actualizado) {
                                    return Mono.error(new ExcepcionTransicionInvalida(anterior, comando.estado()));
                                }
                                return registrarCambio.registrar(entidad, evento, anterior)
                                        .then(dao.consultar(comando.id(), reloj.instant()));
                            });
                }));
    }
}

