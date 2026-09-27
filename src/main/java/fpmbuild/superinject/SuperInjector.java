package fpmbuild.superinject;

import fpmbuild.spec.Spec;
import fpmbuild.util.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/** Post-build compatibility injection for superloaders which need to discover FC Mixin configs. */
public final class SuperInjector {
    public record Result(
            boolean requested,
            boolean applied,
            Path jar,
            String moduleEntry,
            String originalModId,
            String injectedModId,
            String modIdSource,
            List<String> mixinConfigs,
            List<String> injectedEntries,
            List<String> strippedSignatures,
            String beforeSha256,
            String afterSha256,
            String reason) {
        public Map<String,Object> toMap() {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("requested", requested);
            m.put("applied", applied);
            m.put("jar", jar == null ? null : jar.toString());
            m.put("moduleEntry", moduleEntry);
            m.put("originalModId", originalModId);
            m.put("injectedModId", injectedModId);
            m.put("modIdSource", modIdSource);
            m.put("mixinConfigs", mixinConfigs);
            m.put("injectedEntries", injectedEntries);
            m.put("strippedSignatures", strippedSignatures);
            m.put("beforeSha256", beforeSha256);
            m.put("afterSha256", afterSha256);
            m.put("reason", reason);
            return m;
        }
    }

    private static final String NEO_TOML = "META-INF/neoforge.mods.toml";
    private static final String FORGE_TOML = "META-INF/mods.toml";
    private static final String SPONGE_JSON = "META-INF/sponge_plugins.json";
    private static final String FABRIC_JSON = "fabric.mod.json";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";
    /**
     * Ownership marker consumed by FeatureCreep Loader. Superloader metadata in
     * this JAR exists only so the surrounding loader can discover Sponge Mixin
     * configs; the JAR itself is still owned and loaded by FeatureCreep.
     */
    public static final String FEATURECREEP_OWNERSHIP_MARKER = "META-INF/featurecreep/not-superloader-owned";

