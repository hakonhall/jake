package no.ion.jake.graph;

public interface PartialOrder<T> {
    /** Returns -1 if left is before (aka "less") than right, +1 if left is after right, or 0 otherwise (equal or no order). */

    enum ORDER { CORRECT, REVERSE, UNORDERED }

    /**
     * Returns CORRECT is left is ordered before right, REVERSE if left is ordered after right, or UNORDERED if they
     * are not ordered with respect to each other or equal.
     */
    ORDER orderOf(T left, T right);
}
