package co.vidacruz.triage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.vigilancia.comando.ComandoEscalarVencidos;
import co.vidacruz.triage.vigilancia.comando.manejador.ManejadorEscalarVencidos;
import co.vidacruz.triage.vigilancia.tarea.TareaVigilanciaSla;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class TareaVigilanciaSlaTest {
    @Test
    void noIniciaUnSegundoCicloMientrasElPrimeroSigueActivo() {
        var manejador = mock(ManejadorEscalarVencidos.class);
        var ciclo = Sinks.<Void>empty();
        when(manejador.ejecutar(any())).thenReturn(ciclo.asMono());
        var tarea = new TareaVigilanciaSla(manejador);

        StepVerifier.create(Mono.when(tarea.ejecutar(), tarea.ejecutar()))
                .then(() -> verify(manejador, times(1)).ejecutar(any(ComandoEscalarVencidos.class)))
                .then(ciclo::tryEmitEmpty)
                .verifyComplete();
    }
}
