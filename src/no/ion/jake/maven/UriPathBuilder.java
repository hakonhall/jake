package no.ion.jake.maven;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Class for building a URI path, escaping characters as necessary and according to 3.3. Path in RFC 3986 URI: Generic Syntax.
 */
public class UriPathBuilder {
    private final StringBuilder builder = new StringBuilder();

    /** For building a subpath the form 'segment *("/" segment)'. */
    public static UriPathBuilder ofSubpath(String... components) {
        UriPathBuilder builder = new UriPathBuilder();
        Arrays.stream(components).forEach(builder::appendSegment);
        return builder;
    }

    public UriPathBuilder appendSegments(String... segments) {
        Arrays.stream(segments).forEach(this::appendSegment);
        return this;
    }

    /** Adds a new segment to the path.  A segment is '*pchar', see . */
    public UriPathBuilder appendSegment(String segment) {
        if (builder.length() > 0) {
            builder.append('/');
        }

        return appendToSegment(segment);
    }

    public UriPathBuilder appendToSegment(char c) {
        if (mustBePercentEncoded(c)) {
            appendPercentEncodedToSegment(Character.toString(c));
        } else {
            builder.append(c);
        }

        return this;
    }

    /** Appends to the current segment. */
    public UriPathBuilder appendToSegment(String string) {
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; ) {
            if (mustBePercentEncoded(chars[i])) {
                int start = i++;
                for (; i < chars.length && !mustBePercentEncoded(chars[i]); ++i)
                    ; // do nothing

                String substring = new String(chars, start, i - start);
                appendPercentEncodedToSegment(substring);
            } else {
                int start = i++;
                for (; i < chars.length && mustBePercentEncoded(chars[i]); ++i)
                    ; // do nothing

                builder.append(chars, start, i - start);
            }
        }

        return this;
    }

    /** Percent encodes string, and appends it to the current segment. */
    public UriPathBuilder appendPercentEncodedToSegment(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            builder.append('%')
                   .append(Character.forDigit((b >> 4) & 0xF, 16))
                   .append(Character.forDigit(b & 0xF, 16));
        }

        return this;
    }

    @Override
    public String toString() { return builder.toString(); }

    /**
     * @return true if the code point of a path segment must be percent encoded.
     * @apiNote for all chars C of a code point CP, mustBePercentEncoded(C) == mustBePercentEncoded(CP), which makes
     *          it easy to loop over chars.
     */
    public static boolean mustBePercentEncoded(int codePointOrChar) {
        // unreserved, part 1
        if (('a' <= codePointOrChar && codePointOrChar <= 'z') ||
                ('A' <= codePointOrChar && codePointOrChar <= 'Z') ||
                ('0' <= codePointOrChar && codePointOrChar <= '9')) {
            return false;
        }

        switch (codePointOrChar) {
            // unreserved, part 2
            case '-':
            case '.':
            case '_':
            case '~':

                // sub-delims
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case ';':
            case '=':

                // unnamed others
            case ':':
            case '@':

                return false;
        }

        return true;
    }
}
