package fpmbuild.build;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandRunner {
    public record Result(int exitCode, List<String> command) {}

    public Result runShell(String command, Path cwd, Map<String, String> extraEnv, boolean quiet) throws IOException, InterruptedException {
        List<String> cmd = shellCommand(command);
        if (!quiet) System.err.println("[fpmbuild] $ (cd " + cwd + " && " + command + ")");
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(cwd.toFile()).redirectErrorStream(true);
        pb.environment().putAll(extraEnv);
        Process process = pb.start();
        process.getInputStream().transferTo(System.err);
        int code = process.waitFor();
        if (code != 0) throw new IOException("Command failed with exit code " + code + ": " + command);
        return new Result(code, cmd);
    }

    public Result run(List<String> command, Path cwd, Map<String, String> extraEnv, boolean quiet) throws IOException, InterruptedException {
        if (!quiet) System.err.println("[fpmbuild] $ (cd " + cwd + " && " + String.join(" ", command) + ")");
        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command)).directory(cwd.toFile()).redirectErrorStream(true);
        pb.environment().putAll(extraEnv);
        Process process = pb.start();
        process.getInputStream().transferTo(System.err);
        int code = process.waitFor();
        if (code != 0) throw new IOException("Command failed with exit code " + code + ": " + String.join(" ", command));
        return new Result(code, command);
    }

    private static List<String> shellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return List.of("cmd.exe", "/d", "/s", "/c", command);
        return List.of("/bin/sh", "-lc", command);
    }
}
