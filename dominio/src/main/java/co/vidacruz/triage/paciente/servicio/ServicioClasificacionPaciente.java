package co.vidacruz.triage.paciente.servicio;

import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public class ServicioClasificacionPaciente {
    private static final Set<String> QUEJAS_ADMINISTRATIVAS = Set.of("documentos", "recoleccion de resultados", "renovacion de recetario", "vacunacion", "seguimiento");

    public NivelTriage clasificar(DatosRegistroPaciente datos) {
        validar(datos);
        if (datos.frecuenciaCardiaca() < 40 || datos.frecuenciaCardiaca() > 120
                || datos.spo2() < 90 || datos.presionSistolica() < 90 || datos.gcs() <= 8) return NivelTriage.CRITICO;
        if (datos.spo2() <= 93 || datos.presionSistolica() <= 100
                || datos.temperatura().compareTo(new BigDecimal("38.5")) >= 0
                || datos.gcs() <= 13) return NivelTriage.URGENTE;
        if (datos.frecuenciaCardiaca() >= 90 || datos.temperatura().compareTo(new BigDecimal("38.0")) >= 0
                || datos.dolor() >= 7) return NivelTriage.URGENTE_2;
        if (esQuejaAdministrativa(datos.quejaPrincipal())) return NivelTriage.NO_URGENTE_2;
        return NivelTriage.NO_URGENTE;
    }

    private void validar(DatosRegistroPaciente datos) {
        texto(datos.nombreCompleto(), "nombreCompleto", 3, 100);
        rango(datos.edad(), 0, 120, "edad"); rango(datos.frecuenciaCardiaca(), 20, 250, "frecuenciaCardiaca");
        rango(datos.presionSistolica(), 60, 260, "presionSistolica"); rango(datos.presionDiastolica(), 30, 160, "presionDiastolica");
        if (datos.presionDiastolica() >= datos.presionSistolica()) throw new ExcepcionValidacion("presionDiastolica", "Debe ser menor que la presión sistólica");
        rango(datos.spo2(), 70, 100, "spo2");
        BigDecimal temperatura = datos.temperatura();
        if (temperatura == null || temperatura.compareTo(new BigDecimal("34.0")) < 0 || temperatura.compareTo(new BigDecimal("42.0")) > 0 || temperatura.scale() > 1) throw new ExcepcionValidacion("temperatura", "Temperatura fuera del rango permitido");
        rango(datos.dolor(), 0, 10, "dolor"); rango(datos.gcs(), 3, 15, "gcs");
        texto(datos.quejaPrincipal(), "quejaPrincipal", 1, 500);
    }

    private void rango(int valor, int minimo, int maximo, String campo) {
        if (valor < minimo || valor > maximo) throw new ExcepcionValidacion(campo, "Valor fuera del rango permitido");
    }
    private void texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.trim().length() < minimo || valor.trim().length() > maximo) throw new ExcepcionValidacion(campo, "Longitud fuera del rango permitido");
    }
    private boolean esQuejaAdministrativa(String queja) {
        String normalizada = Normalizer.normalize(queja.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
        return QUEJAS_ADMINISTRATIVAS.contains(normalizada);
    }
}
