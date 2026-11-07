package co.vidacruz.triage.evento.configuracion;

import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import co.vidacruz.triage.evento.puerto.repositorio.PublicadorEventos;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionPublicadorOutbox {
    @Bean ManejadorPublicarOutbox manejadorPublicarOutbox(RepositorioOutbox outbox, PublicadorEventos publicador,
            EjecutorTransaccion transaccion, ContadorEventosPendientes contador,
            @Value("${vidacruz.outbox.lote:100}") int lote) {
        return new ManejadorPublicarOutbox(outbox, publicador, transaccion, contador, lote);
    }
}
