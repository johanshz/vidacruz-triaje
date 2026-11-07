package co.vidacruz.asistente.protocolo.adaptador.corpus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import co.vidacruz.asistente.protocolo.adaptador.MetadatosProtocolo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;

public final class CargadorCorpus implements ApplicationRunner {
    private static final List<String> ARCHIVOS = List.of(
            "corpus/PROTO-001.json", "corpus/PROTO-002.json", "corpus/PROTO-003.json");
    private static final Map<String, Integer> SECCIONES_ESPERADAS = Map.of(
            "PROTO-001", 3, "PROTO-002", 2, "PROTO-003", 2);
    private static final int TOTAL_SECCIONES = 7;

    private final ObjectMapper mapper;
    private final VectorStore vectorStore;

    public CargadorCorpus(ObjectMapper mapper, VectorStore vectorStore) {
        this.mapper = mapper;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        cargarEIndexar();
    }

    private void cargarEIndexar() {
        try {
            List<Document> documentos = ARCHIVOS.stream().flatMap(this::leerDocumentos).toList();
            if (documentos.size() != TOTAL_SECCIONES) {
                throw new IllegalStateException("El corpus debe contener exactamente siete secciones");
            }
            vectorStore.add(documentos);
        } catch (RuntimeException error) {
            throw new IllegalStateException("No se pudo cargar o indexar el corpus clínico", error);
        }
    }

    private java.util.stream.Stream<Document> leerDocumentos(String archivo) {
        try (var entrada = new ClassPathResource(archivo).getInputStream()) {
            ProtocoloCorpus protocolo = mapper.readValue(entrada, ProtocoloCorpus.class);
            validar(protocolo);
            // Se indexa título + sección + texto: solo el texto ("presión ≥ 180/120…") no se parece a
            // la pregunta ("crisis hipertensiva") y quedaba bajo el umbral (calibración 2026-11-07)
            return protocolo.secciones().stream().map(seccion -> new Document(
                    protocolo.titulo() + ". " + seccion.nombre() + ": " + seccion.texto(), Map.of(
                    MetadatosProtocolo.PROTOCOLO, protocolo.codigo(),
                    MetadatosProtocolo.TITULO, protocolo.titulo(),
                    MetadatosProtocolo.SECCION, seccion.nombre())));
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer el archivo del corpus: " + archivo, error);
        }
    }

    void validar(ProtocoloCorpus protocolo) {
        if (protocolo == null) throw esquemaInvalido();
        validarCodigo(protocolo);
        validarTitulo(protocolo);
        validarSecciones(protocolo);
    }

    private void validarCodigo(ProtocoloCorpus protocolo) {
        if (protocolo.codigo() == null || !SECCIONES_ESPERADAS.containsKey(protocolo.codigo())) {
            throw esquemaInvalido();
        }
    }

    private void validarTitulo(ProtocoloCorpus protocolo) {
        if (protocolo.titulo() == null || protocolo.titulo().isBlank()) {
            throw esquemaInvalido();
        }
    }

    private void validarSecciones(ProtocoloCorpus protocolo) {
        if (protocolo.secciones() == null || protocolo.secciones().size()
                != SECCIONES_ESPERADAS.get(protocolo.codigo())
                || protocolo.secciones().stream().anyMatch(this::seccionInvalida)) {
            throw esquemaInvalido();
        }
    }

    private boolean seccionInvalida(SeccionCorpus seccion) {
        return seccion == null || seccion.nombre() == null || seccion.nombre().isBlank()
                || seccion.texto() == null || seccion.texto().isBlank();
    }

    private IllegalStateException esquemaInvalido() {
        return new IllegalStateException("El archivo del corpus tiene un esquema o contenido inválido");
    }

    record ProtocoloCorpus(String codigo, String titulo, List<SeccionCorpus> secciones) {
    }

    record SeccionCorpus(String nombre, String texto) {
    }
}
