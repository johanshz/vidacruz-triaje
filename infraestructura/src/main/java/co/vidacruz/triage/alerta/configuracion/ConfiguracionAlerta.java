package co.vidacruz.triage.alerta.configuracion;

import co.vidacruz.triage.alerta.comando.ManejadorEmitirAlerta;
import co.vidacruz.triage.alerta.puerto.dao.DaoAlerta;
import co.vidacruz.triage.alerta.puerto.repositorio.NotificadorAlerta;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ConfiguracionAlerta {
    @Bean
    PropiedadesAlerta propiedadesAlerta(@Value("${vidacruz.instancia:}") String instancia) {
        return new PropiedadesAlerta(instancia);
    }

    @Bean
    ManejadorEmitirAlerta manejadorEmitirAlerta(DaoAlerta dao, NotificadorAlerta notificador, Clock reloj) {
        return new ManejadorEmitirAlerta(dao, notificador, reloj);
    }
}
