package co.vidacruz.triage;

import co.vidacruz.triage.comun.excepcion.ExcepcionConflicto;
import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class ControladorErroresPrueba {

    record SolicitudEdad(Integer edad) {
    }

    @GetMapping("/pruebas/error/validacion")
    Mono<Void> validacion() {
        return Mono.error(new ExcepcionValidacion("edad", "La edad no es válida"));
    }

    @GetMapping("/pruebas/error/no-encontrado")
    Mono<Void> noEncontrado() {
        return Mono.error(new ExcepcionRecursoNoEncontrado("Paciente no encontrado"));
    }

    @GetMapping("/pruebas/error/conflicto")
    Mono<Void> conflicto() {
        return Mono.error(new ExcepcionConflicto("Transición no permitida"));
    }

    @GetMapping("/pruebas/error/interno")
    Mono<Void> interno() {
        return Mono.error(new IllegalStateException("detalle interno confidencial"));
    }

    @PostMapping(path = "/pruebas/entrada", consumes = "application/json")
    Mono<SolicitudEdad> entrada(@RequestBody SolicitudEdad solicitud) {
        return Mono.just(solicitud);
    }
}
