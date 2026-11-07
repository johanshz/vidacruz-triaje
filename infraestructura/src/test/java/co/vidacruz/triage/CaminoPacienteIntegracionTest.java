package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
class CaminoPacienteIntegracionTest extends SoporteIntegracionTriage {
    @Test void registraConsultaYTransicionaConPersistenciaAuditoriaYOutbox() {
        byte[] cuerpo = webTestClient.post().uri("/triage/pacientes")
                .header("X-API-Key", "clave-prueba").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombreCompleto\":\"Paciente Integración\",\"edad\":40,\"frecuenciaCardiaca\":70,\"presionSistolica\":120,\"presionDiastolica\":80,\"spo2\":98,\"temperatura\":36.5,\"dolor\":2,\"gcs\":15,\"quejaPrincipal\":\"Dolor\"}")
                .exchange().expectStatus().isCreated().expectBody()
                .jsonPath("$.nivel").isEqualTo("NO_URGENTE").jsonPath("$.estado").isEqualTo("REGISTRADO")
                .returnResult().getResponseBody();
        String respuesta = new String(cuerpo, java.nio.charset.StandardCharsets.UTF_8);
        String idTexto = respuesta.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");
        UUID id = UUID.fromString(idTexto);
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM paciente WHERE id=:id AND escalado=false AND fecha_estado=fecha_registro AND fecha_primera_consulta IS NULL")
                .bind("id", id).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id=:id AND tipo_evento='paciente_registrado' AND estado_anterior IS NULL AND estado_nuevo='REGISTRADO'")
                .bind("id", id).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave=:id AND tipo='paciente_registrado'")
                .bind("id", id.toString()).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
        webTestClient.get().uri("/triage/pacientes/{id}", id).header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.estado").isEqualTo("REGISTRADO");
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", id).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{" + "\"estado\":\"EN_CONSULTA\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.estado").isEqualTo("EN_CONSULTA");
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", id).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{" + "\"estado\":\"REGISTRADO\"}")
                .exchange().expectStatus().isEqualTo(409);
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id=:id").bind("id", id)
                .map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(2L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave=:id").bind("id", id.toString())
                .map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(2L).verifyComplete();
    }

    @Test void registroGuardaEnElOutboxElTraceparentDeLaPeticion() {
        var resultado = webTestClient.post().uri("/triage/pacientes")
                .header("X-API-Key", "clave-prueba").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombreCompleto\":\"Paciente Traza\",\"edad\":40,\"frecuenciaCardiaca\":70,"
                        + "\"presionSistolica\":120,\"presionDiastolica\":80,\"spo2\":98,\"temperatura\":36.5,"
                        + "\"dolor\":2,\"gcs\":15,\"quejaPrincipal\":\"Dolor\"}")
                .exchange().expectStatus().isCreated().expectBody(String.class).returnResult();
        String traceId = resultado.getResponseHeaders().getFirst("X-Trace-Id");
        String idTexto = resultado.getResponseBody().replaceAll(".*\\\"id\\\":\\\"([^\\\"]+).*", "$1");
        StepVerifier.create(databaseClient.sql("SELECT traceparent FROM outbox WHERE llave=:id AND tipo='paciente_registrado'")
                .bind("id", idTexto).map((row, metadata) -> row.get("traceparent", String.class)).one())
                .assertNext(traceparent -> {
                    org.assertj.core.api.Assertions.assertThat(traceId).isNotBlank();
                    org.assertj.core.api.Assertions.assertThat(traceparent)
                            .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]")
                            .contains("-" + traceId + "-");
                }).verifyComplete();
    }

    @Test void solicitudInvalidaResponde400ConCampoYCodigo() {
        verificarSolicitudInvalida();
    }

    private void verificarSolicitudInvalida() {
        webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(
                        "{\"nombreCompleto\":\"AB\",\"edad\":40,\"frecuenciaCardiaca\":70,"
                                + "\"presionSistolica\":120,\"presionDiastolica\":80,\"spo2\":98,"
                                + "\"temperatura\":36.5,\"dolor\":2,\"gcs\":15,\"quejaPrincipal\":\"Dolor\"}")
                .exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION").jsonPath("$.campo").isEqualTo("nombreCompleto");
    }

    @Test void registraSoloSumaLaMetricaDelNivelYUnRechazoNoLaCambia() {
        double criticosAntes = metricaClasificacion("CRITICO");
        double noUrgentesAntes = metricaClasificacion("NO_URGENTE");
        registrar("Paciente Crítico", 130, 180, 110, 97, 36.8, 8, 15, "Dolor torácico");
        registrar("Paciente Estable", 72, 120, 80, 99, 36.5, 2, 15, "Herida");
        assertEquals(criticosAntes + 1, metricaClasificacion("CRITICO"), 0.0);
        assertEquals(noUrgentesAntes + 1, metricaClasificacion("NO_URGENTE"), 0.0);

        verificarSolicitudInvalida();

        assertEquals(criticosAntes + 1, metricaClasificacion("CRITICO"), 0.0);
        assertEquals(noUrgentesAntes + 1, metricaClasificacion("NO_URGENTE"), 0.0);
    }

    private void registrar(String nombre, int fc, int sistolica, int diastolica, int spo2,
            double temperatura, int dolor, int gcs, String queja) {
        String cuerpo = "{\"nombreCompleto\":\"" + nombre + "\",\"edad\":40,\"frecuenciaCardiaca\":" + fc
                + ",\"presionSistolica\":" + sistolica + ",\"presionDiastolica\":" + diastolica
                + ",\"spo2\":" + spo2 + ",\"temperatura\":" + temperatura + ",\"dolor\":" + dolor
                + ",\"gcs\":" + gcs + ",\"quejaPrincipal\":\"" + queja + "\"}";
        webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange()
                .expectStatus().isCreated().expectBody().jsonPath("$.nivel").exists();
    }

    private double metricaClasificacion(String nivel) {
        var respuesta = webTestClient.get().uri("/actuator/prometheus").header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        String prefijo = "triaje_clasificaciones_total{nivel=\"" + nivel + "\"}";
        return respuesta.lines().filter(linea -> linea.startsWith(prefijo)).mapToDouble(linea ->
                Double.parseDouble(linea.substring(linea.lastIndexOf(' ') + 1))).findFirst().orElse(0);
    }
}
