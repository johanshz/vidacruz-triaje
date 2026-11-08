package co.vidacruz.triage.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class EscritorProblemDetail {
    private final ObjectMapper objectMapper;

    public EscritorProblemDetail(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public Mono<Void> escribir(ServerWebExchange exchange, ProblemDetail problemDetail) {
        try {
            byte[] cuerpo = objectMapper.writeValueAsBytes(problemDetail);
            exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(problemDetail.getStatus()));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(cuerpo)));
        } catch (JsonProcessingException error) {
            return Mono.error(error);
        }
    }
}
