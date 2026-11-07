package co.vidacruz.asistente.protocolo.comando;

public final class FabricaConsultaProtocolo {
    public ComandoConsultarProtocolo crear(String pregunta) {
        return new ComandoConsultarProtocolo(pregunta);
    }
}
