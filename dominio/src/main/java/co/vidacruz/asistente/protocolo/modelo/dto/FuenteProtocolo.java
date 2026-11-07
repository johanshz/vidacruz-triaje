package co.vidacruz.asistente.protocolo.modelo.dto;

import java.util.Objects;

public record FuenteProtocolo(String protocolo, String seccion) {
    public FuenteProtocolo {
        Objects.requireNonNull(protocolo, "protocolo");
        Objects.requireNonNull(seccion, "seccion");
    }
}
