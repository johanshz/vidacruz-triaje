package co.vidacruz.asistente.protocolo.servicio;

import co.vidacruz.asistente.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.asistente.protocolo.modelo.dto.FuenteProtocolo;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaGenerada;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta.Estado;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

public final class ServicioConsultarProtocolo {
    public static final String AVISO_SIN_COBERTURA = "No existe un protocolo disponible para esta pregunta";
    private static final int LONGITUD_MINIMA_PREGUNTA = 3;
    private static final int LONGITUD_MAXIMA_PREGUNTA = 500;
    private static final Pattern NUMERO = Pattern.compile("(?<![\\p{L}\\p{N}])\\d+(?:[.,]\\d+)?");
    private static final Pattern IDENTIFICADOR_PROTOCOLO = Pattern.compile("(?i)PROTO-\\d+");
    private static final Pattern NUMERO_LISTA = Pattern.compile("(?m)^\\s*\\d+[.)]\\s*");

    private final BuscadorProtocolos buscador;
    private final AsistenteProtocolos asistente;
    private final double umbral;
    private final int topK;

    public ServicioConsultarProtocolo(BuscadorProtocolos buscador, AsistenteProtocolos asistente,
            ParametrosBusqueda parametros) {
        this.buscador = buscador;
        this.asistente = asistente;
        this.umbral = parametros.umbral();
        this.topK = parametros.topK();
    }

    public Mono<ResultadoConsulta> consultar(String preguntaOriginal) {
        String pregunta = normalizar(preguntaOriginal);
        return buscador.buscar(pregunta, umbral, topK)
                .collectList()
                .flatMap(fragmentos -> responder(pregunta, fragmentos));
    }

    private Mono<ResultadoConsulta> responder(String pregunta, List<FragmentoProtocolo> fragmentos) {
        if (fragmentos.isEmpty()) {
            return Mono.just(resultadoFijo(pregunta, Estado.SIN_COBERTURA));
        }
        return asistente.responder(pregunta, fragmentos)
                .map(respuesta -> validarRespuesta(pregunta, fragmentos, respuesta));
    }

    private ResultadoConsulta validarRespuesta(String pregunta, List<FragmentoProtocolo> fragmentos,
            RespuestaGenerada generada) {
        Set<FuenteProtocolo> recuperadas = fuentesRecuperadas(fragmentos);
        if (generada.sinCobertura() || generada.respuesta().isBlank()
                || !fuentesRespaldadas(generada.fuentes(), recuperadas)) {
            return resultadoFijo(pregunta, Estado.BARRERA_RECHAZADA);
        }

        List<FuenteProtocolo> fuentes = generada.fuentes().stream()
                .map(fuente -> recuperadas.stream().filter(recuperada -> mismaFuente(fuente, recuperada))
                        .findFirst().orElseThrow())
                .toList();
        String contexto = fragmentos.stream().map(FragmentoProtocolo::texto).collect(Collectors.joining(" "));
        if (!cifrasRespaldadas(generada.respuesta(), contexto)) {
            return resultadoFijo(pregunta, Estado.BARRERA_RECHAZADA);
        }
        List<String> protocolos = fuentes.stream().map(FuenteProtocolo::protocolo).distinct().toList();
        return new ResultadoConsulta(new RespuestaConsulta(generada.respuesta(), fuentes),
                Estado.RESPONDIDA, pregunta.length(), protocolos);
    }

    private Set<FuenteProtocolo> fuentesRecuperadas(List<FragmentoProtocolo> fragmentos) {
        return fragmentos.stream().map(fragmento -> new FuenteProtocolo(fragmento.codigo(), fragmento.seccion()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean fuentesRespaldadas(List<FuenteProtocolo> fuentes, Set<FuenteProtocolo> recuperadas) {
        return !fuentes.isEmpty()
                && fuentes.stream().map(this::claveFuente).distinct().count() == fuentes.size()
                && fuentes.stream().allMatch(fuente -> recuperadas.stream()
                        .anyMatch(recuperada -> mismaFuente(fuente, recuperada)));
    }

    private boolean mismaFuente(FuenteProtocolo primera, FuenteProtocolo segunda) {
        return normalizarClave(primera.protocolo()).equals(normalizarClave(segunda.protocolo()))
                && normalizarClave(primera.seccion()).equals(normalizarClave(segunda.seccion()));
    }

    private String claveFuente(FuenteProtocolo fuente) {
        return normalizarClave(fuente.protocolo()) + "|" + normalizarClave(fuente.seccion());
    }

    private String normalizarClave(String valor) {
        return Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private ResultadoConsulta resultadoFijo(String pregunta, Estado resultado) {
        return new ResultadoConsulta(new RespuestaConsulta(AVISO_SIN_COBERTURA, List.of()),
                resultado, pregunta.length(), List.of());
    }

    private String normalizar(String pregunta) {
        if (pregunta == null) {
            throw new ExcepcionValidacion("pregunta", "La pregunta es obligatoria");
        }
        String recortada = pregunta.strip();
        if (recortada.length() < LONGITUD_MINIMA_PREGUNTA
                || recortada.length() > LONGITUD_MAXIMA_PREGUNTA) {
            throw new ExcepcionValidacion("pregunta", "La pregunta debe tener entre 3 y 500 caracteres");
        }
        return recortada;
    }

    private boolean cifrasRespaldadas(String respuesta, String contexto) {
        Set<BigDecimal> cifrasContexto = extraerCifras(contexto);
        return extraerCifras(respuesta).stream().allMatch(cifrasContexto::contains);
    }

    private Set<BigDecimal> extraerCifras(String texto) {
        String sinIdentificadores = IDENTIFICADOR_PROTOCOLO.matcher(texto).replaceAll(" ");
        String sinNumeracion = NUMERO_LISTA.matcher(sinIdentificadores).replaceAll(" ");
        var matcher = NUMERO.matcher(sinNumeracion);
        Set<BigDecimal> cifras = new HashSet<>();
        while (matcher.find()) {
            cifras.add(new BigDecimal(matcher.group().replace(',', '.')).stripTrailingZeros());
        }
        return cifras;
    }
}
