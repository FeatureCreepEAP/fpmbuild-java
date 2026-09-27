package fpmbuild.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record BuildContext(Path root, String dist) {
    public BuildContext {
        root = root.toAbsolutePath().normalize();
        dist = (dist == null || dist.isBlank()) ? "fc12" : dist;
    }

    public Path sources() { return root.resolve("SOURCES"); }
    public Path specs() { return root.resolve("SPECS"); }
    public Path build() { return root.resolve("BUILD"); }
    public Path buildRoot() { return root.resolve("BUILD_ROOT"); }
    public Path fpms() { return root.resolve("FPMS"); }
    public Path sfpms() { return root.resolve("SFPMS"); }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(sources()); Files.createDirectories(specs()); Files.createDirectories(build());
        Files.createDirectories(buildRoot()); Files.createDirectories(fpms()); Files.createDirectories(sfpms());
    }
}
