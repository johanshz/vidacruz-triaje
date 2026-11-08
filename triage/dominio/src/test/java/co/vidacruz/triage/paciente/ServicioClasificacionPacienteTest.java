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

    @Test void clasificaEjemplosDelEnunciado() {
        assertNivel(NivelTriage.CRITICO, datos(130, 180, 110, 97, "36.8", 8, 15, "Dolor torácico"));
        assertNivel(NivelTriage.URGENTE, datos(95, 150, 95, 91, "37.0", 4, 15, "Disnea"));
        assertNivel(NivelTriage.URGENTE_2, datos(88, 130, 85, 98, "38.2", 6, 15, "Fiebre"));
        assertNivel(NivelTriage.NO_URGENTE, datos(72, 120, 80, 99, "36.5", 2, 15, "Herida"));
        assertNivel(NivelTriage.NO_URGENTE_2, datos(70, 115, 75, 99, "36.4", 0, 15, "Renovación de recetario"));
        assertNivel(NivelTriage.CRITICO, datos(35, 80, 50, 85, "35.9", 0, 6, "Inconsciente"));
    }

    @Test void prevaleceLaGravedadYRespetaSla() {
        assertNivel(NivelTriage.CRITICO, datos(130, 180, 110, 91, "39.0", 9, 8, "Dolor"));
    }

    @Test void clasificaTodosLosBordesClinicosYDaPrioridadAlNivelMasGrave() {
        Object[][] casos = {
                {39, 120, 80, 99, "36.5", 2, 15, NivelTriage.CRITICO},
                {40, 120, 80, 99, "36.5", 2, 15, NivelTriage.NO_URGENTE},
                {89, 120, 80, 99, "36.5", 2, 15, NivelTriage.NO_URGENTE},
                {90, 120, 80, 99, "36.5", 2, 15, NivelTriage.URGENTE_2},
                {120, 120, 80, 99, "36.5", 2, 15, NivelTriage.URGENTE_2},
                {121, 120, 80, 99, "36.5", 2, 15, NivelTriage.CRITICO},
                {72, 120, 80, 89, "36.5", 2, 15, NivelTriage.CRITICO},
                {72, 120, 80, 90, "36.5", 2, 15, NivelTriage.URGENTE},
                {72, 120, 80, 93, "36.5", 2, 15, NivelTriage.URGENTE},
                {72, 120, 80, 94, "36.5", 2, 15, NivelTriage.NO_URGENTE},
                {72, 89, 80, 99, "36.5", 2, 15, NivelTriage.CRITICO},
                {72, 90, 80, 99, "36.5", 2, 15, NivelTriage.URGENTE},
                {72, 100, 80, 99, "36.5", 2, 15, NivelTriage.URGENTE},
                {72, 101, 80, 99, "36.5", 2, 15, NivelTriage.NO_URGENTE},
                {72, 120, 80, 99, "37.9", 2, 15, NivelTriage.NO_URGENTE},
                {72, 120, 80, 99, "38.0", 2, 15, NivelTriage.URGENTE_2},
                {72, 120, 80, 99, "38.4", 2, 15, NivelTriage.URGENTE_2},
                {72, 120, 80, 99, "38.5", 2, 15, NivelTriage.URGENTE},
                {72, 120, 80, 99, "36.5", 2, 8, NivelTriage.CRITICO},
                {72, 120, 80, 99, "36.5", 2, 9, NivelTriage.URGENTE},
                {72, 120, 80, 99, "36.5", 2, 13, NivelTriage.URGENTE},
                {72, 120, 80, 99, "36.5", 2, 14, NivelTriage.NO_URGENTE}
        };

        for (Object[] caso : casos) {
            var entrada = datos((int) caso[0], (int) caso[1], (int) caso[2], (int) caso[3],
                    (String) caso[4], (int) caso[5], (int) caso[6], "Consulta");
            assertNivel((NivelTriage) caso[7], entrada);
        }

        assertNivel(NivelTriage.URGENTE, datos(95, 150, 95, 91, "38.0", 4, 15, "Consulta"));
        assertNivel(NivelTriage.CRITICO, datos(130, 180, 110, 97, "38.5", 8, 15, "Consulta"));
        assertNivel(NivelTriage.URGENTE_2, datos(72, 120, 80, 99, "36.5", 7, 15, "Consulta"));
        assertNivel(NivelTriage.NO_URGENTE, datos(72, 120, 80, 99, "36.5", 6, 15, "Consulta"));
    }

    @Test void clasificaQuejasAdministrativasPorIgualdadNormalizada() {
        for (String queja : java.util.List.of("documentos", "Recolección de resultados", "RENOVACIÓN  de   recetario ",
                "vacunación", " seguimiento ")) {
            assertNivel(NivelTriage.NO_URGENTE_2, datos(72, 120, 80, 99, "36.5", 2, 15, queja));
        }
        assertNivel(NivelTriage.NO_URGENTE, datos(72, 120, 80, 99, "36.5", 2, 15,
                "Renovación de recetario urgente"));
        assertNivel(NivelTriage.URGENTE_2, datos(90, 120, 80, 99, "36.5", 2, 15, "Vacunación"));
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
