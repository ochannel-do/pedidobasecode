package com.exemplo.loja.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.*;

import com.tngtech.archunit.core.importer.*;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.exemplo.loja");

  @Test
  void corePuro() {
    classes()
        .that()
        .resideInAPackage("..core..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("java..", "com.exemplo.loja..core..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..core..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta..",
            "io.micrometer..",
            "io.opentelemetry..",
            "..infrastructure..")
        .check(classes);
  }

  @Test
  void dominioIndependente() {
    noClasses()
        .that()
        .resideInAPackage("..core.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..core.application..")
        .check(classes);
  }

  @Test
  void fronteiras() {
    noClasses()
        .that()
        .resideInAPackage("..pedidos..")
        .and()
        .resideOutsideOfPackage("..pedidos.infrastructure.integration..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..estoque..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..pedidos.infrastructure.integration..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..estoque.core.domain..",
            "..estoque.core.application.usecase..",
            "..estoque.core.application.port.out..",
            "..estoque.infrastructure..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage("..estoque..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..pedidos..")
        .check(classes);
  }

  @Test
  void semCiclos() {
    slices().matching("com.exemplo.loja.(*)..").should().beFreeOfCycles().check(classes);
  }

  @Test
  void estoqueSemHttp() {
    noClasses()
        .that()
        .resideInAPackage("..estoque..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.web..")
        .check(classes);
  }
}
