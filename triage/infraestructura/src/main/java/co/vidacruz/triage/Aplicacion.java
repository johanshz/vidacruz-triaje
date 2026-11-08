package co.vidacruz.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import co.vidacruz.triage.paciente.configuracion.PropiedadesSla;
import co.vidacruz.triage.reporte.configuracion.PropiedadesReporte;
import co.vidacruz.triage.vigilancia.configuracion.PropiedadesVigilancia;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({PropiedadesSla.class, PropiedadesVigilancia.class,
        PropiedadesReporte.class})
public class Aplicacion {
    public static void main(String[] args) {
        SpringApplication.run(Aplicacion.class, args);
    }
}
