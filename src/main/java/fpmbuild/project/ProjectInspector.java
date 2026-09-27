package fpmbuild.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ProjectInspector {
    public ProjectInfo inspect(Path input) throws Exception {
        Path root = findRoot(input.toAbsolutePath().normalize());
        if (Files.isRegularFile(root.resolve("pom.xml"))) return new MavenProjectInspector().inspect(root);
        if (isGradle(root)) return new GradleProjectInspector().inspect(root);
        return new ProjectInfo(root, BuildTool.UNKNOWN, null, "", root.getFileName().toString(), "", "", false, false, List.of(), null, List.of(), java.util.Map.of());
    }

    public static Path findRoot(Path input) {
        Path p = Files.isDirectory(input) ? input : input.getParent();
        Path candidate = p;
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml")) || isGradle(candidate)) return candidate;
            candidate = candidate.getParent();
        }
        return p;
    }

    private static boolean isGradle(Path p) {
        return Files.isRegularFile(p.resolve("build.gradle")) || Files.isRegularFile(p.resolve("build.gradle.kts"))
                || Files.isRegularFile(p.resolve("settings.gradle")) || Files.isRegularFile(p.resolve("settings.gradle.kts"));
    }

    static List<Path> findJarCandidates(Path dir, boolean recursive) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = recursive ? Files.walk(dir, 5) : Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted(Comparator.comparing(ProjectInspector::mtime).reversed())
                    .toList();
        }
    }

    public static Path choosePrimaryJar(List<Path> candidates, String artifact, String version) {
        return candidates.stream().min(Comparator.comparingInt((Path p) -> score(p, artifact, version)).reversed()).orElse(null);
    }

    private static int score(Path p, String artifact, String version) {
        String n=p.getFileName().toString().toLowerCase(Locale.ROOT); int s=0;
        if(artifact!=null&&!artifact.isBlank()&&n.contains(artifact.toLowerCase(Locale.ROOT)))s+=20;
        if(version!=null&&!version.isBlank()&&n.contains(version.toLowerCase(Locale.ROOT)))s+=10;
        if(n.contains("-sources"))s-=100; if(n.contains("-javadoc"))s-=100; if(n.contains("-tests"))s-=80;
        if(n.contains("-plain"))s-=30; if(n.contains("-all")||n.contains("shadow"))s+=15; if(n.contains("remap"))s+=8;
        return s;
    }

    private static FileTime mtime(Path p){try{return Files.getLastModifiedTime(p);}catch(IOException e){return FileTime.fromMillis(0);}}
}
