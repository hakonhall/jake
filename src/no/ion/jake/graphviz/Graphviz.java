package no.ion.jake.graphviz;

import no.ion.jake.engine.ArtifactId;
import no.ion.jake.engine.ArtifactImpl;
import no.ion.jake.engine.BuildId;
import no.ion.jake.engine.BuildInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Graphviz {
    private final StringBuilder dot = new StringBuilder();
    private boolean bol = true;
    private int indentationLevel = 0;
    private final String oneIndent = "  ";

    private final Map<ArtifactId, ArtifactImpl<?>> artifacts;
    private final Map<BuildId, BuildInfo> builds;

    public Graphviz(Map<ArtifactId, ArtifactImpl<?>> artifacts, Map<BuildId, BuildInfo> builds) {
        this.artifacts = artifacts;
        this.builds = builds;
    }

    public String make() {
        appendln("digraph \"Build graph\" {");
        ++indentationLevel;

        Set<String> namespaces = builds.keySet().stream().map(BuildId::namespace).collect(Collectors.toSet());
        List<String> namespaceList = namespaces.stream()
                .sorted()
                .collect(Collectors.toList());

        int i = 0;
        for (; i < namespaceList.size(); ++i) {
            String namespace = namespaceList.get(i);

            append("subgraph cluster").append(Integer.toString(i)).appendln(" {");
            ++indentationLevel;

            append("label = \"").append(namespace).appendln("\";");
            appendln("color = blue;");

            appendln("// Builds");
            builds.values().stream()
                    // TODO: Instead, group by namespace
                    .filter(info -> info.id().namespace().equals(namespace))
                    .sorted(Comparator.comparing(buildInfo -> nameOf(buildInfo.id())))
                    .forEach(buildInfo -> {
                        appendIdOf(buildInfo.id());
                        append(" [label = ").appendNameOf(buildInfo.id()).appendln(", shape=plaintext]");
                    });

            appendln("// Artifacts");
            artifacts.values().stream()
                    // TODO: Instead, group by namespace
                    .filter(artifact -> artifact.artifactId().namespace().equals(namespace))
                    .forEach(artifact -> {
                        appendIdOf(artifact.artifactId()).append(" [label = ").appendNameOf(artifact.artifactId())
                                .appendln("]");
                    });

            --indentationLevel;
            appendln("}");
        }

        builds.forEach((buildId, buildInfo) -> {
            buildInfo.dependencies().forEach(dependencyArtifactId -> {
                appendIdOf(dependencyArtifactId).append(" -> ").appendIdOf(buildId).appendln();
            });
            buildInfo.production().forEach(producedArtifactId -> {
                appendIdOf(buildId).append(" -> ").appendIdOf(producedArtifactId).appendln();
            });
        });

        --indentationLevel;
        appendln("}");

        return dot.toString();
    }

    private Graphviz appendIdOf(ArtifactId artifactId) {
        append(idOf(artifactId));
        return this;
    }

    private Graphviz appendNameOf(ArtifactId artifactId) {
        append(nameOf(artifactId));
        return this;
    }

    private Graphviz appendIdOf(BuildId buildId) {
        append(idOf(buildId));
        return this;
    }

    private Graphviz appendNameOf(BuildId buildId) {
        append(nameOf(buildId));
        return this;
    }

    private static String idOf(ArtifactId artifactId) { return '"' + artifactId.namespace() + ':' + artifactId.artifactName() + '"'; }
    private static String nameOf(ArtifactId artifactId) { return '"' + artifactId.artifactName() + '"'; }
    private static String idOf(BuildId buildId) { return '"' + buildId.namespace() + ':' + buildId.id() + '"'; }
    private static String nameOf(BuildId buildId) { return '"' + buildId.id() + '"'; }

    private Graphviz append(String string) {
        if (bol) {
            dot.append(oneIndent.repeat(indentationLevel));
            bol = false;
        }

        dot.append(string);
        return this;
    }

    private Graphviz appendln(String string) { return append(string).appendln(); }

    private Graphviz appendln() {
        dot.append('\n');
        bol = true;
        return this;
    }
}
