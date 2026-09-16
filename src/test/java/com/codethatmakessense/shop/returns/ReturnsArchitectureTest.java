package com.codethatmakessense.shop.returns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.codethatmakessense.shop.returns", importOptions = ImportOption.DoNotIncludeTests.class)
class ReturnsArchitectureTest {

    @ArchTest
    static final ArchRule domainAndApplicationAreFrameworkFree = noClasses()
            .that().resideInAnyPackage("..returns.domain..", "..returns.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "..adapter..")
            .because("the domain is nobody's hostage");

    @ArchTest
    static final ArchRule domainDoesNotKnowTheApplication = noClasses()
            .that().resideInAPackage("..returns.domain..")
            .should().dependOnClassesThat().resideInAPackage("..returns.application..")
            .because("dependencies point inward");
}
