package no.ion.jake.junit4.container;

import no.ion.jake.BuildContext;
import no.ion.jake.JakeException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RunListener.ThreadSafe
public class TestRun extends RunListener {
    private final BuildContext context;
    private final Map<String, Test> testsById = new ConcurrentHashMap<>();

    private Result result = null;
    private boolean done = false;

    public TestRun(BuildContext context) {
        this.context = context;
    }

    @Override
    public void testRunStarted(Description description) {
        context.logDebug("Starting test run");
    }

    @Override
    public void testRunFinished(Result result) {
        context.logDebug("Test run finished");

        if (done) {
            throw new JakeException("Test run finished invoked after run was finished");
        }

        testsById.values().stream()
                .filter(test -> !test.isAssumptionFailure())
                .forEach(test -> {
                    if (!test.isFinished()) {
                        throw new JakeException("Test has not finished: " + test.toString());
                    }
                });
    }

    @Override
    public void testStarted(Description description) {
        Test test = Test.fromTestStartedDescription(description);
        String id = test.id();

        Test conflictingTest = testsById.put(id, test);
        if (conflictingTest != null) {
            throw new JakeException("There are two JUnit 4 tests with the same ID: " + id);
        }

        context.logDebug("Running test " + test.toString());
    }

    /**
     * Invoked between testStarted() and testFinished() for a failed test.
     *
     * <p>If a class does not contain any tests, JUnit 4 will throw(!) an exception like
     * {@code Exception("initializationError(com.yahoo.slime.JsonBenchmark)")} and 1. invoke
     * {@link #testStarted(Description)} with the description, then 2. invoke this method.
     * The description is characterized by: 0 children.  The Failure is characterized by
     * detailMessage of "No runnable methods", the cause being the said {@code Exception}.
     * And during {@link #testRunStarted(Description)}, there is 1 child with a Description
     * with fTestClass/fDisplayName that classname, and a child with a Description as
     * described above and passed to testStarted and this method.</p>
     *
     * <p>If a test class has exactly 1 test method initializationError(), and that method
     * throws an {@code Exception("initializationError(" + className + ")")}, then it is
     * indistinguishable from the same class having 0 test methods.</p>
     *
     * <p>Except that the stack trace of the Exception would or would not contain a frame in the given class.</p>
     */
    @Override
    public void testFailure(Failure failure) {
        Description description = failure.getDescription();
        String id = Test.idFromDescription(description);
        Test test = testsById.get(id);
        if (test == null) {
            throw new JakeException("Failed test has not started: " + description.getDisplayName());
        }

        test.failTest(failure);

        context.logError("test failure: " + test.getSimpleClassMethod() + ": " + failure.getException().toString());
    }

    /** Test assumptions are used to skip tests. Invoked between testStarted() and testFinished(). */
    @Override
    public void testAssumptionFailure(Failure failure) {
        Description description = failure.getDescription();
        String id = Test.idFromDescription(description);
        Test test = testsById.get(id);
        if (test == null) {
            throw new JakeException("Failed test has not started: " + description.getDisplayName());
        }

        test.testAssumptionFailure(failure);

        context.logDebug("Test assumption failure: " + failure.toString());
    }

    @Override
    public void testIgnored(Description description) {
        // Ignore ignored tests
    }

    @Override
    public void testFinished(Description description) {
        String id = Test.idFromDescription(description);
        Test test = testsById.get(id);
        if (test == null) {
            throw new JakeException("Finished test has not started: " + description.getDisplayName());
        }

        test.finishTest(description);
    }

    void setEndResult(Result result) {
        this.result = result;
        done = true;
    }

    public boolean wasSuccessful() { return result.wasSuccessful(); }
    public double getRunTimeSeconds() { return result.getRunTime() / 1000.0; }
    public int getTestCount() { return result.getRunCount(); }
    public int getFailureCount() { return result.getFailureCount(); }

    public List<Test> getFailedTests() {
        return testsById.values().stream()
                .filter(Test::isFailure)
                .collect(Collectors.toList());
    }

    public static String getSummaryForZeroTests() {
        return "ran 0 JUnit4 tests in 0.000 s";
    }

    public String getSummary() {
        var builder = new StringBuilder();
        builder.append("ran ")
                .append(getTestCount())
                .append(String.format(" test%s in %.3f s",
                        getTestCount() == 1 ? "" : "s",
                        getRunTimeSeconds()));

        if (!result.wasSuccessful()) {
            builder.append(", and ")
                    .append(getFailureCount())
                    .append(" failed");

            getFailedTests().forEach(test -> builder
                    .append('\n')
                    .append(test.getFailureHeading())
                    .append('\n')
                    .append(test.getTrace()));
        }

        return builder.toString();
    }
}
