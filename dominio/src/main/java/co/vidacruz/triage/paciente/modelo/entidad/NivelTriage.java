package co.vidacruz.triage.paciente.modelo.entidad;

public enum NivelTriage {
    CRITICO(1, 5), URGENTE(2, 10), URGENTE_2(3, 30), NO_URGENTE(4, 60), NO_URGENTE_2(5, 120);

    private final int nivel;
    private final int slaMinutos;

    NivelTriage(int nivel, int slaMinutos) {
        this.nivel = nivel;
        this.slaMinutos = slaMinutos;
    }

    public int numero() { return nivel; }
    public int slaMinutos() { return slaMinutos; }
}
