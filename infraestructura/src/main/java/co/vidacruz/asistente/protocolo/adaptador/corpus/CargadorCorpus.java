package co.vidacruz.asistente.protocolo.adaptador.corpus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class CargadorCorpus implements ApplicationRunner {
    private static final List<String> ARCHIVOS = List.of(
            "corpus/PROTO-001.json", "corpus/PROTO-002.json", "corpus/PROTO-003.json");
    private static final Map<String, Integer> SECCIONES_ESPERADAS = Map.of(
            "PROTO-001", 3, "PROTO-002", 2, "PROTO-003", 2);

    private final ObjectMapper mapper;
    private final VectorStore vectorStore;

    public CargadorCorpus(ObjectMapper mapper, VectorStore vectorStore) {
        this.mapper = mapper;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Mono.fromRunnable(this::cargarEIndexar)
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .block();
    }

    private void cargarEIndexar() {
        try {
            List<Document> documentos = new ArrayList<>();
            for (String archivo : ARCHIVOS) {
                ProtocoloCorpus protocolo;
                try (var entrada = new ClassPathResource(archivo).getInputStream()) {
                    protocolo = mapper.readValue(entrada, ProtocoloCorpus.class);
                }
                validar(protocolo);
                protocolo.secciones().forEach(seccion -> documentos.add(new Document(seccion.texto(), Map.of(
                        "protocolo", protocolo.codigo(), "titulo", protocolo.titulo(), "seccion", seccion.nombre()))));
            }
            if (documentos.size() != 7) {
                throw new IllegalStateException("El corpus debe contener exactamente siete secciones");
            }
            vectorStore.add(documentos);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("No se pudo cargar o indexar el corpus clínico", error);
        }
    }

    private void validar(ProtocoloCorpus protocolo) {
        if (protocolo == null || protocolo.codigo() == null || protocolo.titulo() == null
                || protocolo.secciones() == null || protocolo.secciones().size()
                        != SECCIONES_ESPERADAS.getOrDefault(protocolo.codigo(), -1)
                || protocolo.titulo().isBlank()
                || protocolo.secciones().stream().anyMatch(seccion -> seccion == null
                        || seccion.nombre() == null || seccion.nombre().isBlank()
                        || seccion.texto() == null || seccion.texto().isBlank())) {
            throw new IllegalStateException("El archivo del corpus tiene un esquema o contenido inválido");
        }
    }

    record ProtocoloCorpus(String codigo, String titulo, List<SeccionCorpus> secciones) {
    }

    record SeccionCorpus(String nombre, String texto) {
    }
}
