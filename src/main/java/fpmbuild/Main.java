package fpmbuild;

import fpmbuild.agent.AgentServer;
import fpmbuild.build.BuildContext;
import fpmbuild.build.ProjectBuilder;
import fpmbuild.build.SpecBuildEngine;
import fpmbuild.bytecode.JarInspector;
import fpmbuild.cli.Arguments;
import fpmbuild.cli.Maps;
import fpmbuild.json.Json;
import fpmbuild.project.BuildTool;
import fpmbuild.project.ProjectInfo;
import fpmbuild.project.ProjectInspector;
import fpmbuild.spec.MacroExpander;
import fpmbuild.spec.Spec;
import fpmbuild.spec.SpecParser;
import fpmbuild.superinject.SuperInjector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0].equals("help") || args[0].equals("--help") || args[0].equals("-h")) { help(); return; }
            if (args[0].equals("agent") && args.length > 1 && args[1].equals("--stdio")) { new AgentServer().run(); return; }
            if (looksLegacy(args)) { legacy(args); return; }
            Arguments a = new Arguments(args,1); boolean json=a.flag("json"); Map<String,Object> result=execute(args[0],a);
            if(json)System.out.println(Json.stringify(Json.object("ok",true,"result",result)));else human(args[0],result);
        } catch (Exception e) {
            System.err.println("[fpmbuild] ERROR: " + e.getMessage());
            if (java.util.Arrays.asList(args).contains("--json")) System.out.println(Json.stringify(Json.object("ok",false,"error",e.getClass().getSimpleName(),"message",String.valueOf(e.getMessage()))));
            System.exit(1);
        }
    }

    private static boolean looksLegacy(String[] args){return args.length>=3&&args[1].equals("-ba");}
    private static void legacy(String[] args) throws Exception {Path root=Path.of(args[0]);Path spec=Path.of(args[2]);Map<String,Object> r=buildSpec(root,spec,System.getenv().getOrDefault("FPMBUILD_DIST","fc12"),false);human("build",r);}

    private static Map<String,Object> execute(String command, Arguments a) throws Exception {
        return switch(command){
            case "capabilities" -> capabilities();
            case "inspect-spec" -> inspectSpec(Path.of(required(a,"spec")), Path.of(a.option("root",".")), a.option("dist",System.getenv().getOrDefault("FPMBUILD_DIST","fc12")));
            case "inspect-project" -> Maps.project(new ProjectInspector().inspect(Path.of(a.option("path","."))));
            case "artifacts" -> Maps.project(new ProjectInspector().inspect(Path.of(a.option("path","."))));
            case "build-project" -> buildProject(a);
            case "build" -> buildSpec(Path.of(a.option("root",".")),Path.of(required(a,"spec")),a.option("dist",System.getenv().getOrDefault("FPMBUILD_DIST","fc12")),a.flag("quiet"));
            case "inspect-jar" -> inspectJar(a);
            case "bytecode-context" -> bytecodeContext(a);
            case "superinject" -> superInject(a);
            case "shutdown" -> Json.object("shutdown",true);
            default -> throw new IllegalArgumentException("Unknown command: "+command);
        };}

    public static Map<String,Object> executeForAgent(String[] args) throws Exception {
        if(args.length==0)return capabilities();
        Arguments a=new Arguments(args,1);return execute(args[0],a);
    }

    private static Map<String,Object> buildProject(Arguments a) throws Exception {
        Path path=Path.of(a.option("path","."));BuildTool tool=BuildTool.parse(a.option("tool","auto"));List<String> tasks=a.values("task");ProjectInfo info=new ProjectBuilder().build(path,tool,tasks,a.flag("quiet"));return Maps.project(info);
    }
    private static Map<String,Object> bytecodeContext(Arguments a) throws Exception {
        ProjectInfo p = new ProjectInspector().inspect(Path.of(a.option("path", ".")));
        if (p.primaryArtifact() == null) throw new IllegalArgumentException("No built JAR found for project");
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("project", Maps.project(p));
        out.put("jar", new JarInspector().inspect(p.primaryArtifact()));
        out.put("fcdependencies", p.fcDependencies());
        out.put("mutationBackend", "java.lang.classfile");
        out.put("mutationStatus", "active for spec SuperInjection post-build metadata/classes");
        out.put("safety", "SuperInjection rewrites the final JAR atomically and reports input/output SHA-256");
        return out;
    }


    private static Map<String,Object> superInject(Arguments a) throws Exception {
        Path specPath = Path.of(required(a,"spec"));
        Spec spec = new SpecParser().parse(specPath);
        Path jar;
        String explicit = a.option("jar","");
        if (!explicit.isBlank()) jar = Path.of(explicit);
        else {
            ProjectInfo p = new ProjectInspector().inspect(Path.of(a.option("path",".")));
            if (p.primaryArtifact() == null) throw new IllegalArgumentException("No built JAR found; pass --jar or build the project first");
            jar = p.primaryArtifact();
        }
        return new SuperInjector().inject(jar,spec).toMap();
    }

    private static Map<String,Object> inspectJar(Arguments a) throws Exception {
        Path jar;
        String j=a.option("jar","");
        if(!j.isBlank())jar=Path.of(j);else{ProjectInfo p=new ProjectInspector().inspect(Path.of(a.option("path",".")));if(p.primaryArtifact()==null)throw new IllegalArgumentException("No built JAR found; pass --jar or build the project first");jar=p.primaryArtifact();}
        return new JarInspector().inspect(jar);
    }
    private static Map<String,Object> buildSpec(Path root,Path specPath,String dist,boolean quiet) throws Exception {
        BuildContext ctx=new BuildContext(root,dist);Spec spec=new SpecParser().parse(specPath);SpecBuildEngine.BuildResult r=new SpecBuildEngine().build(spec,ctx,quiet);Map<String,Object> out=new LinkedHashMap<>();out.put("spec",Maps.spec(spec));out.put("root",ctx.root().toString());out.put("dist",ctx.dist());out.put("packages",r.packages());return out;
    }
    private static Map<String,Object> inspectSpec(Path specPath,Path root,String dist) throws Exception {Spec s=new SpecParser().parse(specPath);BuildContext ctx=new BuildContext(root,dist);Map<String,Object> out=Maps.spec(s);out.put("expandedRelease",new MacroExpander(s,ctx).value("release"));return out;}
    private static Map<String,Object> capabilities(){Map<String,Object> out=new LinkedHashMap<>();out.put("name","fpmbuild-java");out.put("version","0.0.1-SNAPSHOT");out.put("java",Runtime.version().feature());out.put("requiredJava",25);out.put("buildTools",List.of("maven","gradle"));out.put("commands",List.of("inspect-spec","inspect-project","build-project","build","artifacts","inspect-jar","bytecode-context","superinject","agent --stdio"));out.put("legacyCli","<root> -ba <spec>");out.put("agentProtocol","JSON Lines over stdin/stdout");out.put("classFileApiAvailable",classFileApiAvailable());out.put("bytecodeMutation","java.lang.classfile active for SuperInjection bridge generation");return out;}
    private static boolean classFileApiAvailable(){try{Class.forName("java.lang.classfile.ClassFile");return true;}catch(ClassNotFoundException e){return false;}}
    private static String required(Arguments a,String key){String v=a.option(key,"");if(v.isBlank())throw new IllegalArgumentException("--"+key+" is required");return v;}
    private static void human(String command,Map<String,Object> result){if(command.equals("inspect-project")||command.equals("artifacts")){System.out.println("Build tool: "+result.get("tool"));System.out.println("Project: "+result.get("root"));System.out.println("Primary JAR: "+result.get("primaryArtifact"));System.out.println("FCDependencies: "+result.get("fcdependencies"));return;}if(command.equals("inspect-jar")){System.out.println("JAR: "+result.get("path"));System.out.println("SHA-256: "+result.get("sha256"));System.out.println("Classes: "+result.get("classes"));return;}System.out.println(Json.stringify(result));}
    private static void help(){System.out.println("""
FPMBuild Java 25

Compatibility:
  fpmbuild <fpmbuild-root> -ba <spec-file>

Modern CLI:
  fpmbuild capabilities [--json]
  fpmbuild inspect-spec --spec FILE [--root DIR] [--dist fc12] [--json]
  fpmbuild inspect-project --path DIR [--json]
  fpmbuild build-project --path DIR [--tool auto|maven|gradle] [--task TASK ...] [--json]
  fpmbuild artifacts --path DIR [--json]
  fpmbuild build --root DIR --spec FILE [--dist fc12] [--json]
  fpmbuild inspect-jar --jar FILE [--json]
  fpmbuild inspect-jar --path PROJECT [--json]
  fpmbuild bytecode-context --path PROJECT [--json]
  fpmbuild superinject --spec FILE (--jar FILE | --path PROJECT) [--json]
  fpmbuild agent --stdio

Spec header:
  SuperInjection: true   # post-build superloader metadata/class injection

Spec build commands:
  buildfpm <project> [--build] [--task TASK]
  buildfpm_maven <project> [--build] [--task GOAL]
  buildfpm_gradle <project> [--build] [--task TASK]

AI agent protocol:
  One JSON object per line on stdin, one JSON response per line on stdout.
  Example: {\"id\":1,\"command\":\"inspect-project\",\"args\":[\"--path\",\"/work/mod\"]}
""");}
}
