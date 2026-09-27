package fpmbuild.build;

import fpmbuild.packagefmt.FpmPackager;
import fpmbuild.project.BuildTool;
import fpmbuild.project.ProjectInfo;
import fpmbuild.project.ProjectInspector;
import fpmbuild.spec.MacroExpander;
import fpmbuild.spec.Spec;
import fpmbuild.superinject.SuperInjector;
import fpmbuild.util.FileTrees;
import fpmbuild.util.ShellWords;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpecBuildEngine {
    public record BuildResult(Spec spec, List<Map<String,Object>> packages) {}

    private final ProjectInspector inspector = new ProjectInspector();
    private final ProjectBuilder builder = new ProjectBuilder();
    private final FpmPackager packager = new FpmPackager();
    private final CommandRunner commands = new CommandRunner();
    private final SuperInjector superInjector = new SuperInjector();

    public BuildResult build(Spec spec, BuildContext context, boolean quiet) throws Exception {
        context.ensureDirectories();
        FileTrees.deleteContents(context.buildRoot());
        MacroExpander macros = new MacroExpander(spec, context);
        Path cwd = context.root();
        List<Map<String,Object>> packaged = new ArrayList<>();
        for (String raw : spec.section("build")) {
            String line = macros.expand(raw);
            if (line.isBlank()) continue;
            List<String> words = ShellWords.split(line);
            if (words.isEmpty()) continue;
            String op = words.getFirst();
            if (op.equals("cd")) {
                if (words.size() < 2) throw new IllegalArgumentException("cd requires a path");
                cwd = words.get(1).equals("/dev/null") ? context.root() : resolve(context.root(), cwd, words.get(1));
                continue;
            }
            if (op.equals("buildfpm_maven") || op.equals("buildfpm_gradle") || op.equals("buildfpm")) {
                if (words.size() < 2) throw new IllegalArgumentException(op + " requires a project path");
                Path project = resolve(context.root(), cwd, words.get(1));
                BuildTool forced = op.equals("buildfpm_maven") ? BuildTool.MAVEN : op.equals("buildfpm_gradle") ? BuildTool.GRADLE : BuildTool.UNKNOWN;
                boolean runBuild = words.stream().anyMatch(w -> w.equals("--build"));
                List<String> tasks = optionValues(words,"--task");
                ProjectInfo info = inspector.inspect(project);
                if (runBuild || info.primaryArtifact() == null) info = builder.build(project, forced, tasks, quiet);
                else if (forced != BuildTool.UNKNOWN && info.tool() != forced) throw new IllegalArgumentException("Expected " + forced + " project: " + project);
                SuperInjector.Result injection = null;
                if (spec.superInjection()) injection = superInjector.inject(info.primaryArtifact(), spec);
                FpmPackager.Result result = packager.packageProject(spec, context, info);
                Map<String,Object> packageMap = FpmPackager.resultMap(result);
                if (injection != null) packageMap.put("superInjection", injection.toMap());
                packaged.add(packageMap);
                continue;
            }
            commands.runShell(line, cwd, Map.of("FPM_BUILD_ROOT", context.buildRoot().toString()), quiet);
        }
        return new BuildResult(spec, packaged);
    }

    private static List<String> optionValues(List<String> words,String option){List<String> out=new ArrayList<>();for(int i=0;i<words.size()-1;i++)if(words.get(i).equals(option))out.add(words.get(++i));return out;}
    private static Path resolve(Path root,Path cwd,String value){Path p=Path.of(value);if(p.isAbsolute())return p.normalize();Path c=cwd.resolve(p).normalize();if(Files.exists(c))return c;return root.resolve(p).normalize();}
}
