package co.vidacruz.triage.evento.comando;

import co.vidacruz.triage.evento.puerto.repositorio.CandadoPublicacion;
import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;

public record DependenciasPublicacionOutbox(RepositorioOutbox outbox, CandadoPublicacion candado,
        PublicadorEventos publicador, EjecutorTransaccion transaccion,
        ContadorEventosPendientes contador, int lote) { }
