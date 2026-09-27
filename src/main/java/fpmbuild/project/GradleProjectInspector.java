package fpmbuild.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GradleProjectInspector {
    private static final Pattern ASSIGN = Pattern.compile("(?m)^\\s*(group|version|archivesBaseName|archiveBaseName)\\s*(?:=)?\\s*[\\\"']([^\\\"']+)[\\\"']");
    private static final Pattern JAVA_RELEASE = Pattern.compile("(?m)(?:JavaLanguageVersion\\.of\\(|languageVersion\\s*=\\s*JavaLanguageVersion\\.of\\(|options\\.release\\s*=\\s*)(\\d+)");

    ProjectInfo inspect(Path root) throws IOException {
        Path groovy = root.resolve("build.gradle");
        Path kotlin = root.resolve("build.gradle.kts");
        Path descriptor = Files.isRegularFile(kotlin) ? kotlin : Files.isRegularFile(groovy) ? groovy : settingsPath(root);
        String text = Files.isRegularFile(descriptor) ? Files.readString(descriptor) : "";
        Map<String,String> assigns = new LinkedHashMap<>();
        Matcher m = ASSIGN.matcher(text); while(m.find()) assigns.put(m.group(1),m.group(2));
        String group=assigns.getOrDefault("group","");
        String version=assigns.getOrDefault("version","");
        String artifact=assigns.getOrDefault("archivesBaseName",assigns.getOrDefault("archiveBaseName",root.getFileName().toString()));
        Matcher j=JAVA_RELEASE.matcher(text); String release=j.find()?j.group(1):"";
        Path wrapperPath = findWrapper(root);
        boolean wrapper = wrapperPath != null;
        boolean fcdeps=text.toLowerCase().contains("fcdependencies") || scanForFcDependencies(root);
        List<Path> jars=ProjectInspector.findJarCandidates(root.resolve("build").resolve("libs"), true);
        if (jars.isEmpty()) jars = findSubprojectJars(root);
        Path primary=ProjectInspector.choosePrimaryJar(jars,artifact,version);
        String executable=wrapper?wrapperPath.toAbsolutePath().normalize().toString():"gradle";
        List<String> command=new ArrayList<>();command.add(executable);command.add("clean");command.add("build");
        Map<String,Object> details=new LinkedHashMap<>();
        details.put("kotlinDsl", descriptor.endsWith("build.gradle.kts"));
        details.put("settings", settings(root));
        details.put("shadowPlugin", text.contains("com.github.johnrengelman.shadow")||text.contains("com.gradleup.shadow")||text.contains("shadowJar"));
        details.put("loomPlugin", text.toLowerCase().contains("fabric-loom")||text.contains("net.fabricmc.loom"));
        details.put("buildExecutable", executable);
        return new ProjectInfo(root,BuildTool.GRADLE,descriptor,group,artifact,version,release,wrapper,fcdeps,jars,primary,command,details);
    }

    private static boolean scanForFcDependencies(Path root) throws IOException {
        try(var s=Files.walk(root,2)){return s.filter(Files::isRegularFile).filter(p->p.getFileName().toString().startsWith("build.gradle")).anyMatch(p->{try{return Files.readString(p).toLowerCase().contains("fcdependencies");}catch(IOException e){return false;}});}
    }
    private static List<Path> findSubprojectJars(Path root) throws IOException {
        if(!Files.isDirectory(root))return List.of();
        try(var s=Files.walk(root,5)){return s.filter(Files::isRegularFile).filter(p->p.getParent()!=null&&p.getParent().endsWith("build/libs")).filter(p->p.getFileName().toString().endsWith(".jar")).toList();}
    }
    private static String settings(Path root){Path p=settingsPath(root);return p==null?"":p.toString();}
    private static Path settingsPath(Path root){Path cursor=root;while(cursor!=null){for(String n:List.of("settings.gradle.kts","settings.gradle")){Path p=cursor.resolve(n);if(Files.isRegularFile(p))return p;}cursor=cursor.getParent();}return null;}
    private static Path findWrapper(Path root){Path p=root;while(p!=null){for(String n:List.of(isWindows()?"gradlew.bat":"gradlew","gradlew","gradlew.bat")){Path w=p.resolve(n);if(Files.isRegularFile(w))return w;}p=p.getParent();}return null;}
    private static boolean isWindows(){return System.getProperty("os.name","").toLowerCase().contains("win");}
}