    public Result inject(Path jar, Spec spec) throws Exception {
        if (jar == null || !Files.isRegularFile(jar)) throw new IOException("SuperInjection JAR does not exist: " + jar);
        String before = Hashing.sha256(jar);
        ModuleMetadata module = discover(jar);
        if (module == null) {
            return skipped(jar, before, "No module.xml or module.json found in final JAR");
        }
        if (!module.injectable()) {
            String reason = module.originalModId().isBlank()
                    ? "module descriptor has neither FeatureCreep-Mod-Id nor a JBoss module name"
                    : "module descriptor has no MixinConfigs/SpongeMixinConfig/FeatureCreep-Mixin-Configs";
            return new Result(true,false,jar,module.moduleEntry(),module.originalModId(),module.injectedModId(),module.modIdSource(),module.mixinConfigs(),List.of(),List.of(),before,before,reason);
        }

        BridgeClassGenerator generator = new BridgeClassGenerator();
        BridgeClassGenerator.Generated neo = generator.neoForge(module.injectedModId());
        BridgeClassGenerator.Generated forge = generator.forge(module.injectedModId());
        BridgeClassGenerator.Generated sponge = generator.spongeEntrypoint(module.injectedModId());

        Map<String,byte[]> injected = new LinkedHashMap<>();
        injected.put(NEO_TOML, SuperInjectionMetadata.neoForge(module, spec.version()));
        injected.put(FORGE_TOML, SuperInjectionMetadata.forge(module, spec.version()));
        injected.put(SPONGE_JSON, SuperInjectionMetadata.sponge(module, spec.version(), sponge.className()));
        injected.put(FABRIC_JSON, SuperInjectionMetadata.fabric(module, spec.version()));
        injected.put(FEATURECREEP_OWNERSHIP_MARKER, ownershipMarker(module));
        injected.put(BridgeClassGenerator.entryName(neo.className()), neo.bytes());
        injected.put(BridgeClassGenerator.entryName(forge.className()), forge.bytes());
        injected.put(BridgeClassGenerator.entryName(sponge.className()), sponge.bytes());

        Manifest manifest = readManifest(jar);
        Attributes main = manifest.getMainAttributes();
        if (main.getValue(Attributes.Name.MANIFEST_VERSION) == null) main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        main.putValue("MixinConfigs", SuperInjectionMetadata.mixinManifestValue(module));

        Path parent = jar.toAbsolutePath().normalize().getParent();
        Path tmp = Files.createTempFile(parent, jar.getFileName().toString(), ".superinject.tmp");
        List<String> stripped = new ArrayList<>();
        try {
            rewrite(jar, tmp, manifest, injected, stripped);
            try {
                Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
        String after = Hashing.sha256(jar);
        return new Result(true,true,jar,module.moduleEntry(),module.originalModId(),module.injectedModId(),module.modIdSource(),module.mixinConfigs(),List.copyOf(injected.keySet()),List.copyOf(stripped),before,after,"Injected superloader Mixin discovery metadata after build");
    }

    private static byte[] ownershipMarker(ModuleMetadata module) {
        String text = "format=1\n"
                + "owner=featurecreep\n"
                + "superloaderMetadata=compatibility-only\n"
                + "modid=" + module.injectedModId() + "\n"
                + "moduleDescriptor=" + module.moduleEntry() + "\n";
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static Result skipped(Path jar, String sha, String reason) {
        return new Result(true,false,jar,null,"","","",List.of(),List.of(),List.of(),sha,sha,reason);
    }

    private static ModuleMetadata discover(Path jar) throws Exception {
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry directXml = jf.getJarEntry("module.xml");
            if (directXml != null) return readDescriptor(jf, directXml);
            JarEntry directJson = jf.getJarEntry("module.json");
            if (directJson != null) return readDescriptor(jf, directJson);

            List<JarEntry> candidates = new ArrayList<>();
            Enumeration<JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String lower = entry.getName().toLowerCase(Locale.ROOT);
                if (!entry.isDirectory() && (lower.endsWith("/module.xml") || lower.endsWith("/module.json"))) candidates.add(entry);
            }
            if (candidates.isEmpty()) return null;
            if (candidates.size() > 1) throw new IOException("SuperInjection found multiple module descriptors and no root module.xml/module.json in " + jar + ": " + candidates.stream().map(JarEntry::getName).toList());
            return readDescriptor(jf, candidates.getFirst());
        }
    }

    private static ModuleMetadata readDescriptor(JarFile jar, JarEntry entry) throws Exception {
        byte[] bytes = readAll(jar, entry);
        return entry.getName().toLowerCase(Locale.ROOT).endsWith(".json")
                ? new ModuleJsonReader().read(entry.getName(), bytes)
                : new ModuleXmlReader().read(entry.getName(), bytes);
    }

    private static byte[] readAll(JarFile jar, JarEntry entry) throws IOException {
        try (InputStream in = jar.getInputStream(entry); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out); return out.toByteArray();
        }
    }

    private static Manifest readManifest(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            Manifest existing = jf.getManifest();
            return existing == null ? new Manifest() : new Manifest(existing);
        }
    }

    private static void rewrite(Path input, Path output, Manifest manifest, Map<String,byte[]> injected, List<String> stripped) throws IOException {
        Set<String> replace = new HashSet<>(injected.keySet());
        replace.add(MANIFEST);
        try (JarFile jf = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(output), manifest)) {
            byte[] buffer = new byte[64 * 1024];
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry old = entries.nextElement();
                String name = old.getName();
                if (replace.contains(name)) continue;
                if (isSignatureSidecar(name)) { stripped.add(name); continue; }
                JarEntry copy = new JarEntry(name);
                if (old.getTime() >= 0) copy.setTime(old.getTime());
                if (old.getComment() != null) copy.setComment(old.getComment());
                if (old.getExtra() != null) copy.setExtra(old.getExtra());
                out.putNextEntry(copy);
                if (!old.isDirectory()) {
                    try (InputStream in = jf.getInputStream(old)) {
                        for (int n; (n = in.read(buffer)) >= 0;) if (n > 0) out.write(buffer,0,n);
                    }
                }
                out.closeEntry();
            }
            for (var e : injected.entrySet()) {
                JarEntry add = new JarEntry(e.getKey());
                add.setTime(System.currentTimeMillis());
                out.putNextEntry(add);
                out.write(e.getValue());
                out.closeEntry();
            }
        }
    }

    private static boolean isSignatureSidecar(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!upper.startsWith("META-INF/")) return false;
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC") || upper.startsWith("META-INF/SIG-");
    }
}
