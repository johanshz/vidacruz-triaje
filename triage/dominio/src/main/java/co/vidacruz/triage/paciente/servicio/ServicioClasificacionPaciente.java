package co.vidacruz.triage.paciente.servicio;

import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public class ServicioClasificacionPaciente {
    private static final int FC_CRITICA_MINIMA = 40;
    private static final int FC_CRITICA_MAXIMA = 120;
    private static final int FC_URGENTE_MINIMA = 90;
    private static final int SPO2_CRITICA = 90;
    private static final int SPO2_URGENTE = 93;
    private static final int SISTOLICA_CRITICA = 90;
    private static final int SISTOLICA_URGENTE = 100;
    private static final int GCS_CRITICO = 8;
    private static final int GCS_URGENTE = 13;
    private static final int DOLOR_URGENTE_2 = 7;
    private static final int EDAD_MINIMA = 0;
    private static final int EDAD_MAXIMA = 120;
    private static final int FC_MINIMA = 20;
    private static final int FC_MAXIMA = 250;
    private static final int SISTOLICA_MINIMA = 60;
    private static final int SISTOLICA_MAXIMA = 260;
    private static final int DIASTOLICA_MINIMA = 30;
    private static final int DIASTOLICA_MAXIMA = 160;
    private static final int SPO2_MINIMA = 70;
    private static final int SPO2_MAXIMA = 100;
    private static final int DOLOR_MINIMO = 0;
    private static final int DOLOR_MAXIMO = 10;
    private static final int GCS_MINIMO = 3;
    private static final int GCS_MAXIMO = 15;
    private static final int NOMBRE_MINIMO = 3;
    private static final int NOMBRE_MAXIMO = 100;
    private static final int QUEJA_MINIMA = 1;
    private static final int QUEJA_MAXIMA = 500;
    private static final BigDecimal TEMPERATURA_URGENTE = new BigDecimal("38.5");
    private static final BigDecimal TEMPERATURA_URGENTE_2 = new BigDecimal("38.0");
    private static final BigDecimal TEMPERATURA_MINIMA = new BigDecimal("34.0");
    private static final BigDecimal TEMPERATURA_MAXIMA = new BigDecimal("42.0");
    private static final Set<String> QUEJAS_ADMINISTRATIVAS = Set.of(
            "documentos", "recoleccion de resultados", "renovacion de recetario", "vacunacion", "seguimiento");

    public NivelTriage clasificar(DatosRegistroPaciente datos) {
        if (tieneSignosCriticos(datos)) return NivelTriage.CRITICO;
        if (tieneSignosUrgentes(datos)) return NivelTriage.URGENTE;
        if (tieneSignosUrgentesSecundarios(datos)) return NivelTriage.URGENTE_2;
        if (esQuejaAdministrativa(datos.quejaPrincipal())) return NivelTriage.NO_URGENTE_2;
        return NivelTriage.NO_URGENTE;
    }

    private boolean tieneSignosCriticos(DatosRegistroPaciente datos) {
        boolean frecuenciaFueraDeUmbral = datos.frecuenciaCardiaca() < FC_CRITICA_MINIMA
                || datos.frecuenciaCardiaca() > FC_CRITICA_MAXIMA;
        return frecuenciaFueraDeUmbral || datos.spo2() < SPO2_CRITICA
                || datos.presionSistolica() < SISTOLICA_CRITICA || datos.gcs() <= GCS_CRITICO;
    }

    private boolean tieneSignosUrgentes(DatosRegistroPaciente datos) {
        boolean temperaturaUrgente = datos.temperatura().compareTo(TEMPERATURA_URGENTE) >= 0;
        return datos.spo2() <= SPO2_URGENTE || datos.presionSistolica() <= SISTOLICA_URGENTE
                || temperaturaUrgente || datos.gcs() <= GCS_URGENTE;
    }

    private boolean tieneSignosUrgentesSecundarios(DatosRegistroPaciente datos) {
        boolean temperaturaElevada = datos.temperatura().compareTo(TEMPERATURA_URGENTE_2) >= 0;
        return datos.frecuenciaCardiaca() >= FC_URGENTE_MINIMA
                || temperaturaElevada || datos.dolor() >= DOLOR_URGENTE_2;
    }

    public void validar(DatosRegistroPaciente datos) {
        if (datos == null) throw new ExcepcionValidacion("cuerpo", "El cuerpo es obligatorio");
        texto(datos.nombreCompleto(), "nombreCompleto", NOMBRE_MINIMO, NOMBRE_MAXIMO);
        rango(datos.edad(), EDAD_MINIMA, EDAD_MAXIMA, "edad");
        rango(datos.frecuenciaCardiaca(), FC_MINIMA, FC_MAXIMA, "frecuenciaCardiaca");
        rango(datos.presionSistolica(), SISTOLICA_MINIMA, SISTOLICA_MAXIMA, "presionSistolica");
        rango(datos.presionDiastolica(), DIASTOLICA_MINIMA, DIASTOLICA_MAXIMA, "presionDiastolica");
        if (datos.presionDiastolica() >= datos.presionSistolica()) {
            throw new ExcepcionValidacion("presionDiastolica", "Debe ser menor que la presión sistólica");
        }
        rango(datos.spo2(), SPO2_MINIMA, SPO2_MAXIMA, "spo2");
        BigDecimal temperatura = datos.temperatura();
        if (temperatura == null || temperatura.compareTo(TEMPERATURA_MINIMA) < 0
                || temperatura.compareTo(TEMPERATURA_MAXIMA) > 0 || temperatura.scale() > 1) {
            throw new ExcepcionValidacion("temperatura", "Temperatura fuera del rango permitido");
        }
        rango(datos.dolor(), DOLOR_MINIMO, DOLOR_MAXIMO, "dolor");
        rango(datos.gcs(), GCS_MINIMO, GCS_MAXIMO, "gcs");
        texto(datos.quejaPrincipal(), "quejaPrincipal", QUEJA_MINIMA, QUEJA_MAXIMA);
    }

    private void rango(int valor, int minimo, int maximo, String campo) {
        if (valor < minimo || valor > maximo) throw new ExcepcionValidacion(campo, "Valor fuera del rango permitido");
    }
    private void texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.trim().length() < minimo || valor.trim().length() > maximo) {
            throw new ExcepcionValidacion(campo, "Longitud fuera del rango permitido");
        }
    }
    private boolean esQuejaAdministrativa(String queja) {
        String normalizada = Normalizer.normalize(queja.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
        return QUEJAS_ADMINISTRATIVAS.contains(normalizada);
    }
}
