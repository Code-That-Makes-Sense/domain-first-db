package com.codethatmakessense.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.codethatmakessense.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainAndApplicationAreFrameworkFree = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..", "..shared..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "..adapter..")
            .because("the domain is nobody's hostage (jakarta.transaction is a standard, not a framework)");

    @ArchTest
    static final ArchRule domainDoesNotKnowTheApplication = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .because("dependencies point inward");

    @ArchTest
    static final ArchRule legacyStockIsReachedThroughThePort = noClasses()
            .that().resideInAnyPackage("..order.domain..", "..order.application..", "..returns..")
            .should().dependOnClassesThat().resideInAPackage("..stock..")
            .because("stock was never migrated; the only way in is StockReservations, adapted in order.adapter.legacy");

    @ArchTest
    static final ArchRule modulesDoNotShareDomains = noClasses()
            .that().resideInAPackage("..returns.domain..")
            .should().dependOnClassesThat().resideInAPackage("..order.domain..")
            .because("the return side asks the order side one question, through ShippedItems, and shares only the kernel");
}
