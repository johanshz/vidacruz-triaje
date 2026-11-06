package co.vidacruz.triage;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
@AnalyzeClasses(packages = "co.vidacruz.triage", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {
    @ArchTest static final ArchRule dominio_no_depende_de_otras_capas = noClasses().that().resideInAPackage("..dominio..")
      .should().dependOnClassesThat().resideInAnyPackage("..aplicacion..", "..infraestructura..").allowEmptyShould(true);
    @ArchTest static final ArchRule aplicacion_no_depende_de_infraestructura = noClasses().that().resideInAPackage("..aplicacion..")
      .should().dependOnClassesThat().resideInAPackage("..infraestructura..").allowEmptyShould(true);
    @ArchTest static final ArchRule capas_internas_no_dependen_de_spring = noClasses().that().resideInAnyPackage("..dominio..", "..aplicacion..")
      .should().dependOnClassesThat().resideInAPackage("org.springframework..").allowEmptyShould(true);
    @ArchTest static final ArchRule controladores_no_depende_de_adaptadores = noClasses().that().resideInAPackage("..controlador..")
      .should().dependOnClassesThat().resideInAPackage("..adaptador..").allowEmptyShould(true);
    @ArchTest static final ArchRule consultas_no_usan_modelo_de_escritura = noClasses().that().resideInAPackage("..consulta..")
      .should().dependOnClassesThat().resideInAnyPackage("..servicio..", "..modelo.entidad..").allowEmptyShould(true);
}
