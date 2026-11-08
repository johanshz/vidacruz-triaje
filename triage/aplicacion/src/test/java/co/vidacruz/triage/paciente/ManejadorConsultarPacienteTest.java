package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.triage.paciente.consulta.ManejadorConsultarPaciente;
import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorConsultarPacienteTest {
    private final DaoPaciente dao = mock(DaoPaciente.class);
    private final Instant ahora = Instant.parse("2026-11-06T01:00:00Z");
    private final UUID id = UUID.randomUUID();

    @Test void consultaPorDaoConRelojControlado() {
        var dto = new DtoPaciente(id, "Ana", 40, 70, 120, 80, 98, new BigDecimal("36.5"), 2, 15,
                "Dolor", NivelTriage.NO_URGENTE, 60, EstadoPaciente.REGISTRADO, false,
                ahora.minusSeconds(7200), ahora.minusSeconds(3599), null, 59);
        when(dao.consultar(id, ahora)).thenReturn(Mono.just(dto));
        StepVerifier.create(manejador().ejecutar(id))
                .assertNext(resultado -> assertEquals(59, resultado.tiempoEnEstadoActualMinutos())).verifyComplete();
        verify(dao).consultar(id, ahora);
    }

    @Test void pacienteAusenteProduce404DeDominio() {
        when(dao.consultar(id, ahora)).thenReturn(Mono.empty());
        StepVerifier.create(manejador().ejecutar(id)).expectError(ExcepcionRecursoNoEncontrado.class).verify();
    }

    private ManejadorConsultarPaciente manejador() {
        return new ManejadorConsultarPaciente(dao, Clock.fixed(ahora, ZoneOffset.UTC));
    }
}
