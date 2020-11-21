package no.ion.jake;

public class Version {
    private final String version;
    private final String bundleVersion;

    public static Version getDefault() {
        return new Version("7-SNAPSHOT", "7.0.0");
    }

    private Version(String version, String bundleVersion) {
        this.version = version;
        this.bundleVersion = bundleVersion;
    }

    public String version() { return version; }
    public String bundleVersion() { return bundleVersion; }

    @Override
    public String toString() { return version; }
}
