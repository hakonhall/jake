package no.ion.jake.io;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A path pattern can matched against a path relative to an implicit root directory.
 */
@FunctionalInterface
public interface PathPattern extends Predicate<Path> {

    /** Creates a path pattern that matches a path if the filename (last component of the path) has the given suffix. */
    static PathPattern forFilenameSuffix(String suffix) {
        return path -> path.getFileName().toString().endsWith(suffix);
    }

    /**
     * Creates a path pattern that matches a path if the regex, compiled to a {@link Pattern},
     * {@link Matcher#matches() matches()} the path.
     */
    static PathPattern fromRegex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return path -> pattern.matcher(path.toString()).matches();
    }

    /**
     * A path pattern is a String that matches a path, relative to some implicit root directory, according to the
     * following rules:
     *
     * <ol>
     *     <li>A pattern string "{@code *SUFFIX}" will match the path if its filename ends with "{@code SUFFIX}".</li>
     *     <li>All other patterns are illegal for now</li>
     * </ol>
     */
    static PathPattern of(String pathPattern) {
        if (pathPattern.isEmpty()) {
            throw new IllegalArgumentException("path pattern cannot be empty");
        }

        if (pathPattern.charAt(0) == '*' && pathPattern.indexOf('*', 1) == -1 && pathPattern.indexOf('/', 1) == -1) {
            String suffix = pathPattern.substring(1);
            return path -> path.getFileName().toString().endsWith(suffix);
        }

        throw new IllegalArgumentException("path pattern not supported: " + pathPattern);
    }

    static PathPattern ofAll() { return path -> true; }

    /**
     *
     * @param path can be assumed to be a non-empty relative path to a regular file (when following symlinks)
     * @return true iff the path pattern matches the {@code path}.
     */
    @Override
    boolean test(Path path);
}
