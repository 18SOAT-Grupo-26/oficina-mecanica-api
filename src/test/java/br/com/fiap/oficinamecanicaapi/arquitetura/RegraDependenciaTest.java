package br.com.fiap.oficinamecanicaapi.arquitetura;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regra de dependência da ADR 001 ("Pode depender de" / "Não pode depender de"), conferida no {@code mvn verify}.
 * Os pacotes são ancorados na raiz do projeto: {@code core/domain/shared} não é o {@code shared} transversal, e
 * {@code io.micrometer.core} não é o {@code core}.
 */
@AnalyzeClasses(packages = RegraDependenciaTest.RAIZ, importOptions = ImportOption.DoNotIncludeTests.class)
class RegraDependenciaTest {

    static final String RAIZ = "br.com.fiap.oficinamecanicaapi";

    private static final String CONTROLLER = RAIZ + ".controller..";
    private static final String DOMAIN = RAIZ + ".core.domain..";
    private static final String PORT = RAIZ + ".core.port..";
    private static final String USECASE = RAIZ + ".core.usecase..";
    private static final String CORE = RAIZ + ".core..";
    private static final String INFRASTRUCTURE = RAIZ + ".infrastructure..";
    private static final String SHARED = RAIZ + ".shared..";

    @ArchTest
    static final ArchRule controllerSoConheceOPortIn = noClasses().that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAnyPackage(USECASE, INFRASTRUCTURE)
            .because("o controller chama o caso de uso pelo port in, sem conhecer a implementação nem a persistência");

    /**
     * {@link Perfil} implementa {@code GrantedAuthority}: exceção conhecida (dívida H4), registrada na ADR 001.
     */
    @ArchTest
    static final ArchRule dominioSemFrameworkNemCamadas = noClasses().that().resideInAPackage(DOMAIN)
            .and(not(equivalentTo(Perfil.class)))
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                    "org.hibernate..", PORT, USECASE, INFRASTRUCTURE, CONTROLLER)
            .because("o domínio é Java puro: depende só de shared/exception e dos utilitários puros de shared/utils");

    @ArchTest
    static final ArchRule portSemJpaNemImplementacao = noClasses().that().resideInAPackage(PORT)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", USECASE, INFRASTRUCTURE,
                    CONTROLLER)
            .because("o port é o contrato do core: domínio e DTOs, sem entidade JPA");

    @ArchTest
    static final ArchRule usecaseSemInfraestrutura = noClasses().that().resideInAPackage(USECASE)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", INFRASTRUCTURE, CONTROLLER)
            .because("o caso de uso fala com a persistência só pelo port out");

    @ArchTest
    static final ArchRule infraestruturaSemCasoDeUsoConcreto = noClasses().that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAnyPackage(USECASE, CONTROLLER)
            .because("o adapter implementa o port out e, quando precisa de um caso de uso, usa o port in");

    @ArchTest
    static final ArchRule sharedTransversal = noClasses().that().resideInAPackage(SHARED)
            .should().dependOnClassesThat().resideInAnyPackage(CORE, INFRASTRUCTURE, CONTROLLER)
            .because("shared é usado por todas as camadas e não pode depender de nenhuma delas");
}
