package fpmbuild;

import fpmbuild.json.Json;
import fpmbuild.spec.Spec;
import fpmbuild.superinject.SuperInjector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

final class SuperInjectionTest {
    private static final Spec SPEC = new Spec(Path.of("test.spec"),
            Map.of("version", List.of("12"), "superinjection", List.of("true")), Map.of());

    @Test
    void injectsAllSuperloaderMetadataFromExplicitFeatureCreepId() throws Exception {
        Path jar = jarWith("module.xml", """
                <module name="fallback.module" xmlns="urn:jboss:module:1.9">
                  <properties>
                    <property name="FeatureCreep-Mod-Id" value="actual-mod"/>
                    <property name="MixinConfigs" value="a.mixins.json,b.mixins.json"/>
                  </properties>
                </module>
                """);

        SuperInjector.Result result = new SuperInjector().inject(jar, SPEC);
        assertTrue(result.applied());
        assertEquals("actual-mod", result.originalModId());
        assertEquals("actual_mod", result.injectedModId());
        assertEquals("FeatureCreep-Mod-Id", result.modIdSource());
        assertEquals(List.of("a.mixins.json", "b.mixins.json"), result.mixinConfigs());

        try (JarFile jf = new JarFile(jar.toFile())) {
            assertNotNull(jf.getJarEntry("module.xml"));
            assertNotNull(jf.getJarEntry("META-INF/neoforge.mods.toml"));
            assertNotNull(jf.getJarEntry("META-INF/mods.toml"));
            assertNotNull(jf.getJarEntry("META-INF/sponge_plugins.json"));
            assertNotNull(jf.getJarEntry("fabric.mod.json"));
            assertNotNull(jf.getJarEntry("META-INF/featurecreep/not-superloader-owned"));
            String ownership = text(jf, "META-INF/featurecreep/not-superloader-owned");
            assertTrue(ownership.contains("owner=featurecreep"));
            assertTrue(ownership.contains("superloaderMetadata=compatibility-only"));
            assertTrue(ownership.contains("modid=actual_mod"));

            String fabric = text(jf, "fabric.mod.json");
            Map<String,Object> fabricJson = Json.asObject(Json.parse(fabric));
            assertEquals("actual_mod", fabricJson.get("id"));
            assertEquals(List.of("a.mixins.json", "b.mixins.json"), fabricJson.get("mixins"));
            assertFalse(fabricJson.containsKey("entrypoints"));

            String neo = text(jf, "META-INF/neoforge.mods.toml");
            assertTrue(neo.contains("modId=\"actual_mod\""));
            assertTrue(neo.contains("config=\"a.mixins.json\""));
            assertTrue(neo.contains("config=\"b.mixins.json\""));

            String forge = text(jf, "META-INF/mods.toml");
            assertTrue(forge.contains("modId=\"actual_mod\""));

            Manifest mf = jf.getManifest();
            assertNotNull(mf);
            assertEquals("a.mixins.json,b.mixins.json", mf.getMainAttributes().getValue("MixinConfigs"));

            String sponge = text(jf, "META-INF/sponge_plugins.json");
            assertTrue(sponge.contains("\"id\":\"actual_mod\""));

            byte[] neoClass = bytes(jf, "featurecreep/superinjection/actual_mod/NeoForgeBridge.class");
            byte[] forgeClass = bytes(jf, "featurecreep/superinjection/actual_mod/ForgeBridge.class");
            byte[] spongeClass = bytes(jf, "featurecreep/superinjection/actual_mod/SpongeBridge.class");
            assertTrue(latin1(neoClass).contains("net/neoforged/fml/common/Mod"));
            assertTrue(latin1(forgeClass).contains("net/minecraftforge/fml/common/Mod"));
            assertFalse(latin1(spongeClass).contains("org/spongepowered"));
        }
    }

    @Test
    void usesJbossModuleNameWhenFeatureCreepModIdIsAbsent() throws Exception {
        Path jar = jarWith("module.xml", """
                <module name="example.cool-mod" xmlns="urn:jboss:module:1.9">
                  <properties>
                    <property name="MixinConfigs" value="cool.mixins.json"/>
                  </properties>
                </module>
                """);

        SuperInjector.Result result = new SuperInjector().inject(jar, SPEC);
        assertTrue(result.applied());
        assertEquals("example.cool-mod", result.originalModId());
        assertEquals("example_cool_mod", result.injectedModId());
        assertEquals("module-name", result.modIdSource());
    }

    @Test
    void moduleJsonNameAlsoCountsAsModId() throws Exception {
        Path jar = jarWith("module.json", """
                {
                  "name":"json.example.mod",
                  "properties": {
                    "MixinConfigs":["json.mixins.json"]
                  }
                }
                """);

        SuperInjector.Result result = new SuperInjector().inject(jar, SPEC);
        assertTrue(result.applied());
        assertEquals("json.example.mod", result.originalModId());
        assertEquals("json_example_mod", result.injectedModId());
        assertEquals("module-name", result.modIdSource());
    }

    @Test
    void doesNothingWithoutMixinConfig() throws Exception {
        Path jar = jarWith("module.xml", "<module name=\"example.mod\" xmlns=\"urn:jboss:module:1.9\"/>");
        String before = sha(jar);
        SuperInjector.Result result = new SuperInjector().inject(jar, SPEC);
        assertFalse(result.applied());
        assertTrue(result.reason().contains("no MixinConfigs"));
        assertEquals(before, sha(jar));
    }

    private static Path jarWith(String descriptorName, String descriptor) throws IOException {
        Path jar = Files.createTempFile("fpmbuild-superinject-", ".jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), mf)) {
            out.putNextEntry(new JarEntry(descriptorName));
            out.write(descriptor.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new JarEntry("keep.txt"));
            out.write("keep".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private static String text(JarFile jf, String name) throws IOException {
        return new String(bytes(jf, name), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(JarFile jf, String name) throws IOException {
        JarEntry entry = jf.getJarEntry(name);
        assertNotNull(entry, "missing JAR entry " + name);
        try (InputStream in = jf.getInputStream(entry)) { return in.readAllBytes(); }
    }

    private static String latin1(byte[] bytes) { return new String(bytes, StandardCharsets.ISO_8859_1); }
    private static String sha(Path file) throws Exception { return fpmbuild.util.Hashing.sha256(file); }
}
