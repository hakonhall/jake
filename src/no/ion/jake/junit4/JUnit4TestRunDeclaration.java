package no.ion.jake.junit4;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.java.ClassPathEntry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JUnit4TestRunDeclaration {
    private final List<ClassPathEntry> classPath = new ArrayList<>();
    private final String name;
    private Artifact<Path> testClassesArtifact = null;

    public JUnit4TestRunDeclaration(String name) {
        this.name = name;
    }

    public JUnit4TestRunDeclaration addClassPathEntries(List<ClassPathEntry> entries) {
        classPath.addAll(entries);
        return this;
    }

    public JUnit4TestRunDeclaration addClassPathEntry(ClassPathEntry entry) {
        classPath.add(entry);
        return this;
    }

    public JUnit4TestRunDeclaration addTestClassesArtifact(Artifact<Path> testClassesArtifact) {
        this.testClassesArtifact = testClassesArtifact;
        return this;
    }

    public Artifact<Void> declareTestRun(Declarator declarator) {
        if (testClassesArtifact == null) {
            throw new IllegalStateException("testClassesArtifact has not been set");
        }

        try (var buildDeclaration = declarator.declareNewBuild()) {
            classPath.forEach(entry -> entry.getArtifact().ifPresent(buildDeclaration::dependsOn));
            buildDeclaration.dependsOn(testClassesArtifact);
            Artifact<Void> testArtifact = buildDeclaration.producesArtifact(Void.class, "test");
            var testRun = new JUnit4TestRun(name, testClassesArtifact, classPath, testArtifact);
            buildDeclaration.forBuild(testRun);
            return testArtifact;
        }
    }
}
