package no.ion.jake.maven;

public enum Scope {
    PROVIDED, COMPILE, TEST;

    public String toMavenScope() { return name().toLowerCase(); }
}
