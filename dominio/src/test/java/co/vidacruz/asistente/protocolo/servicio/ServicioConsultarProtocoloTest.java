package co.vidacruz.asistente.protocolo.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

import co.vidacruz.asistente.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.asistente.protocolo.modelo.dto.FuenteProtocolo;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaGenerada;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ServicioConsultarProtocoloTest {
    @Mock private BuscadorProtocolos buscador;
    @Mock private AsistenteProtocolos asistente;

    private ServicioConsultarProtocolo servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioConsultarProtocolo(buscador, asistente, 0.75, 3);
    }

    @Test
    void sinCoincidenciaDevuelveAvisoSinLlamarAlChat() {
        when(buscador.buscar("¿hay diabetes?", 0.75, 3)).thenReturn(Flux.empty());

        StepVerifier.create(servicio.consultar(" ¿hay diabetes? "))
                .assertNext(resultado -> {
                    org.junit.jupiter.api.Assertions.assertEquals("sin_cobertura", resultado.resultado());
                    org.junit.jupiter.api.Assertions.assertEquals(ServicioConsultarProtocolo.AVISO_SIN_COBERTURA,
                            resultado.respuesta().respuesta());
                    org.junit.jupiter.api.Assertions.assertTrue(resultado.respuesta().fuentes().isEmpty());
                })
                .verifyComplete();

        verify(buscador).buscar("¿hay diabetes?", 0.75, 3);
        verifyNoInteractions(asistente);
    }

    @Test
    void aceptaFuenteRecuperadaYCifraDecimalEquivalente() {
        FragmentoProtocolo fragmento = fragmento("PROTO-001", "Medidas iniciales", "reposo tras 5.0 min");
        FuenteProtocolo fuente = new FuenteProtocolo("PROTO-001", "Medidas iniciales");
        when(buscador.buscar("¿qué medida?", 0.75, 3)).thenReturn(Flux.just(fragmento));
        when(asistente.responder("¿qué medida?", List.of(fragmento)))
                .thenReturn(Mono.just(new RespuestaGenerada("Reposar 5 min", List.of(fuente), false)));

        StepVerifier.create(servicio.consultar("¿qué medida?"))
                .assertNext(resultado -> {
                    org.junit.jupiter.api.Assertions.assertEquals("respondida", resultado.resultado());
                    org.junit.jupiter.api.Assertions.assertEquals(List.of(fuente), resultado.respuesta().fuentes());
                })
                .verifyComplete();
    }

    @Test
    void fuenteInventadaDevuelveAviso() {
        FragmentoProtocolo fragmento = fragmento("PROTO-001", "Medidas iniciales", "reposo tras 5 min");
        when(buscador.buscar("pregunta", 0.75, 3)).thenReturn(Flux.just(fragmento));
        when(asistente.responder("pregunta", List.of(fragmento))).thenReturn(Mono.just(new RespuestaGenerada(
                "Reposar 5 min", List.of(new FuenteProtocolo("PROTO-999", "Medidas iniciales")), false)));

        StepVerifier.create(servicio.consultar("pregunta"))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals(
                        "barrera_rechazada", resultado.resultado()))
                .verifyComplete();
    }

    @Test
    void cifraQueSoloEstaEnLaPreguntaSeRechaza() {
        FragmentoProtocolo fragmento = fragmento("PROTO-001", "Definición", "crisis hipertensiva");
        FuenteProtocolo fuente = new FuenteProtocolo("PROTO-001", "Definición");
        when(buscador.buscar("¿presión 200 mmHg?", 0.75, 3)).thenReturn(Flux.just(fragmento));
        when(asistente.responder("¿presión 200 mmHg?", List.of(fragmento)))
                .thenReturn(Mono.just(new RespuestaGenerada("La presión es 200 mmHg", List.of(fuente), false)));

        StepVerifier.create(servicio.consultar("¿presión 200 mmHg?"))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals(
                        "barrera_rechazada", resultado.resultado()))
                .verifyComplete();
    }

    @Test
    void reconoceRangosRazonesIdsYNumeracionDeListas() {
        FragmentoProtocolo fragmento = fragmento("PROTO-003", "Clasificación",
                "PROTO-003: 1. pérdida de 3-5 %; presión 180/120.");
        FuenteProtocolo fuente = new FuenteProtocolo("PROTO-003", "Clasificación");
        when(buscador.buscar("rangos", 0.75, 3)).thenReturn(Flux.just(fragmento));
        when(asistente.responder("rangos", List.of(fragmento))).thenReturn(Mono.just(new RespuestaGenerada(
                "3-5 % y 180/120", List.of(fuente), false)));

        StepVerifier.create(servicio.consultar("rangos"))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals("respondida", resultado.resultado()))
                .verifyComplete();
    }

    @Test
    void validaPreguntaNulaVaciaCortaYMayorDeQuinientos() {
        assertThrows(ExcepcionValidacion.class, () -> servicio.consultar(null));
        assertThrows(ExcepcionValidacion.class, () -> servicio.consultar("  "));
        assertThrows(ExcepcionValidacion.class, () -> servicio.consultar("ab"));
        assertThrows(ExcepcionValidacion.class, () -> servicio.consultar("x".repeat(501)));
        when(buscador.buscar(anyString(), eq(0.75), eq(3))).thenReturn(Flux.empty());

        StepVerifier.create(servicio.consultar("x".repeat(3)))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals("sin_cobertura", resultado.resultado()))
                .verifyComplete();
        StepVerifier.create(servicio.consultar("x".repeat(500) + " "))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals("sin_cobertura", resultado.resultado()))
                .verifyComplete();
    }

    @Test
    void respuestaSinCoberturaDelModeloDevuelveAviso() {
        FragmentoProtocolo fragmento = fragmento("PROTO-001", "Definición", "crisis hipertensiva");
        when(buscador.buscar("pregunta", 0.75, 3)).thenReturn(Flux.just(fragmento));
        when(asistente.responder("pregunta", List.of(fragmento)))
                .thenReturn(Mono.just(RespuestaGenerada.invalida()));

        StepVerifier.create(servicio.consultar("pregunta"))
                .assertNext(resultado -> org.junit.jupiter.api.Assertions.assertEquals(
                        "barrera_rechazada", resultado.resultado()))
                .verifyComplete();
    }

    private FragmentoProtocolo fragmento(String codigo, String seccion, String texto) {
        return new FragmentoProtocolo(codigo, "Protocolo de prueba", seccion, texto);
    }
}
