package fpmbuild.build;

import fpmbuild.project.BuildTool;
import fpmbuild.project.ProjectInfo;
import fpmbuild.project.ProjectInspector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ProjectBuilder {
    private final ProjectInspector inspector = new ProjectInspector();
    private final CommandRunner runner = new CommandRunner();

    public ProjectInfo build(Path project, BuildTool requestedTool, List<String> tasks, boolean quiet) throws Exception {
        ProjectInfo before = inspector.inspect(project);
        BuildTool tool = requestedTool == BuildTool.UNKNOWN ? before.tool() : requestedTool;
        if (tool == BuildTool.UNKNOWN) throw new IllegalArgumentException("Could not detect Maven or Gradle project at " + project);
        List<String> command = switch (tool) {
            case MAVEN -> mavenCommand(before, tasks);
            case GRADLE -> gradleCommand(before, tasks);
            default -> throw new IllegalStateException();
        };
        runner.run(command, before.root(), Map.of(), quiet);
        ProjectInfo after = inspector.inspect(before.root());
        if (after.primaryArtifact() == null || !Files.isRegularFile(after.primaryArtifact())) {
            throw new IOException("Build completed but no primary JAR could be identified in " + after.root());
        }
        return after;
    }

    private static List<String> mavenCommand(ProjectInfo info, List<String> tasks) {
        String exe = String.valueOf(info.details().getOrDefault("buildExecutable", "mvn"));
        List<String> out = executablePrefix(exe);
        if (tasks == null || tasks.isEmpty()) { out.add("clean"); out.add("package"); }
        else out.addAll(tasks);
        return out;
    }

    private static List<String> gradleCommand(ProjectInfo info, List<String> tasks) {
        String exe = String.valueOf(info.details().getOrDefault("buildExecutable", "gradle"));
        List<String> out = executablePrefix(exe);
        if (tasks == null || tasks.isEmpty()) { out.add("clean"); out.add("build"); }
        else out.addAll(tasks);
        out.add("--console=plain");
        return out;
    }

    private static List<String> executablePrefix(String exe) {
        if (isWindows() && (exe.toLowerCase().endsWith(".cmd") || exe.toLowerCase().endsWith(".bat"))) {
            return new ArrayList<>(List.of("cmd.exe", "/d", "/c", exe));
        }
        Path p;
        try { p = Path.of(exe); } catch (Exception ignored) { return new ArrayList<>(List.of(exe)); }
        if (!isWindows() && p.isAbsolute() && Files.isRegularFile(p) && !Files.isExecutable(p)) {
            return new ArrayList<>(List.of("/bin/sh", exe));
        }
        return new ArrayList<>(List.of(exe));
    }

    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
}
