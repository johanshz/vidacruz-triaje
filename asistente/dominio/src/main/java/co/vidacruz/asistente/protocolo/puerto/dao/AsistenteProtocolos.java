package co.vidacruz.asistente.protocolo.puerto.dao;

import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaGenerada;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import java.util.List;
import reactor.core.publisher.Mono;

public interface AsistenteProtocolos {
    Mono<RespuestaGenerada> responder(String pregunta, List<FragmentoProtocolo> fragmentos);
}
