package co.vidacruz.asistente.protocolo.modelo.entidad;

import java.util.Objects;

public record FragmentoProtocolo(String codigo, String titulo, String seccion, String texto) {
    public FragmentoProtocolo {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(seccion, "seccion");
        Objects.requireNonNull(texto, "texto");
    }
}
