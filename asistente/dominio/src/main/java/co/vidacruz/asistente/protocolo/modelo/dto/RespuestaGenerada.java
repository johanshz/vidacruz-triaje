package co.vidacruz.asistente.protocolo.modelo.dto;

import java.util.List;

public record RespuestaGenerada(String respuesta, List<FuenteProtocolo> fuentes,
        boolean sinCobertura) {
    public RespuestaGenerada {
        respuesta = respuesta == null ? "" : respuesta;
        fuentes = List.copyOf(fuentes);
    }

    public static RespuestaGenerada invalida() {
        return new RespuestaGenerada("", List.of(), true);
    }
}
