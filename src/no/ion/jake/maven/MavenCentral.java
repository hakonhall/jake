package no.ion.jake.maven;

import no.ion.jake.JakeException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class MavenCentral {
    private final HttpClient client;

    public MavenCentral() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /** Downloads Maven artifact to destinationPath.  Parent directories are created if necessary. */
    public void downloadTo(MavenArtifact mavenArtifact, Path destinationPath) {
        String subpath = mavenArtifact.getUriSubpath();
        URI url = URI.create("https://repo1.maven.org/maven2/" + subpath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        Path downloadPath = uncheckIO(() -> Files.createTempFile(destinationPath.getFileName().toString(), ".downloading"));

        boolean exceptionThrown[] = { true };
        HttpResponse<Path> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(downloadPath));
            exceptionThrown[0] = false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new UncheckedIOException(new IOException("interrupted", e));
        } finally {
            if (exceptionThrown[0]) {
                uncheckIO(() -> Files.deleteIfExists(downloadPath));
            }
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            uncheckIO(() -> Files.deleteIfExists(downloadPath));

            switch (response.statusCode()) {
                case 404:
                    throw new JakeException("failed to download " + mavenArtifact.toCoordinate() + " from maven central: " +
                            "Not Found: " + response.uri());
            }

            throw new JakeException("failed to download " + mavenArtifact + " from maven central: " + response.toString());
        }

        uncheckIO(() -> Files.createDirectories(destinationPath.getParent()));
        uncheckIO(() -> Files.move(downloadPath, destinationPath));
    }
}
