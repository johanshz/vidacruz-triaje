package co.vidacruz.triage.seguridad;

import co.vidacruz.triage.comun.excepcion.CodigoError;
import co.vidacruz.triage.error.EscritorProblemDetail;
import co.vidacruz.triage.error.FabricaProblemDetail;
import co.vidacruz.triage.error.MensajesError;
import co.vidacruz.triage.error.TraductorErrorHttp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class EntradaAutenticacionApiKey implements ServerAuthenticationEntryPoint {
    private final FabricaProblemDetail fabrica;
    private final EscritorProblemDetail escritor;

    public EntradaAutenticacionApiKey(FabricaProblemDetail fabrica, EscritorProblemDetail escritor) {
        this.fabrica = fabrica;
        this.escritor = escritor;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        var error = TraductorErrorHttp.ErrorHttp.sinCampo(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTORIZADO,
            MensajesError.API_KEY_INVALIDA);
        ProblemDetail problem = fabrica.crear(exchange, error);
        return escritor.escribir(exchange, problem);
    }
}
