package fpmbuild;

import fpmbuild.project.BuildTool;
import fpmbuild.project.ProjectInfo;
import fpmbuild.project.ProjectInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectInspectorTest {
    @TempDir Path temp;

    @Test void findsMavenPrimaryJarAndFcdependencies() throws Exception {
        Files.writeString(temp.resolve("pom.xml"), """
                <project><modelVersion>4.0.0</modelVersion><groupId>x</groupId><artifactId>demo</artifactId><version>1</version>
                <properties><maven.compiler.release>25</maven.compiler.release></properties><build><plugins><plugin><artifactId>fcdependencies-maven-plugin</artifactId></plugin></plugins></build></project>
                """);
        Files.createDirectories(temp.resolve("target"));
        jar(temp.resolve("target/demo-1-sources.jar")); jar(temp.resolve("target/demo-1.jar"));
        ProjectInfo info = new ProjectInspector().inspect(temp);
        assertEquals(BuildTool.MAVEN, info.tool());
        assertEquals("25", info.javaRelease());
        assertTrue(info.fcDependencies());
        assertEquals("demo-1.jar", info.primaryArtifact().getFileName().toString());
    }

    @Test void findsGradleWrapperAboveSubprojectAndAvoidsPlainJar() throws Exception {
        Path sub=temp.resolve("sub");Files.createDirectories(sub.resolve("build/libs"));
        Files.writeString(temp.resolve("settings.gradle.kts"),"include(\"sub\")");Files.writeString(temp.resolve("gradlew"),"#!/bin/sh\n");
        Files.writeString(sub.resolve("build.gradle.kts"),"group = \"x\"\nversion = \"2\"\njava { toolchain { languageVersion = JavaLanguageVersion.of(25) } }\n// fcdependencies\n");
        jar(sub.resolve("build/libs/sub-2-plain.jar"));jar(sub.resolve("build/libs/sub-2.jar"));
        ProjectInfo info=new ProjectInspector().inspect(sub);
        assertEquals(BuildTool.GRADLE,info.tool());assertTrue(info.wrapper());assertTrue(info.fcDependencies());assertEquals("sub-2.jar",info.primaryArtifact().getFileName().toString());
    }

    private static void jar(Path p) throws Exception { try(JarOutputStream ignored=new JarOutputStream(Files.newOutputStream(p))){} }
}
