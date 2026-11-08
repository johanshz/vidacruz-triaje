package co.vidacruz.asistente.urgencias.servicio;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import co.vidacruz.asistente.urgencias.puerto.repositorio.RepositorioUrgencias;
import java.util.Set;
import reactor.core.publisher.Mono;

public final class ServicioActualizarUrgencias {
    private static final Set<String> TIPOS_CONOCIDOS = Set.of(
            "paciente_registrado", "paciente_en_consulta", "paciente_alta", "paciente_escalado");
    private final RepositorioUrgencias repositorio;

    public ServicioActualizarUrgencias(RepositorioUrgencias repositorio) {
        this.repositorio = repositorio;
    }

    public Mono<Void> actualizar(String tipo, String idPaciente, String nivel) {
        return Mono.defer(() -> {
            if (tipo == null || tipo.isBlank()) return Mono.error(new ExcepcionEventoInvalido("Falta tipo"));
            if (!TIPOS_CONOCIDOS.contains(tipo)) return Mono.empty();
            if (idPaciente == null || idPaciente.isBlank()) return Mono.error(new ExcepcionEventoInvalido("Falta datos.idPaciente"));
            return switch (tipo) {
                case "paciente_registrado" -> registrar(idPaciente, nivel);
                case "paciente_alta" -> repositorio.quitarDeTodosLosNiveles(idPaciente);
                case "paciente_en_consulta", "paciente_escalado" -> Mono.empty();
                default -> Mono.empty();
            };
        });
    }

    private Mono<Void> registrar(String idPaciente, String nivel) {
        return NivelUrgencias.desde(nivel)
                .<Mono<Void>>map(nivelValido -> repositorio.agregar(nivelValido, idPaciente))
                .orElseGet(() -> Mono.error(new ExcepcionEventoInvalido("Nivel de urgencias inválido")));
    }
}
