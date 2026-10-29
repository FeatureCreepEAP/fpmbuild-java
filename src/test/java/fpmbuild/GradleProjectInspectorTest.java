package fpmbuild;

import fpmbuild.project.ProjectInfo;
import fpmbuild.project.ProjectInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GradleProjectInspectorTest {
    @TempDir Path temp;

    @Test void detectsFcdependenciesPluginAndRunTasksOn26_1_2() throws Exception {
        ProjectInfo info = inspect("""
                plugins { id("com.asbestosstar.fcdependencies") version "0.0.1" }
                fcdependencies { minecraftVersion = "26.1.2" }
                """);
        assertEquals(true, info.details().get("fcdependenciesPlugin"));
        assertEquals("26.1.2", info.details().get("minecraftVersion"));
        assertEquals(true, info.details().get("supportsRunTasks"));
        assertEquals(false, info.details().get("loomPlugin"));
    }

    @Test void loomOnlyProjectReportsLoomAndNoFcdependenciesPlugin() throws Exception {
        ProjectInfo info = inspect("""
                plugins { id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" }
                minecraftVersion = "26.3"
                """);
        assertEquals(true, info.details().get("loomPlugin"));
        assertEquals(false, info.details().get("fcdependenciesPlugin"));
        assertEquals(true, info.details().get("supportsRunTasks"));
    }

    @Test void versionsNewerThan26_1_2AreSupported() throws Exception {
        assertEquals(true, inspect("minecraftVersion = \"26.2\"").details().get("supportsRunTasks"));
        assertEquals(true, inspect("minecraftVersion = \"26.4-snapshot1\"").details().get("supportsRunTasks"));
        assertEquals(true, inspect("minecraftVersion = \"27.0\"").details().get("supportsRunTasks"));
    }

    @Test void olderVersionsDoNotSupportRunTasks() throws Exception {
        assertEquals(false, inspect("minecraftVersion = \"26.1.1\"").details().get("supportsRunTasks"));
        assertEquals(false, inspect("minecraftVersion = \"26.1\"").details().get("supportsRunTasks"));
        assertEquals(false, inspect("minecraftVersion = \"1.21.4\"").details().get("supportsRunTasks"));
    }

    @Test void missingVersionDoesNotSupportRunTasks() throws Exception {
        ProjectInfo info = inspect("plugins { id(\"com.asbestosstar.fcdependencies\") version \"0.0.1\" }");
        assertEquals("", info.details().get("minecraftVersion"));
        assertEquals(false, info.details().get("supportsRunTasks"));
        assertEquals(true, info.details().get("fcdependenciesPlugin"));
    }

    private ProjectInfo inspect(String buildScript) throws Exception {
        Files.writeString(temp.resolve("build.gradle.kts"), buildScript);
        return new ProjectInspector().inspect(temp);
    }
}
