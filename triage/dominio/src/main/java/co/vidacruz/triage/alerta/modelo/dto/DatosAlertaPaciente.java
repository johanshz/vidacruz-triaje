package co.vidacruz.triage.alerta.modelo.dto;

import java.time.Instant;

public record DatosAlertaPaciente(String nombre, Instant fechaRegistro) { }
