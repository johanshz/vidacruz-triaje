package co.vidacruz.triage.paciente.modelo.entidad;

import java.math.BigDecimal;

public record DatosRegistroPaciente(String nombreCompleto, int edad, int frecuenciaCardiaca,
        int presionSistolica, int presionDiastolica, int spo2, BigDecimal temperatura,
        int dolor, int gcs, String quejaPrincipal) { }
