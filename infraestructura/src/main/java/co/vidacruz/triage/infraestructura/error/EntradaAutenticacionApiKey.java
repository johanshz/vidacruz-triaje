package co.vidacruz.triage.infraestructura.error;

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

    public EntradaAutenticacionApiKey(FabricaProblemDetail fabrica) {
        this.fabrica = fabrica;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        ProblemDetail problem = fabrica.crear(exchange, HttpStatus.UNAUTHORIZED, "NO_AUTORIZADO",
            "API key ausente o inválida", null);
        return fabrica.escribir(exchange, problem);
    }
}
