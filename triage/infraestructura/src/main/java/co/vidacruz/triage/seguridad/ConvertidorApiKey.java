package co.vidacruz.triage.seguridad;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class ConvertidorApiKey implements ServerAuthenticationConverter {
    private static final String CABECERA = "X-API-Key";
    private static final String PRINCIPAL = "triage-api-key";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(CABECERA))
            .map(clave -> (Authentication) new UsernamePasswordAuthenticationToken(PRINCIPAL, clave));
    }
}
