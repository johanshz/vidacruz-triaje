package co.vidacruz.asistente;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {
    @Test
    void importaLasClasesDeLosTresModulosGradleYVerificaSusLimites() {
        Path raiz = Path.of(System.getProperty("user.dir")).getParent();
        ClassFileImporter importador = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);
        JavaClasses dominio = importador.importPath(raiz.resolve("dominio/build/classes/java/main"));
        JavaClasses aplicacion = importador.importPath(raiz.resolve("aplicacion/build/classes/java/main"));
        JavaClasses infraestructura = importador.importPath(raiz.resolve("infraestructura/build/classes/java/main"));

        assertFalse(dominio.isEmpty());
        assertFalse(aplicacion.isEmpty());
        assertTrue(infraestructura.stream().anyMatch(clase -> clase.getName().endsWith("Aplicacion")));

        ArchRule dominioAislado = noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("..adaptador..", "..configuracion..", "..controlador..", "org.springframework..");
        ArchRule aplicacionAislada = noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("..adaptador..", "..configuracion..", "..controlador..", "org.springframework..");
        ArchRule controladoresAislados = noClasses().that().resideInAPackage("..controlador..")
                .should().dependOnClassesThat().resideInAPackage("..adaptador..");
        ArchRule consultasSinModeloEscritura = noClasses().that().resideInAPackage("..consulta..")
                .should().dependOnClassesThat().resideInAnyPackage("..servicio..", "..modelo.entidad..");

        dominioAislado.check(dominio);
        aplicacionAislada.check(aplicacion);
        controladoresAislados.check(infraestructura);
        consultasSinModeloEscritura.check(aplicacion);
        verificarDependenciasEntreModulos(dominio, aplicacion, infraestructura);
    }

    private void verificarDependenciasEntreModulos(JavaClasses dominio, JavaClasses aplicacion, JavaClasses infraestructura) {
        verificarQueNoDependeDe(dominio, aplicacion);
        verificarQueNoDependeDe(dominio, infraestructura);
        verificarQueNoDependeDe(aplicacion, infraestructura);
    }

    private void verificarQueNoDependeDe(JavaClasses origen, JavaClasses destino) {
        Set<String> nombresDestino = destino.stream().map(clase -> clase.getName()).collect(Collectors.toSet());
        origen.forEach(clase -> clase.getDirectDependenciesFromSelf().forEach(dependencia ->
                org.junit.jupiter.api.Assertions.assertFalse(nombresDestino.contains(dependencia.getTargetClass().getName()),
                        clase.getName() + " depende de un módulo externo: " + dependencia.getTargetClass().getName())));
    }
}
