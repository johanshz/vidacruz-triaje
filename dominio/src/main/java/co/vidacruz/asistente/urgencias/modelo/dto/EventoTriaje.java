package co.vidacruz.asistente.urgencias.modelo.dto;

import java.time.Instant;

public record EventoTriaje(
        String idEvento,
        String tipo,
        Integer version,
        Instant ocurridoEn,
        DatosEventoTriaje datos) {
}
