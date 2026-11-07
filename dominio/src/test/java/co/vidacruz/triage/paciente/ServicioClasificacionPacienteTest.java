package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ServicioClasificacionPacienteTest {
    private final ServicioClasificacionPaciente servicio = new ServicioClasificacionPaciente();

    @Test void clasificaEjemplosCeiba() {
        assertNivel(NivelTriage.CRITICO, datos(130, 180, 110, 97, "36.8", 8, 15, "Dolor torácico"));
        assertNivel(NivelTriage.URGENTE, datos(95, 150, 95, 91, "37.0", 4, 15, "Disnea"));
        assertNivel(NivelTriage.URGENTE_2, datos(88, 130, 85, 98, "38.2", 6, 15, "Fiebre"));
        assertNivel(NivelTriage.NO_URGENTE, datos(72, 120, 80, 99, "36.5", 2, 15, "Herida"));
        assertNivel(NivelTriage.NO_URGENTE_2, datos(70, 115, 75, 99, "36.4", 0, 15, "Renovación de recetario"));
        assertNivel(NivelTriage.CRITICO, datos(35, 80, 50, 85, "35.9", 0, 6, "Inconsciente"));
    }

    @Test void prevaleceLaGravedadYRespetaSla() {
        assertEquals(5, NivelTriage.CRITICO.slaMinutos());
        assertNivel(NivelTriage.CRITICO, datos(130, 180, 110, 91, "39.0", 9, 8, "Dolor"));
    }

    private void assertNivel(NivelTriage esperado, DatosRegistroPaciente datos) {
        StepVerifier.create(Mono.fromCallable(() -> servicio.clasificar(datos)))
                .assertNext(nivel -> assertEquals(esperado, nivel)).verifyComplete();
    }

    static DatosRegistroPaciente datos(int fc, int ps, int pd, int spo2, String temperatura, int dolor, int gcs, String queja) {
        return new DatosRegistroPaciente("Paciente válido", 40, fc, ps, pd, spo2,
                new BigDecimal(temperatura), dolor, gcs, queja);
    }
}
