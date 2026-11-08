package co.vidacruz.asistente.protocolo.modelo.dto;

import java.util.List;

public record RespuestaConsulta(String respuesta, List<FuenteProtocolo> fuentes) {
    public RespuestaConsulta {
        fuentes = List.copyOf(fuentes);
    }
}
