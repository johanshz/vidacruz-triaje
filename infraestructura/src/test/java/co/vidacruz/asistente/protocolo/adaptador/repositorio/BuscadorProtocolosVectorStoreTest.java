package co.vidacruz.asistente.protocolo.adaptador.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
        var buscador = new BuscadorProtocolosVectorStore(vectorStore);

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
        var buscador = new BuscadorProtocolosVectorStore(vectorStore);

        StepVerifier.create(buscador.buscar("pregunta", 0.75, 3))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("seccion"))
                .verify();
    }

    @Test
    void devuelveFlujoVacioCuandoNoHayCoincidencias() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        StepVerifier.create(new BuscadorProtocolosVectorStore(vectorStore).buscar("sin resultado", 0.75, 3))
                .verifyComplete();
    }
}
