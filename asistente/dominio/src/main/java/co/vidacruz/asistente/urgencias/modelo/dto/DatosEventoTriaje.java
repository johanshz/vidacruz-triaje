package co.vidacruz.asistente.urgencias.modelo.dto;

public record DatosEventoTriaje(
        String idPaciente,
        String nivel,
        String estadoAnterior,
        String estadoNuevo,
        Boolean escalado) {
}
