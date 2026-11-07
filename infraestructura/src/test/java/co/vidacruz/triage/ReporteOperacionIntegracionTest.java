package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vidacruz.vigilancia.intervalo=PT1H", "vidacruz.outbox.intervalo=PT1H",
                "spring.kafka.listener.auto-startup=false"})
class ReporteOperacionIntegracionTest extends SoporteIntegracionTriage {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final RangoReporte RANGO = RangoReporte.desdeParametros(
            "2026-11-08T10:00:00-05:00", "2026-11-08T12:00:00-05:00");
    @Autowired private DaoReporteOperacion dao;

    @Test
    void agrupaEnZonaCalculaEsperaP95AcumuladoYNiveles() {
        UUID pacienteCritico = UUID.randomUUID();
        UUID pacienteUrgente = UUID.randomUUID();
        UUID pacienteSinConsulta = UUID.randomUUID();
        UUID pacienteHoraSiguiente = UUID.randomUUID();
        insertar(pacienteCritico, "CRITICO", "2026-11-08T15:05:00Z", "2026-11-08T15:06:00Z");
        insertar(pacienteUrgente, "URGENTE", "2026-11-08T15:20:00Z", "2026-11-08T15:23:00Z");
        insertar(pacienteSinConsulta, "NO_URGENTE", "2026-11-08T15:40:00Z", null);
        insertar(pacienteHoraSiguiente, "URGENTE_2", "2026-11-08T16:00:00Z", null);
        insertar(UUID.randomUUID(), "NO_URGENTE_2", "2026-11-08T16:59:00Z", null);
        insertar(UUID.randomUUID(), "CRITICO", "2026-11-08T17:00:00Z", null);

        StepVerifier.create(dao.consultar(RANGO, BOGOTA).collectList())
                .assertNext(horas -> {
                    assertEquals(2, horas.size());
                    assertEquals(OffsetDateTime.parse("2026-11-08T10:00:00-05:00"), horas.get(0).hora());
                    assertEquals(3, horas.get(0).totalRegistrados());
                    assertEquals(3, horas.get(0).acumuladoRegistrados());
                    assertEquals(1, horas.get(0).porNivel().get("CRITICO"));
                    assertEquals(1, horas.get(0).porNivel().get("URGENTE"));
                    assertEquals(1, horas.get(0).porNivel().get("NO_URGENTE"));
                    assertEquals(0, horas.get(0).esperaPromedioMinutos().compareTo(new BigDecimal("2.0")));
                    assertEquals(0, horas.get(0).esperaP95Minutos().compareTo(new BigDecimal("2.9")));
                    assertEquals(2, horas.get(1).totalRegistrados());
                    assertEquals(5, horas.get(1).acumuladoRegistrados());
                    assertEquals(1, horas.get(1).porNivel().get("URGENTE_2"));
                    assertEquals(1, horas.get(1).porNivel().get("NO_URGENTE_2"));
                    assertNull(horas.get(1).esperaPromedioMinutos());
                    assertNull(horas.get(1).esperaP95Minutos());
                }).verifyComplete();
    }

    @Test
    void rangoSinRegistrosNoDevuelveHoras() {
        var rangoVacio = RangoReporte.desdeParametros("2026-11-10T00:00:00Z", "2026-11-11T00:00:00Z");

        StepVerifier.create(dao.consultar(rangoVacio, BOGOTA).collectList())
                .expectNext(List.of()).verifyComplete();
    }

    private void insertar(UUID id, String nivel, String registroTexto, String consultaTexto) {
        Instant registro = Instant.parse(registroTexto);
        Instant consulta = consultaTexto == null ? null : Instant.parse(consultaTexto);
        var insercion = databaseClient.sql("""
                INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel,
                    sla_minutos, estado, escalado, fecha_registro, fecha_estado, fecha_primera_consulta)
                VALUES (:id, 'Paciente reporte', 30, 80, 120, 80, 98, 36.5, 0, 15, 'Dolor', :nivel,
                    30, 'REGISTRADO', false, :registro, :registro, :consulta)
                """).bind("id", id).bind("nivel", nivel).bind("registro", registro);
        if (consulta == null) {
            insercion = insercion.bindNull("consulta", Instant.class);
        } else {
            insercion = insercion.bind("consulta", consulta);
        }
        StepVerifier.create(insercion.fetch().rowsUpdated()).expectNext(1L).verifyComplete();
    }
}
