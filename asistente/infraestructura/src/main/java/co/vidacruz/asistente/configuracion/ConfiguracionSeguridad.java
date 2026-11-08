package co.vidacruz.asistente.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class ConfiguracionSeguridad {
    @Bean
    SecurityWebFilterChain filtroSeguridad(ServerHttpSecurity http,
            PropiedadesSeguridad propiedades,
            ObjectMapper mapper) {
        ServerAuthenticationConverter conversor = intercambio -> Mono.justOrEmpty(
                intercambio.getRequest().getHeaders().getFirst("X-API-Key"))
                .<Authentication>map(clave -> new UsernamePasswordAuthenticationToken("api-key", clave));
        ServerAuthenticationEntryPoint entryPoint = (intercambio, excepcion) -> {
            var respuesta = intercambio.getResponse();
            respuesta.setStatusCode(HttpStatus.UNAUTHORIZED);
            respuesta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            String traceId = intercambio.getResponse().getHeaders().getFirst("X-Trace-Id");
            if (traceId == null) traceId = "desconocido";
            respuesta.getHeaders().set("X-Trace-Id", traceId);
            byte[] cuerpo;
            try {
                cuerpo = mapper.writeValueAsBytes(Map.of(
                        "type", "about:blank", "title", "Unauthorized", "status", 401,
                        "detail", "API key ausente o inválida", "codigo", "NO_AUTORIZADO",
                        "traceId", traceId));
            } catch (Exception error) {
                return Mono.error(error);
            }
            return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
        };
        org.springframework.security.authentication.ReactiveAuthenticationManager autenticador = autenticacion ->
                Mono.<Authentication>defer(() -> {
                    String recibida = String.valueOf(autenticacion.getCredentials());
                    boolean valida = MessageDigest.isEqual(
                            propiedades.apiKey().getBytes(StandardCharsets.UTF_8),
                            recibida.getBytes(StandardCharsets.UTF_8));
                    return valida ? Mono.just(new UsernamePasswordAuthenticationToken("api-key", "", java.util.List.of()))
                            : Mono.error(new BadCredentialsException("API key ausente o inválida"));
                });
        var filtroAutenticacion = new org.springframework.security.web.server.authentication.AuthenticationWebFilter(autenticador);
        filtroAutenticacion.setServerAuthenticationConverter(conversor);
        filtroAutenticacion.setAuthenticationFailureHandler((intercambio, excepcion) -> entryPoint.commence(
                intercambio.getExchange(), excepcion));

        return http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(errores -> errores.authenticationEntryPoint(entryPoint))
                .authorizeExchange(rutas -> rutas.pathMatchers("/actuator/health/**").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(filtroAutenticacion,
                        org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
