package co.vidacruz.triage.alerta.configuracion;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vidacruz")
public class PropiedadesAlerta {
    private String instancia;

    public String instancia() {
        if (instancia == null || instancia.isBlank()) {
            instancia = UUID.randomUUID().toString();
        }
        return instancia;
    }

    public void setInstancia(String instancia) {
        this.instancia = instancia;
    }
}
