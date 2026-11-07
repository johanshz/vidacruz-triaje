package co.vidacruz.triage.seguridad;

import co.vidacruz.triage.infraestructura.error.EntradaAutenticacionApiKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;

@Configuration
@EnableConfigurationProperties(ConfiguracionSeguridad.class)
public class ConfiguracionSeguridadWeb {

    @Bean
    GestorAutenticacionApiKey gestorAutenticacionApiKey(ConfiguracionSeguridad configuracion) {
        return new GestorAutenticacionApiKey(configuracion.apiKey());
    }

    @Bean
    SecurityWebFilterChain filtroSeguridad(ServerHttpSecurity http,
                                             GestorAutenticacionApiKey gestor,
                                             EntradaAutenticacionApiKey entrada) {
        AuthenticationWebFilter filtroApiKey = new AuthenticationWebFilter(gestor);
        filtroApiKey.setServerAuthenticationConverter(gestor);
        filtroApiKey.setAuthenticationFailureHandler(new ServerAuthenticationEntryPointFailureHandler(entrada));

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authorizeExchange(reglas -> reglas.pathMatchers("/actuator/health/**").permitAll()
                .anyExchange().authenticated())
            .exceptionHandling(excepciones -> excepciones.authenticationEntryPoint(entrada))
            .addFilterAt(filtroApiKey, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
}
