package co.vidacruz.triage.paciente.puerto.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;

public interface ContadorClasificaciones {
    void incrementar(NivelTriage nivel);
}
