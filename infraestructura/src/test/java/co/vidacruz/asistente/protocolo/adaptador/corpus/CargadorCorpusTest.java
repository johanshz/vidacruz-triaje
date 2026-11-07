package co.vidacruz.asistente.protocolo.adaptador.corpus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.DefaultApplicationArguments;
import org.mockito.Mockito;

class CargadorCorpusTest {
    @Test
    void indexaSieteFragmentosConMetadatosDeProtocoloYSeccion() throws Exception {
        VectorStore vectorStore = Mockito.mock(VectorStore.class);
        CargadorCorpus cargador = new CargadorCorpus(new ObjectMapper(), vectorStore);

        cargador.run(new DefaultApplicationArguments(new String[0]));

        verify(vectorStore).add(argThat(documentos -> {
            if (documentos.size() != 7) return false;
            List<String> codigos = documentos.stream()
                    .map(documento -> (String) documento.getMetadata().get("protocolo"))
                    .distinct().sorted().toList();
            boolean metadatosCompletos = documentos.stream().allMatch(documento ->
                    documento.getMetadata().get("seccion") instanceof String
                    && documento.getMetadata().get("titulo") instanceof String);
            assertEquals(List.of("PROTO-001", "PROTO-002", "PROTO-003"), codigos);
            return metadatosCompletos;
        }));
    }
}
