package com.specforge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.repository.forge.Forge;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The read-only guarantee is a test, not a promise in a document. Two things have to stay true:
 * nothing outside the repository module can reach the forge at all, and the port itself never
 * grows an operation that writes content.
 */
class ForgePortTest {

    private static final List<String> WRITE_VERBS =
            List.of("create", "write", "commit", "push", "delete", "update", "comment", "merge", "open", "post", "put");

    @Test
    void isReachableOnlyFromTheRepositoryModule() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.specforge");

        noClasses()
                .that()
                .resideOutsideOfPackage("com.specforge.repository..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.specforge.repository.forge..")
                .check(classes);
    }

    @Test
    void exposesNoOperationThatWritesContent() {
        List<String> writeLike = Arrays.stream(Forge.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> !name.equals("reportReviewStatus"))
                .filter(name -> WRITE_VERBS.stream().anyMatch(verb -> name.toLowerCase().startsWith(verb)))
                .toList();

        // reportReviewStatus is the one permitted write, and it writes a commit status rather than
        // repository content. Anything else that reads like a write is a boundary violation.
        assertThat(writeLike).isEmpty();
    }
}
