package co.vidacruz.asistente.protocolo.modelo.dto;

import java.util.List;

public record ResultadoConsulta(RespuestaConsulta respuesta, Estado resultado,
        int longitudPregunta, List<String> protocolosCitados) {
    public ResultadoConsulta {
        protocolosCitados = List.copyOf(protocolosCitados);
    }

    public enum Estado {
        RESPONDIDA("respondida"), SIN_COBERTURA("sin_cobertura"),
        BARRERA_RECHAZADA("barrera_rechazada"), NO_DISPONIBLE("no_disponible");

        private final String etiqueta;

        Estado(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }
}
