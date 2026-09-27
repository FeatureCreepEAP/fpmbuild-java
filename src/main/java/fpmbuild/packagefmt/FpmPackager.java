package fpmbuild.packagefmt;

import fpmbuild.build.BuildContext;
import fpmbuild.project.ProjectInfo;
import fpmbuild.spec.MacroExpander;
import fpmbuild.spec.Spec;
import fpmbuild.util.FileTrees;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FpmPackager {
    public record Result(Path binaryFpm, Path sourceFpm, Path modZip, Path projectArtifact) {}

    public Result packageProject(Spec spec, BuildContext context, ProjectInfo project) throws IOException {
        if (project.primaryArtifact() == null) throw new IllegalArgumentException("No built JAR found for " + project.root());
        context.ensureDirectories();
        FileTrees.deleteContents(context.buildRoot());
        Path packing=context.buildRoot().resolve("packing");
        Path mods=context.buildRoot().resolve("mods");
        Files.createDirectories(packing);Files.createDirectories(mods);

        ZipTools.extractJar(project.primaryArtifact(), packing);
        Files.copy(spec.path(), packing.resolve(spec.path().getFileName()), StandardCopyOption.REPLACE_EXISTING);

        copyPrivateLibs(project.root(), mods);
        MacroExpander macros = new MacroExpander(spec, context);
        String base=spec.name()+"-"+spec.version()+"-"+macros.value("release")+".noarch";
        Path modZip=mods.resolve(base+".zip");
        ZipTools.zipDirectory(packing,modZip);
        Files.copy(modZip, context.build().resolve(modZip.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        Path fpm=context.fpms().resolve(base+".fpm");
        ZipTools.zipDirectory(packing,fpm);

        Path sfpmRoot=context.buildRoot().resolve("sfpm");
        Files.createDirectories(sfpmRoot);
        Files.copy(spec.path(), sfpmRoot.resolve(spec.path().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Path projectDst=sfpmRoot.resolve(project.root().getFileName());
        FileTrees.copySourceTree(project.root(),projectDst);
        Path sfpm=context.sfpms().resolve(base+".sfpm");
        ZipTools.zipDirectory(sfpmRoot,sfpm);
        FileTrees.deleteContents(context.buildRoot());
        return new Result(fpm,sfpm,context.build().resolve(modZip.getFileName()),project.primaryArtifact());
    }

    private static void copyPrivateLibs(Path project, Path mods) throws IOException {
        for (Path dir : new Path[]{project.resolve("libs"), project.resolve("lib")}) {
            if (!Files.isDirectory(dir)) continue;
            try (var stream=Files.list(dir)) {
                for(Path p:stream.filter(Files::isRegularFile).toList()) Files.copy(p,mods.resolve(p.getFileName()),StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static Map<String,Object> resultMap(Result r){Map<String,Object> m=new LinkedHashMap<>();m.put("fpm",r.binaryFpm().toString());m.put("sfpm",r.sourceFpm().toString());m.put("modZip",r.modZip().toString());m.put("projectArtifact",r.projectArtifact().toString());return m;}
}
