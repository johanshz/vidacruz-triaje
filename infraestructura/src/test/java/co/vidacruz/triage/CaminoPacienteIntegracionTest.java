package co.vidacruz.triage;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
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
    }

    @Test void requestInvalidoNoIncrementaClasificaciones() {
        webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{" + "\"nombreCompleto\":\"A\"}")
                .exchange().expectStatus().isBadRequest();
    }
}
