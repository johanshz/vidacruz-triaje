package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.comun.excepcion.ExcepcionConflicto;
import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import reactor.core.publisher.Mono;

public class ManejadorCambiarEstadoPaciente {
    private final DaoPaciente dao;
    private final RepositorioPaciente pacientes;
    private final RepositorioAuditoria auditoria;
    private final RepositorioOutbox outbox;
    private final EjecutorTransaccion transaccion;
    private final Clock reloj;

    public ManejadorCambiarEstadoPaciente(DaoPaciente dao, RepositorioPaciente pacientes,
            RepositorioAuditoria auditoria, RepositorioOutbox outbox, EjecutorTransaccion transaccion, Clock reloj) {
        this.dao = dao; this.pacientes = pacientes; this.auditoria = auditoria;
        this.outbox = outbox; this.transaccion = transaccion; this.reloj = reloj;
    }

    public Mono<DtoPaciente> ejecutar(ComandoCambiarEstadoPaciente comando) {
        return transaccion.ejecutar(() -> dao.consultar(comando.id(), reloj.instant())
                .switchIfEmpty(Mono.error(new ExcepcionRecursoNoEncontrado("No existe el paciente " + comando.id())))
                .flatMap(actual -> {
                    EstadoPaciente anterior = actual.estado();
                    anterior.validarTransicion(comando.estado());
                    var datos = new DatosRegistroPaciente(actual.nombreCompleto(), actual.edad(), actual.frecuenciaCardiaca(),
                            actual.presionSistolica(), actual.presionDiastolica(), actual.spo2(), actual.temperatura(),
                            actual.dolor(), actual.gcs(), actual.quejaPrincipal());
                    var entidad = new Paciente(actual.id(), datos, actual.nivel(), anterior, actual.escalado(),
                            actual.fechaRegistro(), actual.fechaEstado(), actual.fechaPrimeraConsulta())
                            .cambiarEstado(comando.estado(), reloj.instant().truncatedTo(ChronoUnit.MICROS));
                    String tipo = comando.estado() == EstadoPaciente.EN_CONSULTA ? "paciente_en_consulta" : "paciente_alta";
                    UUID idEvento = UUID.randomUUID();
                    var evento = new EventoPaciente(idEvento, tipo, 1, entidad.fechaEstado(),
                            new DatosEventoPaciente(entidad.id(), entidad.nivel(), anterior, entidad.estado(), entidad.escalado()));
                    return pacientes.cambiarEstado(comando.id(), entidad, anterior.name()).flatMap(actualizado -> {
                        if (!actualizado) return Mono.error(new ExcepcionConflicto("El estado del paciente cambió concurrentemente"));
                        return auditoria.registrar(entidad, tipo, anterior.name(), entidad.estado().name(), entidad.fechaEstado())
                                .then(outbox.agregar(evento, entidad.id().toString()))
                                .then(dao.consultar(comando.id(), reloj.instant()));
                    });
                }));
    }
}
