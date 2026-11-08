package co.vidacruz.triage.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import co.vidacruz.triage.error.MensajesError;
import reactor.core.publisher.Mono;

public final class GestorAutenticacionApiKey implements ReactiveAuthenticationManager {

    private static final String PRINCIPAL = "triage-api-key";
    private final byte[] claveEsperada;

    public GestorAutenticacionApiKey(String claveEsperada) {
        this.claveEsperada = claveEsperada.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        byte[] claveRecibida = String.valueOf(authentication.getCredentials()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(claveEsperada, claveRecibida)) {
            return Mono.error(new BadCredentialsException(MensajesError.API_KEY_INVALIDA));
        }
        return Mono.just(UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, "", List.of()));
    }
}
