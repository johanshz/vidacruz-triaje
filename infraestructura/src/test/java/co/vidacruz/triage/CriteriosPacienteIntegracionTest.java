package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CriteriosPacienteIntegracionTest extends SoporteIntegracionTriage {
    @LocalServerPort
    private int puerto;
    private static final String REGISTRO_VALIDO = "{\"nombreCompleto\":\"Paciente QA\",\"edad\":40,"
            + "\"frecuenciaCardiaca\":70,\"presionSistolica\":120,\"presionDiastolica\":80,"
            + "\"spo2\":98,\"temperatura\":36.5,\"dolor\":2,\"gcs\":15,\"quejaPrincipal\":\"Dolor\"}";

    @Test void rangosInvalidosInformanCampoYNoEscribenPacienteAuditoriaNiOutbox() {
        List<String[]> casos = List.of(
                new String[]{"edad", "\"edad\":-1"}, new String[]{"edad", "\"edad\":121"},
                new String[]{"frecuenciaCardiaca", "\"frecuenciaCardiaca\":19"},
                new String[]{"frecuenciaCardiaca", "\"frecuenciaCardiaca\":251"},
                new String[]{"presionSistolica", "\"presionSistolica\":59"},
                new String[]{"presionSistolica", "\"presionSistolica\":261"},
                new String[]{"presionDiastolica", "\"presionDiastolica\":29"},
                new String[]{"presionDiastolica", "\"presionDiastolica\":161"},
                new String[]{"spo2", "\"spo2\":69"}, new String[]{"spo2", "\"spo2\":101"},
                new String[]{"temperatura", "\"temperatura\":33.9"},
                new String[]{"temperatura", "\"temperatura\":42.1"},
                new String[]{"dolor", "\"dolor\":-1"}, new String[]{"dolor", "\"dolor\":11"},
                new String[]{"gcs", "\"gcs\":2"}, new String[]{"gcs", "\"gcs\":16"});

        for (String[] caso : casos) {
            AtomicLong antes = new AtomicLong();
            StepVerifier.create(totalFilasPacienteAuditoriaOutbox()).assertNext(antes::set).verifyComplete();
            String cuerpo = reemplazarCampo(caso[0], caso[1]);
            webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange()
                    .expectStatus().isBadRequest().expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                    .jsonPath("$.campo").isEqualTo(caso[0]).jsonPath("$.traceId").exists();
            StepVerifier.create(totalFilasPacienteAuditoriaOutbox()).expectNext(antes.get()).verifyComplete();
        }
    }

    @Test void formatosYCamposInvalidosSeTraducenAValidacion() {
        List<String[]> casos = List.of(
                new String[]{"spo2", REGISTRO_VALIDO.replace("\"spo2\":98,", "")},
                new String[]{"frecuenciaCardiaca", REGISTRO_VALIDO.replace("\"frecuenciaCardiaca\":70", "\"frecuenciaCardiaca\":95.5")},
                new String[]{"edad", REGISTRO_VALIDO.replace("\"edad\":40", "\"edad\":\"treinta\"")},
                new String[]{"temperatura", REGISTRO_VALIDO.replace("36.5", "36.85")},
                new String[]{"nombreCompleto", REGISTRO_VALIDO.replace("Paciente QA", "AB")},
                new String[]{"nombreCompleto", REGISTRO_VALIDO.replace("Paciente QA", "A".repeat(101))},
                new String[]{"quejaPrincipal", REGISTRO_VALIDO.replace("Dolor", "Q".repeat(501))},
                new String[]{"presionDiastolica", REGISTRO_VALIDO.replace("\"presionDiastolica\":80", "\"presionDiastolica\":120")});

        for (String[] caso : casos) {
            webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(caso[1]).exchange()
                    .expectStatus().isBadRequest().expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                    .jsonPath("$.campo").isEqualTo(caso[0]);
        }
        webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{").exchange()
                .expectStatus().isBadRequest().expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION");
    }

    @Test void consultaInexistenteYUuidInvalidoUsanElContratoDeError() {
        webTestClient.get().uri("/triage/pacientes/{id}", UUID.randomUUID()).header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isNotFound().expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO")
                .jsonPath("$.detail").isEqualTo("Paciente no encontrado");
        webTestClient.get().uri("/triage/pacientes/abc").header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.campo").isEqualTo("id");
    }

    @Test void todosLosEndpointsDelPacienteRequierenApiKeyYNoEscribenSinAutorizacion() {
        AtomicLong antes = new AtomicLong();
        StepVerifier.create(totalFilasPacienteAuditoriaOutbox()).assertNext(antes::set).verifyComplete();
        UUID id = UUID.randomUUID();
        webTestClient.post().uri("/triage/pacientes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(REGISTRO_VALIDO).exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");
        webTestClient.get().uri("/triage/pacientes/{id}", id).header("X-API-Key", "clave-incorrecta")
                .exchange().expectStatus().isUnauthorized().expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", id).header("X-API-Key", "clave-incorrecta")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"ALTA\"}")
                .exchange().expectStatus().isUnauthorized().expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");
        StepVerifier.create(totalFilasPacienteAuditoriaOutbox()).expectNext(antes.get()).verifyComplete();
    }

    @Test void consultaExistenteRetornaFichaCompletaYRedondeaElTiempoAbajo() {
        UUID id = UUID.randomUUID();
        insertarPacienteConFechas(id, "REGISTRADO", Duration.ofSeconds(220));
        webTestClient.get().uri("/triage/pacientes/{id}", id).header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.nombreCompleto").isEqualTo("Paciente Tiempo").jsonPath("$.edad").isEqualTo(30)
                .jsonPath("$.nivel").isEqualTo("URGENTE").jsonPath("$.slaMinutos").isEqualTo(30)
                .jsonPath("$.estado").isEqualTo("REGISTRADO").jsonPath("$.escalado").isEqualTo(false)
                .jsonPath("$.fechaRegistro").exists().jsonPath("$.fechaEstado").exists()
                .jsonPath("$.fechaPrimeraConsulta").isEmpty().jsonPath("$.tiempoEnEstadoActualMinutos").isEqualTo(3);
    }

    @Test void aceptaLosExtremosValidosPorHttp() {
        String minimo = "{\"nombreCompleto\":\"Ana\",\"edad\":0,\"frecuenciaCardiaca\":20,"
                + "\"presionSistolica\":60,\"presionDiastolica\":30,\"spo2\":70,\"temperatura\":34.0,"
                + "\"dolor\":0,\"gcs\":3,\"quejaPrincipal\":\"Q\"}";
        String maximo = "{\"nombreCompleto\":\"" + "A".repeat(100) + "\",\"edad\":120,"
                + "\"frecuenciaCardiaca\":250,\"presionSistolica\":260,\"presionDiastolica\":160,"
                + "\"spo2\":100,\"temperatura\":42.0,\"dolor\":10,\"gcs\":15,"
                + "\"quejaPrincipal\":\"" + "Q".repeat(500) + "\"}";
        for (String cuerpo : List.of(minimo, maximo)) {
            webTestClient.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange()
                    .expectStatus().isCreated().expectBody().jsonPath("$.id").exists();
        }
    }

    @Test void patchRechazaEstadosInvalidosYMatrizDeTransicionesProhibidas() throws Exception {
        UUID pacienteRegistrado = UUID.randomUUID();
        UUID pacienteConsulta = UUID.randomUUID();
        UUID pacienteAlta = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteRegistrado).then(insertarPaciente(pacienteConsulta, "URGENTE", "EN_CONSULTA"))
                .then(insertarPaciente(pacienteAlta, "URGENTE", "ALTA"))).expectNext(1L).verifyComplete();

        for (String cuerpo : List.of("{\"estado\":\"CERRADO\"}", "{}", "{\"estado\":null}")) {
            var resultado = webTestClient.patch().uri("/triage/pacientes/{id}/estado", pacienteRegistrado)
                    .header("X-API-Key", "clave-prueba").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cuerpo).exchange().expectStatus().isBadRequest().expectBody()
                    .jsonPath("$.codigo").isEqualTo("VALIDACION").returnResult();
            if (!cuerpo.equals("{\"estado\":\"CERRADO\"}")) {
                assertEquals("estado", new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(resultado.getResponseBody()).get("campo").asText());
            }
        }
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", pacienteRegistrado)
                .header("X-API-Key", "clave-prueba").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{").exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION");
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", UUID.randomUUID()).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"ALTA\"}")
                .exchange().expectStatus().isNotFound().expectBody().jsonPath("$.detail").isEqualTo("Paciente no encontrado");
        webTestClient.patch().uri("/triage/pacientes/abc/estado").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"ALTA\"}")
                .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.campo").isEqualTo("id");

        assertTransicionInvalida(pacienteRegistrado, "REGISTRADO", "REGISTRADO");
        assertTransicionInvalida(pacienteConsulta, "EN_CONSULTA", "REGISTRADO");
        assertTransicionInvalida(pacienteConsulta, "EN_CONSULTA", "EN_CONSULTA");
        assertTransicionInvalida(pacienteAlta, "ALTA", "REGISTRADO");
        assertTransicionInvalida(pacienteAlta, "ALTA", "EN_CONSULTA");
        assertTransicionInvalida(pacienteAlta, "ALTA", "ALTA");
    }

    @Test void lasFechasYElTiempoEnEstadoReflejanElUltimoCambio() {
        UUID paciente = UUID.randomUUID();
        insertarPacienteConFechas(paciente, "REGISTRADO", Duration.ofMinutes(5));
        webTestClient.get().uri("/triage/pacientes/{id}", paciente).header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.tiempoEnEstadoActualMinutos").isEqualTo(5)
                .jsonPath("$.fechaRegistro").exists().jsonPath("$.fechaEstado").exists()
                .jsonPath("$.fechaPrimeraConsulta").isEmpty();

        webTestClient.patch().uri("/triage/pacientes/{id}/estado", paciente).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"EN_CONSULTA\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.fechaPrimeraConsulta").exists();
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", paciente).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"ALTA\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.fechaPrimeraConsulta").exists();

        UUID altaDirecta = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(altaDirecta)).expectNext(1L).verifyComplete();
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", altaDirecta).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"ALTA\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.fechaPrimeraConsulta").isEmpty();
    }

    @Test void dosCambiosConcurrentesDejanUnaSolaAuditoriaYUnSoloEvento() {
        UUID id = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(id)).expectNext(1L).verifyComplete();
        Mono<HttpStatusCode> solicitudUno = patchConcurrente(id);
        Mono<HttpStatusCode> solicitudDos = patchConcurrente(id);

        StepVerifier.create(Mono.zip(solicitudUno, solicitudDos).map(estados -> List.of(estados.getT1(), estados.getT2())))
                .assertNext(estados -> {
                    assertEquals(1, estados.stream().filter(HttpStatusCode::is2xxSuccessful).count());
                    assertEquals(1, estados.stream().filter(status -> status.value() == 409).count());
                }).expectComplete().verify(Duration.ofSeconds(15));
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id=:id AND tipo_evento='paciente_en_consulta'")
                .bind("id", id).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave=:id AND tipo='paciente_en_consulta'")
                .bind("id", id.toString()).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
    }

    private Mono<HttpStatusCode> patchConcurrente(UUID id) {
        return WebClient.create("http://localhost:" + puerto).patch().uri("/triage/pacientes/{id}/estado", id)
                .header("X-API-Key", "clave-prueba").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\":\"EN_CONSULTA\"}")
                .exchangeToMono(respuesta -> respuesta.releaseBody().thenReturn(respuesta.statusCode()));
    }

    private void assertTransicionInvalida(UUID id, String anterior, String destino) {
        webTestClient.patch().uri("/triage/pacientes/{id}/estado", id).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"" + destino + "\"}")
                .exchange().expectStatus().isEqualTo(409).expectBody().jsonPath("$.codigo").isEqualTo("TRANSICION_INVALIDA")
                .jsonPath("$.detail").isEqualTo("No se puede pasar de " + anterior + " a " + destino);
        StepVerifier.create(databaseClient.sql("SELECT estado FROM paciente WHERE id=:id").bind("id", id)
                .map((row, metadata) -> row.get("estado", String.class)).one()).expectNext(anterior).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id=:id")
                .bind("id", id).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(0L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave=:id")
                .bind("id", id.toString()).map((row, metadata) -> row.get("total", Long.class)).one()).expectNext(0L).verifyComplete();
    }

    private Mono<Long> totalFilasPacienteAuditoriaOutbox() {
        return databaseClient.sql("SELECT (SELECT COUNT(*) FROM paciente) + (SELECT COUNT(*) FROM auditoria) + (SELECT COUNT(*) FROM outbox) AS total")
                .map((row, metadata) -> row.get("total", Long.class)).one();
    }

    private String reemplazarCampo(String nombre, String campo) {
        int inicio = REGISTRO_VALIDO.indexOf('"' + nombre + '"');
        int fin = REGISTRO_VALIDO.indexOf(',', inicio);
        if (fin < 0) fin = REGISTRO_VALIDO.indexOf('}', inicio);
        return REGISTRO_VALIDO.substring(0, inicio) + campo + REGISTRO_VALIDO.substring(fin);
    }

    private void insertarPacienteConFechas(UUID id, String estado, Duration antiguedad) {
        var fecha = OffsetDateTime.now(ZoneOffset.UTC).minus(antiguedad);
        StepVerifier.create(databaseClient.sql("INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica, presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel, sla_minutos, estado, fecha_registro, fecha_estado) VALUES (:id,'Paciente Tiempo',30,80,120,80,98,36.5,0,15,'Dolor','URGENTE',30,:estado,:fecha,:fecha)")
                .bind("id", id).bind("estado", estado).bind("fecha", fecha).fetch().rowsUpdated()).expectNext(1L).verifyComplete();
    }
}
