package co.vidacruz.triage.evento.modelo.entidad;

public enum TipoEventoPaciente {
    PACIENTE_REGISTRADO("paciente_registrado"),
    PACIENTE_EN_CONSULTA("paciente_en_consulta"),
    PACIENTE_ALTA("paciente_alta"),
    PACIENTE_ESCALADO("paciente_escalado");

    private final String valor;

    TipoEventoPaciente(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
