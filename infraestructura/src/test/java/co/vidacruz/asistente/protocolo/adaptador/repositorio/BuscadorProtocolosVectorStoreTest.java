package co.vidacruz.asistente.protocolo.adaptador.repositorio;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.time.Duration;
import co.vidacruz.asistente.configuracion.PropiedadesIa;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class BuscadorProtocolosVectorStoreTest {
    @Test
    void convierteResultadosYMetadatosDelIndiceAFragmentos() {
        VectorStore vectorStore = mock(VectorStore.class);
        Document documento = new Document("Reposo inicial", Map.of(
                "protocolo", "PROTO-001", "titulo", "Crisis hipertensiva", "seccion", "Medidas iniciales"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(documento));
        var buscador = crearBuscador(vectorStore, Duration.ofSeconds(1));

        StepVerifier.create(buscador.buscar("manejo inicial", 0.72, 4))
                .assertNext(fragmento -> {
                    assertEquals("PROTO-001", fragmento.codigo());
                    assertEquals("Crisis hipertensiva", fragmento.titulo());
                    assertEquals("Medidas iniciales", fragmento.seccion());
                    assertEquals("Reposo inicial", fragmento.texto());
                })
                .verifyComplete();
    }

    @Test
    void rechazaResultadosConMetadatosObligatoriosVacios() {
        VectorStore vectorStore = mock(VectorStore.class);
        Document documento = new Document("Texto", Map.of(
                "protocolo", "PROTO-001", "titulo", "Título", "seccion", " "));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(documento));
        var buscador = crearBuscador(vectorStore, Duration.ofSeconds(1));

        StepVerifier.create(buscador.buscar("pregunta", 0.75, 3))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("seccion"))
                .verify();
    }

    @Test
    void devuelveFlujoVacioCuandoNoHayCoincidencias() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        StepVerifier.create(crearBuscador(vectorStore, Duration.ofSeconds(1)).buscar("sin resultado", 0.75, 3))
                .verifyComplete();
    }

    @Test
    void convierteTimeoutDeEmbeddingsEnIaNoDisponible() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocacion -> {
            Thread.sleep(250);
            return List.of();
        });
        var buscador = crearBuscador(vectorStore, Duration.ofMillis(30));

        StepVerifier.create(buscador.buscar("pregunta", 0.75, 3))
                .expectError(co.vidacruz.asistente.protocolo.excepcion.ExcepcionIaNoDisponible.class)
                .verify(Duration.ofSeconds(2));
    }

    private BuscadorProtocolosVectorStore crearBuscador(VectorStore vectorStore, Duration timeout) {
        var resiliencia = new PropiedadesIa.Resiliencia(Duration.ofSeconds(10), timeout,
                10, 5, 50.0f, Duration.ofSeconds(30), 2);
        return new BuscadorProtocolosVectorStore(vectorStore,
                new PropiedadesIa("claude-sonnet-5-5", 0.75, 3, resiliencia));
    }
}
