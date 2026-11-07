package co.vidacruz.triage;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class BlockHoundIntegracionTest {

    @org.junit.jupiter.api.BeforeAll
    static void instalarBlockHound() { PruebaIntegracionPostgres.instalarBlockHoundParaIntegracion(); }

    @Test
    void detectaBloqueoEnHiloReactivo() {
        StepVerifier.create(Mono.delay(Duration.ofMillis(1))
                .publishOn(Schedulers.parallel())
                .map(tick -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                    return tick;
                }))
            .expectError(BlockingOperationError.class)
            .verify();
    }
}
