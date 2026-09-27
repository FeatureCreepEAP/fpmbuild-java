package fpmbuild.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MavenProjectInspector {
    ProjectInfo inspect(Path root) throws Exception {
        Path pom = root.resolve("pom.xml");
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document d = f.newDocumentBuilder().parse(pom.toFile());
        Element project = d.getDocumentElement();
        Map<String, String> props = properties(project);
        String group = resolve(first(project,"groupId", nested(project,"parent","groupId")), props);
        String artifact = resolve(first(project,"artifactId"), props);
        String version = resolve(first(project,"version", nested(project,"parent","version")), props);
        String release = resolve(first(project,"maven.compiler.release"), props);
        if (release.isBlank()) release = resolve(props.getOrDefault("maven.compiler.release", props.getOrDefault("maven.compiler.target", "")), props);
        boolean wrapper = Files.isRegularFile(root.resolve(isWindows() ? "mvnw.cmd" : "mvnw")) || Files.isRegularFile(root.resolve("mvnw")) || Files.isRegularFile(root.resolve("mvnw.cmd"));
        boolean fcdeps = Files.readString(pom).contains("fcdependencies");
        List<Path> jars = ProjectInspector.findJarCandidates(root.resolve("target"), false);
        Path primary = ProjectInspector.choosePrimaryJar(jars, artifact, version);
        String executable;
        if (wrapper) {
            Path wp = Files.isRegularFile(root.resolve(isWindows() ? "mvnw.cmd" : "mvnw"))
                    ? root.resolve(isWindows() ? "mvnw.cmd" : "mvnw")
                    : root.resolve(Files.isRegularFile(root.resolve("mvnw")) ? "mvnw" : "mvnw.cmd");
            executable = wp.toAbsolutePath().normalize().toString();
        } else executable = "mvn";
        Map<String,Object> details = new LinkedHashMap<>();
        details.put("packaging", first(project,"packaging").isBlank()?"jar":first(project,"packaging"));
        details.put("targetDirectory", root.resolve("target").toString());
        details.put("buildExecutable", executable);
        return new ProjectInfo(root, BuildTool.MAVEN, pom, group, artifact, version, release, wrapper, fcdeps,
                jars, primary, List.of(executable,"clean","package"), details);
    }

    private static Map<String,String> properties(Element project) {
        Map<String,String> out=new LinkedHashMap<>();
        for (Node n=project.getFirstChild(); n!=null; n=n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals("properties")) {
                for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling()) if(c instanceof Element ce) out.put(ce.getTagName(),ce.getTextContent().strip());
            }
        }
        return out;
    }
    private static String first(Element e,String... names){for(String n:names){String v=nested(e,n);if(!v.isBlank())return v;}return "";}
    private static String nested(Element e,String... path){Element cur=e;for(String p:path){Element next=null;for(Node n=cur.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element ce&&ce.getTagName().equals(p)){next=ce;break;}if(next==null)return "";cur=next;}return cur.getTextContent().strip();}
    private static String resolve(String value, Map<String,String> props){ if(value==null)return ""; String out=value; for(int round=0;round<8;round++){String before=out;for(var e:props.entrySet())out=out.replace("${"+e.getKey()+"}",e.getValue());if(before.equals(out))break;}return out;}
    private static boolean isWindows(){return System.getProperty("os.name","").toLowerCase().contains("win");}
}
