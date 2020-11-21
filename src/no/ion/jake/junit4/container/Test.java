package no.ion.jake.junit4.container;

import no.ion.jake.JakeException;
import no.ion.jake.util.Stopwatch;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^([_a-zA-Z][_a-zA-Z0-9]*)\\(([_a-zA-Z][_a-zA-Z0-9.]*)\\)$");

    private final String displayName;
    private final String testClass;
    private final String testMethod;
    private final Stopwatch.Running runningStopwatch;

    private final Object monitor = new Object();
    private boolean finished = false;
    private Duration duration = null;
    private Failure failure = null;
    private boolean assumptionFailure = false;

    static Test fromTestStartedDescription(Description description) {
        String displayName = description.getDisplayName();

        // Display name of format "testPartialOrderTesterFailsOnIncorrectOrdering(com.yahoo.test.OrderTesterTest)"
        Matcher matcher = NAME_PATTERN.matcher(displayName);
        if (!matcher.matches()) {
            // TODO: Parametrized tests get a Description that contains no trace of the class and method names.
            // And more generally, we probably want to accept a full hierarchy at test run start, and form
            // a path to identify a test.  The next-to-top-most elements appears to be class names.
            // And a parameterized test is a lonely child of a test Description with the same attributes,
            // that is a child of the next-to-top-most classes.
            throw new JakeException("Format of display name of JUnit 4 test not recognized: " + displayName);
        }

        String testMethod = matcher.group(1);
        String testClass = matcher.group(2);
        return new Test(displayName, testClass, testMethod, Stopwatch.start());
    }

    static String idFromDescription(Description description) {
        return description.getDisplayName();
    }

    private Test(String displayName, String testClass, String testMethod, Stopwatch.Running runningStopwatch) {
        this.displayName = displayName;
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.runningStopwatch = runningStopwatch;
    }

    String id() { return displayName; }
    String className() { return testClass; }
    String simpleClassName() {
        int dot = testClass.lastIndexOf('.');
        return dot == -1 ? testClass : testClass.substring(dot + 1);
    }
    String methodName() { return testMethod; }
    public String getSimpleClassMethod() { return simpleClassName() + "." + methodName() + "()"; }
    public String getClassMethod() { return className() + "." + methodName() + "()"; }

    void failTest(Failure failure) {
        synchronized (monitor) {
            if (finished) {
                throw new JakeException("Test already finished: " + toString());
            }

            duration = runningStopwatch.stop();
            this.failure = failure;
        }
    }

    void testAssumptionFailure(Failure failure) {
        synchronized (monitor) {
            if (finished) {
                throw new JakeException("Test already finished: " + toString());
            }

            duration = runningStopwatch.stop();
            this.failure = failure;
            this.assumptionFailure = true;
        }
    }

    /** Always invoked, after any failure/assumption failures if any. */
    void finishTest(Description description) {
        synchronized (monitor) {
            if (finished) {
                throw new JakeException("Test finished twice: " + toString());
            }
            finished = true;

            if (duration == null) {
                duration = runningStopwatch.stop();
            } else {
                // was done in testFailure() or testAssumptionFailure()
            }
        }
    }

    // Accessors post end of test run does not need synchronization.

    boolean isFinished() {
        return finished;
    }

    boolean isAssumptionFailure() {
        return assumptionFailure;
    }

    boolean isFailure() {
        return !assumptionFailure && failure != null;
    }

    @Override
    public String toString() {
        return getClassMethod();
    }

    public String getFailureHeading() {
        Objects.requireNonNull(failure);
        return toString() + ": " + failure.getMessage();
    }

    public String getTrace() {
        Objects.requireNonNull(failure);
        return failure.getTrace();
    }
}
