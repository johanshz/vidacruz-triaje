package co.vidacruz.triage.paciente.consulta;

import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import java.time.Clock;
import java.util.UUID;
import reactor.core.publisher.Mono;

public class ManejadorConsultarPaciente {
    private final DaoPaciente dao;
    private final Clock reloj;

    public ManejadorConsultarPaciente(DaoPaciente dao, Clock reloj) {
        this.dao = dao; this.reloj = reloj;
    }

    public Mono<DtoPaciente> ejecutar(UUID id) {
        return dao.consultar(id, reloj.instant())
                .switchIfEmpty(Mono.error(new ExcepcionRecursoNoEncontrado("No existe el paciente " + id)));
    }
}
