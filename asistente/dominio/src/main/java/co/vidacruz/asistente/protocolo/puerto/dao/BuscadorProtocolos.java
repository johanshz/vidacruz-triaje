package co.vidacruz.asistente.protocolo.puerto.dao;

import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import reactor.core.publisher.Flux;

public interface BuscadorProtocolos {
    Flux<FragmentoProtocolo> buscar(String pregunta, double umbral, int topK);
}
