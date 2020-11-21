package no.ion.jake.junit4.container;

import no.ion.jake.BuildContext;
import no.ion.jake.junit4.JUnit4TestResults;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class Runner {
    public static JUnit4TestResults run(BuildContext buildContext, List<String> testClassNames) {
        Class<?>[] testClasses = testClassNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new NoClassDefFoundError(className + " not found");
                    }
                })
                .filter(Runner::isJUnit4TestClass)
                .toArray(Class[]::new);

        if (testClasses.length == 0) {
            return new JUnit4TestResults(true, TestRun.getSummaryForZeroTests());
        }

        var junitCore = new JUnitCore();
        TestRun testRun = new TestRun(buildContext);
        junitCore.addListener(testRun);
        // TODO:  Pass a computer with a proper thread pool
        Result result = junitCore.run(testClasses);
        testRun.setEndResult(result);

        return new JUnit4TestResults(testRun.wasSuccessful(), testRun.getSummary());
    }

    private static boolean isJUnit4TestClass(Class<?> clazz) {
        // Implementation based on maven-surefire-plugin, see JUnit48TestChecker.
        // JUnit 4 should have just ignored the classes we're returning false for...

        if (Modifier.isAbstract(clazz.getModifiers())) {

            RunWith runWith = clazz.getAnnotation(org.junit.runner.RunWith.class);
            if (runWith != null && runWith.value() == org.junit.experimental.runners.Enclosed.class) {
                return true;
            }

            return false;
        }

        // TODO: JUnit3 type test: If assignable from junit.framework.Test or if clazz has a 'public static T suite()' method,
        // with T being an instanceof junit.framework.Test.

        if (clazz.getAnnotation(org.junit.runner.RunWith.class) != null) {
            return true;
        }

        if (!hasAnyMethodsAnnotatedWith(clazz, org.junit.Test.class)) {
            // If such a class is passed on for JUnitCore testing, it will fail in a way almost indistinguishable from
            // declaring a test method initializationError() and throwing Exception("initializationError(" + clazz.class.getName() + ")").
            // The only way to distinguish this from JUnit 4 having found a class without tests is to verify the
            // stack trace does NOT include any frames from clazz.
            //
            // This may be handled in response to the failure of a "test", the test being the method initializationError()
            // of class clazz.  It would then be passed to testRunStarted(), testStarted(), testFailure(), and testFinished().
            // Or the class can be filtered away here.  We have chosen the latter based on maven-surefire-plugin.
            return false;
        }

        return true;
    }

    private static <T extends Annotation> boolean hasAnyMethodsAnnotatedWith(Class<?> clazz, Class<T> annotationClass) {
        // See org.junit.runners.model.TestClass.scanAnnotatedMembers() in junit:junit:4.12
        for (Class<?> superClass = clazz; superClass != Object.class && superClass != null; superClass = superClass.getSuperclass()) {
            for (Method method : superClass.getDeclaredMethods()) {
                T annotation = method.getAnnotation(annotationClass);
                if (annotation != null) {
                    return true;
                }
            }
        }

        return false;
    }
}
