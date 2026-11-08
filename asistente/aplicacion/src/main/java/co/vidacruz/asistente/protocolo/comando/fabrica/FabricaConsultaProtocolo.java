package co.vidacruz.asistente.protocolo.comando.fabrica;

import co.vidacruz.asistente.protocolo.comando.ComandoConsultarProtocolo;

public final class FabricaConsultaProtocolo {
    public ComandoConsultarProtocolo crear(String pregunta) {
        return new ComandoConsultarProtocolo(pregunta);
    }
}
