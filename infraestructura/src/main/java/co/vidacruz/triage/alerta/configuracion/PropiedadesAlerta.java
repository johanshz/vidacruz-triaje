package co.vidacruz.triage.alerta.configuracion;

import java.util.UUID;

public record PropiedadesAlerta(String instancia) {
    public PropiedadesAlerta {
        if (instancia == null || instancia.isBlank()) {
            instancia = UUID.randomUUID().toString();
        }
    }
}
