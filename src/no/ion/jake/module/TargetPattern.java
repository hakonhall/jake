package no.ion.jake.module;

/**
 * The specification of the goal targets of the current build execution, e.g. "build everything",
 * "build a specific JAR file and run these tests", or "clean".  If a module X does not understand
 * all targets it should register all possible targets, as a module Y that understands the target may
 * depends on any of X's targets.
 */
public interface TargetPattern {
    /** Whether to clean up after previous builds of the module. */
    boolean clean();
}
