package fpmbuild;

import fpmbuild.build.BuildContext;
import fpmbuild.spec.ConditionEvaluator;
import fpmbuild.spec.MacroExpander;
import fpmbuild.spec.Spec;
import fpmbuild.spec.SpecParser;
import fpmbuild.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpecParserTest {
    @TempDir Path temp;

    @Test void parsesHeadersSectionsConditionsAndMacros() throws Exception {
        Path spec = temp.resolve("x.spec");
        Files.writeString(spec, """
                Name: demo
                Version: 12
                Release: 3.%{?dist}
                Summary: test
                %description
                hello
                %ifos linux
                linux-only
                %else
                other
                %endif
                %build
                echo %{?name} %{?version} %{?sources_location}
                %install
                """);
        Spec parsed = new SpecParser(new ConditionEvaluator(Os.LINUX, Map.of())).parse(spec);
        assertEquals("demo", parsed.name());
        assertTrue(parsed.description().contains("linux-only"));
        assertFalse(parsed.description().contains("other"));
        MacroExpander e = new MacroExpander(parsed, new BuildContext(temp, "fc12"));
        assertEquals("3.fc12", e.value("release"));
        assertTrue(e.expand(parsed.section("build").getFirst()).contains("demo 12"));
    }
}
