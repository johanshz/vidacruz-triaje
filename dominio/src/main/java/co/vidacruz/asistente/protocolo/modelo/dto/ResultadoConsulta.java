package co.vidacruz.asistente.protocolo.modelo.dto;

import java.util.List;

public record ResultadoConsulta(RespuestaConsulta respuesta, String resultado,
        int longitudPregunta, List<String> protocolosCitados) {
    public ResultadoConsulta {
        protocolosCitados = List.copyOf(protocolosCitados);
    }
}
