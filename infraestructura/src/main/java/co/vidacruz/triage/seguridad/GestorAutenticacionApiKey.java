package co.vidacruz.triage.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public final class GestorAutenticacionApiKey implements ReactiveAuthenticationManager, ServerAuthenticationConverter {

    private static final String CABECERA = "X-API-Key";
    private static final String PRINCIPAL = "triage-api-key";
    private final byte[] claveEsperada;

    public GestorAutenticacionApiKey(String claveEsperada) {
        this.claveEsperada = claveEsperada.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(CABECERA))
            .map(clave -> (Authentication) new UsernamePasswordAuthenticationToken(PRINCIPAL, clave));
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        byte[] claveRecibida = String.valueOf(authentication.getCredentials()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(claveEsperada, claveRecibida)) {
            return Mono.error(new BadCredentialsException("API key ausente o inválida"));
        }
        return Mono.just(UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, "", List.of()));
    }
}
