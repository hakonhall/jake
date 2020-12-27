package no.ion.jake.maven;

import no.ion.jake.UserError;
import no.ion.jake.util.Java;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class MavenArtifactId {
    private final String groupId;
    private final String artifactId;
    private final String versionOrNull;
    private final String classifierOrNull;

    // TODO: Sort out what we actually mean: either packaging, type, or extension.
    // See https://maven.apache.org/maven-core/artifact-handlers.html
    private final String packaging;

    private final Scope scope;

    public static MavenArtifactId from(String groupId, String artifactId) {
        return new MavenArtifactId(groupId, artifactId, null, null, null, null);
    }

    /**
     * Coordinate must be of the form GROUP:ID[:VERSION] or GROUP:ID:[VERSION]:CLASSIFIER.
     *
     * <p>WARNING: Some coordinates are of the form GROUP:ID:TYPE:VERSION (TYPE or PACKAGING).</p>
     */
    public static MavenArtifactId fromCoordinate(String coordinate) {
        if (coordinate.indexOf('/') != -1) {
            throw new UserError("bad maven coordinate: " + coordinate);
        }

        String[] elements = coordinate.split(":", -1);
        switch (elements.length) {
            case 2: return new MavenArtifactId(elements[0], elements[1], null, null, null, null);
            case 3: return new MavenArtifactId(elements[0], elements[1], elements[2], null, null, null);
            case 4: return new MavenArtifactId(elements[0], elements[1], elements[2].isEmpty() ? null : elements[2], elements[3], null, null);
            default: throw new UserError("bad maven coordinate: " + coordinate);
        }
    }

    private MavenArtifactId(String groupId, String artifactId,
                            String versionOrNull, String classifierOrNull, String packagingOrNull, Scope scopeOrNull) {
        this.groupId = validateGroupId(groupId);
        this.artifactId = validateArtifactId(artifactId);
        this.versionOrNull = versionOrNull;
        this.classifierOrNull = classifierOrNull;
        this.packaging = validatePackaging(packagingOrNull);
        this.scope = Optional.ofNullable(scopeOrNull).orElse(Scope.COMPILE);
    }

    public String groupId() { return groupId; }
    public String artifactId() { return artifactId; }
    public String version() { return requireNonNull(versionOrNull); }
    public String classifier() { return requireNonNull(classifierOrNull); }
    public String packaging() { return packaging; }
    public Scope scope() { return scope; }

    public Optional<String> optionalVersion() { return Optional.ofNullable(versionOrNull); }
    public Optional<String> optionalClassifier() { return Optional.ofNullable(classifierOrNull); }

    public MavenArtifactId withVersion(String version) {
        return new MavenArtifactId(groupId, artifactId, validateVersion(version), classifierOrNull, packaging, scope);
    }

    public MavenArtifactId withClassifier(String classifier) {
        return new MavenArtifactId(groupId, artifactId, versionOrNull, validateClassifier(classifier), packaging, scope);
    }

    public MavenArtifactId withPackaging(String packaging) {
        return new MavenArtifactId(groupId, artifactId, versionOrNull, classifierOrNull, validatePackaging(packaging), scope);
    }

    public MavenArtifactId withScope(Scope scope) {
        return new MavenArtifactId(groupId, artifactId, versionOrNull, classifierOrNull, packaging, scope);
    }

    public String filename() {
        return artifactId + '-' +
                requireNonNull(versionOrNull, "missing version: unable to generate filename")
                + (classifierOrNull == null ? "" : '-' + classifierOrNull) +
                '.' + packaging;
    }

    public String toRepoPath() {
        requireNonNull(versionOrNull, "missing version: required to generated repository path");
        var path = new StringBuilder()
                .append(groupId.replace('.', '/'))
                .append('/')
                .append(artifactId)
                .append('/')
                .append(versionOrNull)
                .append('/')
                .append(artifactId)
                .append('-')
                .append(versionOrNull);
        if (classifierOrNull != null) {
            path.append('-').append(classifierOrNull);
        }
        path.append('.').append(packaging);
        return path.toString();
    }

    /**
     * Returns a URI subpath of the form group1/.../groupN/artifactId/version/artifactId-version[-classifier].packaging',
     * with each segment being URI percent-encoded as necessary.
     */
    public String getUriSubpath() {
        requireNonNull(versionOrNull, "missing version: required to generated repository path");

        UriPathBuilder path = UriPathBuilder.ofSubpath()
                .appendSegments(groupId.split("\\.", -1))
                .appendSegment(artifactId)
                .appendSegment(versionOrNull)
                .appendSegment(artifactId)
                .appendToSegment('-')
                .appendToSegment(versionOrNull);
        optionalClassifier().ifPresent(classifier -> path.appendToSegment('-').appendToSegment(classifier));
        path.appendToSegment('.')
                .appendToSegment(packaging);

        return path.toString();
    }

    /** Group ID, artifact ID, version, and classifier.  NOT type/packaging - use filename() for that. */
    public String toCoordinate() {
        if (versionOrNull == null) {
            if (classifierOrNull == null) {
                return groupId + ':' + artifactId;
            } else {
                return groupId + ':' + artifactId + "::" + classifierOrNull;
            }
        } else {
            if (classifierOrNull == null) {
                return groupId + ':' + artifactId + ':' + versionOrNull;
            } else {
                return groupId + ':' + artifactId + ':' + versionOrNull + ':' + classifierOrNull;
            }
        }
    }

    private static String nullAsEmpty(String stringOrNull) { return stringOrNull == null ? "" : stringOrNull; }

    @Override
    public String toString() {
        return "MavenArtifactId{" + toCoordinate() +
                ", packaging='" + packaging + '\'' +
                ", scope=" + scope +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenArtifactId that = (MavenArtifactId) o;
        return groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                Objects.equals(versionOrNull, that.versionOrNull) &&
                Objects.equals(classifierOrNull, that.classifierOrNull) &&
                packaging.equals(that.packaging) &&
                scope == that.scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, versionOrNull, classifierOrNull, packaging, scope);
    }

    /** Maven doc: "A group ID should follow Java's package name rules" */
    private static String validateGroupId(String groupId) {
        requireNonNull(groupId);
        if (!Java.isValidClassName(groupId)) {
            throw new IllegalArgumentException("Invalid groupId: " + groupId);
        }

        return groupId;
    }

    private static String validateArtifactId(String artifactId) {
        requireNonNull(artifactId);
        if (artifactId.indexOf('/') != -1) {
            throw new IllegalArgumentException("artifactId cannot contain '/': " + artifactId);
        }
        if (artifactId.indexOf(':') != -1) {
            throw new IllegalArgumentException("artifactId cannot contain ':': " + artifactId);
        }
        return artifactId;
    }

    private static String validateVersion(String version) {
        requireNonNull(version);
        if (version.indexOf('/') != -1) {
            throw new IllegalArgumentException("version cannot contain '/': " + version);
        }
        if (version.indexOf(':') != -1) {
            throw new IllegalArgumentException("version cannot contain ':': " + version);
        }
        return version;
    }

    private static String validateClassifier(String classifier) {
        requireNonNull(classifier);
        if (classifier.indexOf('/') != -1) {
            throw new IllegalArgumentException("classifier cannot contain '/': " + classifier);
        }
        if (classifier.indexOf(':') != -1) {
            throw new IllegalArgumentException("classifier cannot contain ':': " + classifier);
        }
        return classifier;
    }

    private static String validatePackaging(String packaging) {
        if (packaging == null) {
            packaging = "jar";
        }
        if (packaging.indexOf('/') != -1) {
            throw new IllegalArgumentException("packaging cannot contain '/': " + packaging);
        }
        if (packaging.indexOf(':') != -1) {
            throw new IllegalArgumentException("packaging cannot contain ':': " + packaging);
        }
        return packaging;
    }
}
