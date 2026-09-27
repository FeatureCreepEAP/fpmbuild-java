package fpmbuild.project;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ProjectInfo(
        Path root,
        BuildTool tool,
        Path descriptor,
        String group,
        String artifact,
        String version,
        String javaRelease,
        boolean wrapper,
        boolean fcDependencies,
        List<Path> artifactCandidates,
        Path primaryArtifact,
        List<String> suggestedBuildCommand,
        Map<String, Object> details) {
}
