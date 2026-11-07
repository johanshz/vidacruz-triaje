package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.comando.ComandoRegistrarPaciente;
import co.vidacruz.triage.paciente.comando.DependenciasRegistroPaciente;
import co.vidacruz.triage.paciente.comando.FabricaPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorRegistrarPaciente;
import co.vidacruz.triage.paciente.comando.RegistrarCambioPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ManejadorRegistrarPacienteTest {
    private final RepositorioPaciente pacientes = mock(RepositorioPaciente.class);
    private final RepositorioAuditoria auditoria = mock(RepositorioAuditoria.class);
    private final RepositorioOutbox outbox = mock(RepositorioOutbox.class);
    private final RegistrarCambioPaciente registrarCambio = new RegistrarCambioPaciente(auditoria, outbox);
    private final AtomicInteger incrementos = new AtomicInteger();

    @Test void escribeLasTresPiezasYCuentaTrasElExito() {
        when(pacientes.guardar(any())).thenReturn(Mono.empty());
        when(auditoria.registrar(any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.empty());
        var manejador = manejador();
        StepVerifier.create(manejador.ejecutar(new ComandoRegistrarPaciente(datos())))
                .assertNext(paciente -> assertEquals(NivelTriage.NO_URGENTE, paciente.nivel())).verifyComplete();
        assertEquals(1, incrementos.get());
        InOrder orden = inOrder(pacientes, auditoria, outbox);
        orden.verify(pacientes).guardar(any(Paciente.class));
        orden.verify(auditoria).registrar(any(RegistroAuditoria.class));
        orden.verify(outbox).agregar(any(), anyString());
    }

    @Test void aplicaElSlaConfiguradoAUnaFichaNueva() {
        when(pacientes.guardar(any())).thenReturn(Mono.empty());
        when(auditoria.registrar(any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(manejador(new ConfiguracionSla(1, 10, 30, 60, 120))
                .ejecutar(new ComandoRegistrarPaciente(datosCriticos())))
                .assertNext(paciente -> assertEquals(1, paciente.slaMinutos())).verifyComplete();
    }

    @Test void rechazaSlaNoPositivoEnLaConfiguracionDelDominio() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionSla(0, 10, 30, 60, 120));
    }

    @Test void falloDeOutboxPropagaYNoIncrementaMetrica() {
        when(pacientes.guardar(any())).thenReturn(Mono.empty());
        when(auditoria.registrar(any())).thenReturn(Mono.empty());
        when(outbox.agregar(any(), anyString())).thenReturn(Mono.error(new IllegalStateException("fallo")));
        StepVerifier.create(manejador().ejecutar(new ComandoRegistrarPaciente(datos())))
                .expectError(IllegalStateException.class).verify();
        assertEquals(0, incrementos.get());
    }

    private ManejadorRegistrarPaciente manejador() {
        return manejador(new ConfiguracionSla(5, 10, 30, 60, 120));
    }

    private ManejadorRegistrarPaciente manejador(ConfiguracionSla configuracion) {
        EjecutorTransaccion transaccion = new EjecutorTransaccion() {
            public <T> Mono<T> ejecutar(java.util.function.Supplier<Mono<T>> trabajo) { return Mono.defer(trabajo); }
        };
        ContadorClasificaciones contador = nivel -> incrementos.incrementAndGet();
        var dependencias = new DependenciasRegistroPaciente(new ServicioClasificacionPaciente(), new FabricaPaciente(),
                pacientes, registrarCambio, transaccion, contador,
                Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC),
                configuracion);
        return new ManejadorRegistrarPaciente(dependencias);
    }

    private DatosRegistroPaciente datos() {
        return new DatosRegistroPaciente(" Ana ", 40, 70, 120, 80, 98, new BigDecimal("36.5"), 2, 15, " Dolor ");
    }

    private DatosRegistroPaciente datosCriticos() {
        return new DatosRegistroPaciente("Paciente crítico", 40, 130, 180, 110, 97,
                new BigDecimal("36.8"), 8, 15, "Dolor torácico");
    }
}
