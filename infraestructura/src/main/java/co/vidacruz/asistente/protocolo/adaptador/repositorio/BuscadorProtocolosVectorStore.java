package co.vidacruz.asistente.protocolo.adaptador.repositorio;

import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class BuscadorProtocolosVectorStore implements BuscadorProtocolos {
    private final VectorStore vectorStore;

    public BuscadorProtocolosVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Flux<FragmentoProtocolo> buscar(String pregunta, double umbral, int topK) {
        SearchRequest solicitud = SearchRequest.builder()
                .query(pregunta)
                .similarityThreshold(umbral)
                .topK(topK)
                .build();
        return Mono.fromCallable(() -> vectorStore.similaritySearch(solicitud))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::convertir);
    }

    private FragmentoProtocolo convertir(Document documento) {
        Map<String, Object> metadatos = documento.getMetadata();
        return new FragmentoProtocolo(texto(metadatos, "protocolo"), texto(metadatos, "titulo"),
                texto(metadatos, "seccion"), documento.getText());
    }

    private String texto(Map<String, Object> metadatos, String nombre) {
        Object valor = metadatos.get(nombre);
        if (!(valor instanceof String texto) || texto.isBlank()) {
            throw new IllegalStateException("El índice contiene metadatos inválidos: " + nombre);
        }
        return texto;
    }
}
