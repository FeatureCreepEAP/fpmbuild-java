package fpmbuild;

import fpmbuild.build.BuildContext;
import fpmbuild.build.SpecBuildEngine;
import fpmbuild.spec.Spec;
import fpmbuild.spec.SpecParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PackagingTest {
    @TempDir Path temp;
    @Test void packagesAlreadyBuiltMavenArtifact() throws Exception {
        Path project=temp.resolve("SOURCES/demo");Files.createDirectories(project.resolve("target"));Files.createDirectories(temp.resolve("SPECS"));
        Files.writeString(project.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><groupId>x</groupId><artifactId>demo</artifactId><version>1</version></project>");
        try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(project.resolve("target/demo-1.jar")))){jar.putNextEntry(new JarEntry("x.txt"));jar.write("x".getBytes());jar.closeEntry();}
        Path specPath=temp.resolve("SPECS/demo.spec");Files.writeString(specPath,"Name: demo\nVersion: 12\nRelease: 1.%{?dist}\n%description\nx\n%build\nbuildfpm_maven %{?sources_location}/demo\n%install\n");
        Spec spec=new SpecParser().parse(specPath);var result=new SpecBuildEngine().build(spec,new BuildContext(temp,"fc12"),true);
        assertEquals(1,result.packages().size());assertTrue(Files.isRegularFile(temp.resolve("FPMS/demo-12-1.fc12.noarch.fpm")));assertTrue(Files.isRegularFile(temp.resolve("SFPMS/demo-12-1.fc12.noarch.sfpm")));
    }
}
