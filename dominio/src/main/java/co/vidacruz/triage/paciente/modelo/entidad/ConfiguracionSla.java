package co.vidacruz.triage.paciente.modelo.entidad;

public record ConfiguracionSla(int critico, int urgente, int urgente2, int noUrgente, int noUrgente2) {
    public ConfiguracionSla {
        if (critico <= 0 || urgente <= 0 || urgente2 <= 0 || noUrgente <= 0 || noUrgente2 <= 0) {
            throw new IllegalArgumentException("Los SLA deben ser enteros mayores que cero");
        }
    }

    public int minutos(NivelTriage nivel) {
        return switch (nivel) {
            case CRITICO -> critico;
            case URGENTE -> urgente;
            case URGENTE_2 -> urgente2;
            case NO_URGENTE -> noUrgente;
            case NO_URGENTE_2 -> noUrgente2;
        };
    }
}
