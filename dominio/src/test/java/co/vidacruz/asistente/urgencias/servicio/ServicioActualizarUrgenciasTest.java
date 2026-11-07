package co.vidacruz.asistente.urgencias.servicio;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import co.vidacruz.asistente.urgencias.puerto.repositorio.RepositorioUrgencias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ServicioActualizarUrgenciasTest {
    @Mock private RepositorioUrgencias repositorio;
    private ServicioActualizarUrgencias servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioActualizarUrgencias(repositorio);
    }

    @Test
    void registraPacienteEnNivel() {
        when(repositorio.agregar(NivelUrgencias.URGENTE, "p-1")).thenReturn(Mono.empty());
        StepVerifier.create(servicio.actualizar("paciente_registrado", "p-1", "URGENTE")).verifyComplete();
        verify(repositorio).agregar(eq(NivelUrgencias.URGENTE), eq("p-1"));
    }

    @Test
    void daDeAltaEnTodosLosNivelesSinExigirNivel() {
        when(repositorio.quitarDeTodosLosNiveles("p-1")).thenReturn(Mono.empty());
        StepVerifier.create(servicio.actualizar("paciente_alta", "p-1", null)).verifyComplete();
        verify(repositorio).quitarDeTodosLosNiveles("p-1");
    }

    @Test
    void conservaLaVistaParaConsultaYEscalado() {
        StepVerifier.create(servicio.actualizar("paciente_en_consulta", "p-1", null)).verifyComplete();
        StepVerifier.create(servicio.actualizar("paciente_escalado", "p-1", "CRITICO")).verifyComplete();
        verifyNoInteractions(repositorio);
    }

    @Test
    void rechazaTipoPacienteONivelInvalidos() {
        StepVerifier.create(servicio.actualizar(null, "p-1", null)).expectError(ExcepcionEventoInvalido.class).verify();
        StepVerifier.create(servicio.actualizar("paciente_registrado", " ", "CRITICO")).expectError(ExcepcionEventoInvalido.class).verify();
        StepVerifier.create(servicio.actualizar("paciente_registrado", "p-1", null)).expectError(ExcepcionEventoInvalido.class).verify();
        StepVerifier.create(servicio.actualizar("paciente_registrado", "p-1", "OTRO")).expectError(ExcepcionEventoInvalido.class).verify();
        verifyNoInteractions(repositorio);
    }

    @Test
    void ignoraTipoDesconocidoYRegistroRepetidoEsIdempotentePorConjunto() {
        when(repositorio.agregar(NivelUrgencias.CRITICO, "p-1")).thenReturn(Mono.empty());
        StepVerifier.create(servicio.actualizar("tipo_nuevo", "p-1", null)).verifyComplete();
        StepVerifier.create(servicio.actualizar("paciente_registrado", "p-1", "CRITICO")).verifyComplete();
        StepVerifier.create(servicio.actualizar("paciente_registrado", "p-1", "CRITICO")).verifyComplete();
        verify(repositorio, org.mockito.Mockito.times(2)).agregar(NivelUrgencias.CRITICO, "p-1");
    }
}
