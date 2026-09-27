package fpmbuild.cli;

import fpmbuild.project.ProjectInfo;
import fpmbuild.spec.Spec;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Maps {
    private Maps() {}
    public static Map<String,Object> project(ProjectInfo p){
        Map<String,Object> m=new LinkedHashMap<>();m.put("root",p.root().toString());m.put("tool",p.tool().name().toLowerCase());m.put("descriptor",p.descriptor()==null?null:p.descriptor().toString());m.put("group",p.group());m.put("artifact",p.artifact());m.put("version",p.version());m.put("javaRelease",p.javaRelease());m.put("wrapper",p.wrapper());m.put("fcdependencies",p.fcDependencies());m.put("artifacts",p.artifactCandidates().stream().map(Path::toString).toList());m.put("primaryArtifact",p.primaryArtifact()==null?null:p.primaryArtifact().toString());m.put("suggestedBuildCommand",p.suggestedBuildCommand());m.put("details",p.details());return m;}
    public static Map<String,Object> spec(Spec s){Map<String,Object> m=new LinkedHashMap<>();m.put("path",s.path().toString());m.put("name",s.name());m.put("version",s.version());m.put("release",s.release());m.put("summary",s.summary());m.put("license",s.license());m.put("url",s.url());m.put("source0",s.source0());m.put("superInjection",s.superInjection());m.put("buildRequires",s.headerValues("buildrequires"));m.put("requires",s.headerValues("requires"));m.put("description",s.description());m.put("sections",s.sections());return m;}
}
