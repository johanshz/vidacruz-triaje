package co.vidacruz.triage.paciente.controlador;

import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import java.math.BigDecimal;

public record SolicitudRegistroPaciente(String nombreCompleto, Integer edad, Integer frecuenciaCardiaca,
        Integer presionSistolica, Integer presionDiastolica, Integer spo2, BigDecimal temperatura,
        Integer dolor, Integer gcs, String quejaPrincipal) {
    public DatosRegistroPaciente aDatos() {
        requerido(nombreCompleto, "nombreCompleto"); requerido(edad, "edad"); requerido(frecuenciaCardiaca, "frecuenciaCardiaca");
        requerido(presionSistolica, "presionSistolica"); requerido(presionDiastolica, "presionDiastolica");
        requerido(spo2, "spo2"); requerido(temperatura, "temperatura"); requerido(dolor, "dolor");
        requerido(gcs, "gcs"); requerido(quejaPrincipal, "quejaPrincipal");
        return new DatosRegistroPaciente(nombreCompleto, edad, frecuenciaCardiaca, presionSistolica,
                presionDiastolica, spo2, temperatura, dolor, gcs, quejaPrincipal);
    }
    private void requerido(Object valor, String campo) {
        if (valor == null) throw new ExcepcionValidacion(campo, "El campo es obligatorio");
    }
}
