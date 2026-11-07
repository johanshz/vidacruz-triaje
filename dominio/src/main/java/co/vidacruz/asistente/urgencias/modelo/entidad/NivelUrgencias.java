package co.vidacruz.asistente.urgencias.modelo.entidad;

import java.util.List;
import java.util.Optional;

public enum NivelUrgencias {
    CRITICO,
    URGENTE,
    URGENTE_2,
    NO_URGENTE,
    NO_URGENTE_2;

    public static List<NivelUrgencias> enOrden() {
        return List.of(values());
    }

    public static Optional<NivelUrgencias> desde(String valor) {
        if (valor == null) return Optional.empty();
        try {
            return Optional.of(valueOf(valor));
        } catch (IllegalArgumentException excepcion) {
            return Optional.empty();
        }
    }
}
