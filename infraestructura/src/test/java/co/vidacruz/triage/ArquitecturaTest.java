package co.vidacruz.triage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {
    private static final JavaClasses DOMINIO = importar("dominio");
    private static final JavaClasses APLICACION = importar("aplicacion");
    private static final JavaClasses INFRAESTRUCTURA = importar("infraestructura");

    @Test void dominioNoDependeDeAplicacionNiInfraestructura() {
        noClasses().should().dependOnClassesThat().haveNameMatching(nombresDe(APLICACION)).check(DOMINIO);
        noClasses().should().dependOnClassesThat().haveNameMatching(nombresDe(INFRAESTRUCTURA)).check(DOMINIO);
    }
    @Test void dominioNoDependeDeSpring() { noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..").check(DOMINIO); }
    @Test void aplicacionNoDependeDeInfraestructura() {
        noClasses().should().dependOnClassesThat().haveNameMatching(nombresDe(INFRAESTRUCTURA)).check(APLICACION);
    }
    @Test void aplicacionNoDependeDeSpring() { noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..").check(APLICACION); }
    @Test void controladoresNoDependenDeAdaptadores() { noClasses().that().resideInAPackage("..controlador..").should().dependOnClassesThat().resideInAPackage("..adaptador..").allowEmptyShould(true).check(INFRAESTRUCTURA); }
    @Test void consultasNoDependenDeServiciosNiEntidades() { noClasses().that().resideInAPackage("..consulta..").should().dependOnClassesThat().resideInAnyPackage("..servicio..", "..modelo.entidad..").allowEmptyShould(true).check(APLICACION); }

    private static JavaClasses importar(String modulo) {
        Path directorio = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directorio != null && !Files.exists(directorio.resolve("settings.gradle"))) directorio = directorio.getParent();
        if (directorio == null) throw new IllegalStateException("No se encontró la raíz Gradle");
        return new ClassFileImporter().importPath(directorio.resolve(Path.of(modulo, "build", "classes", "java", "main")));
    }

    private static String nombresDe(JavaClasses clases) {
        return clases.stream().map(clase -> Pattern.quote(clase.getName())).collect(Collectors.joining("|", "(?:", ")"));
    }
}



