package co.vidacruz.asistente.protocolo.servicio;

import co.vidacruz.asistente.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.asistente.protocolo.modelo.dto.FuenteProtocolo;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaGenerada;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

public final class ServicioConsultarProtocolo {
    public static final String AVISO_SIN_COBERTURA = "No existe un protocolo disponible para esta pregunta";
    private static final Pattern NUMERO = Pattern.compile("(?<![\\p{L}\\p{N}])\\d+(?:[.,]\\d+)?(?![\\p{L}])");
    private static final Pattern IDENTIFICADOR_PROTOCOLO = Pattern.compile("(?i)PROTO-\\d+");
    private static final Pattern NUMERO_LISTA = Pattern.compile("(?m)^\\s*\\d+[.)]\\s*");

    private final BuscadorProtocolos buscador;
    private final AsistenteProtocolos asistente;
    private final double umbral;
    private final int topK;

    public ServicioConsultarProtocolo(BuscadorProtocolos buscador, AsistenteProtocolos asistente,
            double umbral, int topK) {
        this.buscador = buscador;
        this.asistente = asistente;
        this.umbral = umbral;
        this.topK = topK;
    }

    public Mono<ResultadoConsulta> consultar(String preguntaOriginal) {
        String pregunta = normalizar(preguntaOriginal);
        return buscador.buscar(pregunta, umbral, topK)
                .collectList()
                .flatMap(fragmentos -> responder(pregunta, fragmentos));
    }

    private Mono<ResultadoConsulta> responder(String pregunta, List<FragmentoProtocolo> fragmentos) {
        if (fragmentos.isEmpty()) {
            return Mono.just(resultadoFijo(pregunta, "sin_cobertura"));
        }
        return asistente.responder(pregunta, fragmentos)
                .map(respuesta -> validarRespuesta(pregunta, fragmentos, respuesta));
    }

    private ResultadoConsulta validarRespuesta(String pregunta, List<FragmentoProtocolo> fragmentos,
            RespuestaGenerada generada) {
        Set<FuenteProtocolo> recuperadas = fuentesRecuperadas(fragmentos);
        if (generada.sinCobertura() || generada.respuesta().isBlank()
                || !fuentesRespaldadas(generada.fuentes(), recuperadas)) {
            return resultadoFijo(pregunta, "barrera_rechazada");
        }

        List<FuenteProtocolo> fuentes = generada.fuentes().stream()
                .distinct()
                .map(fuente -> recuperadas.stream().filter(fuente::equals).findFirst().orElseThrow())
                .toList();
        String contexto = fragmentos.stream().map(FragmentoProtocolo::texto).collect(Collectors.joining(" "));
        if (!cifrasRespaldadas(generada.respuesta(), contexto)) {
            return resultadoFijo(pregunta, "barrera_rechazada");
        }
        List<String> protocolos = fuentes.stream().map(FuenteProtocolo::protocolo).distinct().toList();
        return new ResultadoConsulta(new RespuestaConsulta(generada.respuesta(), fuentes),
                "respondida", pregunta.length(), protocolos);
    }

    private Set<FuenteProtocolo> fuentesRecuperadas(List<FragmentoProtocolo> fragmentos) {
        return fragmentos.stream().map(fragmento -> new FuenteProtocolo(fragmento.codigo(), fragmento.seccion()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean fuentesRespaldadas(List<FuenteProtocolo> fuentes, Set<FuenteProtocolo> recuperadas) {
        return !fuentes.isEmpty() && recuperadas.containsAll(fuentes);
    }

    private ResultadoConsulta resultadoFijo(String pregunta, String resultado) {
        return new ResultadoConsulta(new RespuestaConsulta(AVISO_SIN_COBERTURA, List.of()),
                resultado, pregunta.length(), List.of());
    }

    private String normalizar(String pregunta) {
        if (pregunta == null) {
            throw new ExcepcionValidacion("pregunta", "La pregunta es obligatoria");
        }
        String recortada = pregunta.strip();
        if (recortada.length() < 3 || recortada.length() > 500) {
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
        var cifras = new java.util.HashSet<BigDecimal>();
        while (matcher.find()) {
            cifras.add(new BigDecimal(matcher.group().replace(',', '.')).stripTrailingZeros());
        }
        return Set.copyOf(cifras);
    }
}
