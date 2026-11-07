package co.vidacruz.triage.vigilancia.comando;

import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.vigilancia.puerto.repositorio.ContadorEscalamientos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.vigilancia.servicio.ServicioEscalamiento;
import java.time.Clock;

public record DependenciasEscalamiento(RepositorioEscalamiento repositorio, RepositorioAuditoria auditoria,
        RepositorioOutbox outbox, EjecutorTransaccion transaccion, ServicioEscalamiento servicio,
        ContadorEscalamientos contador, Clock reloj) { }
