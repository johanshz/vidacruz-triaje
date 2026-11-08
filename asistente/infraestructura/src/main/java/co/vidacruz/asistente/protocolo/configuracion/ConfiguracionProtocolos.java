package co.vidacruz.asistente.protocolo.configuracion;

import co.vidacruz.asistente.configuracion.PropiedadesIa;
import co.vidacruz.asistente.protocolo.adaptador.corpus.CargadorCorpus;
import co.vidacruz.asistente.protocolo.adaptador.repositorio.BuscadorProtocolosVectorStore;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableConfigurationProperties(PropiedadesIa.class)
@ConditionalOnProperty(name = CondicionesProtocolo.PROPIEDAD_CARGAR_CORPUS, havingValue = "true", matchIfMissing = true)
public class ConfiguracionProtocolos {
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    BuscadorProtocolos buscadorProtocolos(VectorStore vectorStore, PropiedadesIa propiedades) {
        return new BuscadorProtocolosVectorStore(vectorStore, propiedades);
    }

    @Bean
    CargadorCorpus cargadorCorpus(ObjectMapper mapper, VectorStore vectorStore) {
        return new CargadorCorpus(mapper, vectorStore);
    }
}
