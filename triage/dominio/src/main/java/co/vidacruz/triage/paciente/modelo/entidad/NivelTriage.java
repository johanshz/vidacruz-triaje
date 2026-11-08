package co.vidacruz.triage.paciente.modelo.entidad;

public enum NivelTriage {
    CRITICO(1), URGENTE(2), URGENTE_2(3), NO_URGENTE(4), NO_URGENTE_2(5);

    private final int nivel;

    NivelTriage(int nivel) {
        this.nivel = nivel;
    }

    public int numero() { return nivel; }
}
