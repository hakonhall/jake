package no.ion.jake.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtil {
    /** The difference between 'left' and 'right': Returns the set of elements in 'left' but not in 'right'. */
    public static <T> Set<T> difference(Set<T> left, Set<T> right) {
        Set<T> remaining = new HashSet<>(left);
        remaining.removeAll(right);
        return remaining;
    }
}
