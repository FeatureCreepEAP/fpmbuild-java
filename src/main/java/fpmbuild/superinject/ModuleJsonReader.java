package fpmbuild.superinject;

import fpmbuild.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Compatibility reader for projects which carry FeatureCreep/JBoss-style metadata in module.json. */
final class ModuleJsonReader {
    private static final List<String> MIXIN_KEYS = List.of("MixinConfigs", "SpongeMixinConfig", "FeatureCreep-Mixin-Configs", "mixinConfigs", "mixins");

    ModuleMetadata read(String entryName, byte[] bytes) {
        Map<String,Object> root = Json.asObject(Json.parse(new String(bytes, StandardCharsets.UTF_8)));
        Map<String,Object> flattened = new LinkedHashMap<>(root);
        Object properties = getIgnoreCase(root, "properties");
        if (properties instanceof Map<?,?> map) {
            for (var e : map.entrySet()) flattened.put(String.valueOf(e.getKey()), e.getValue());
        }

        String moduleName = firstString(flattened, "name", "moduleName", "module");
        String explicitModId = firstString(flattened, "FeatureCreep-Mod-Id", "modId", "modid");
        String chosenModId = !explicitModId.isBlank() ? explicitModId : moduleName;
        String source = !explicitModId.isBlank() ? "FeatureCreep-Mod-Id" : !moduleName.isBlank() ? "module-name" : "";

        Set<String> mixins = new LinkedHashSet<>();
        for (String key : MIXIN_KEYS) collectMixins(getIgnoreCase(flattened, key), mixins);
        return new ModuleMetadata(entryName, moduleName, chosenModId, ModId.normalize(chosenModId), source, new ArrayList<>(mixins));
    }

    private static Object getIgnoreCase(Map<String,?> map, String key) {
        Object direct = map.get(key);
        if (direct != null) return direct;
        String lower = key.toLowerCase(Locale.ROOT);
        for (var e : map.entrySet()) if (e.getKey().toLowerCase(Locale.ROOT).equals(lower)) return e.getValue();
        return null;
    }

    private static String firstString(Map<String,?> map, String... keys) {
        for (String key : keys) {
            Object value = getIgnoreCase(map, key);
            if (value instanceof String s && !s.isBlank()) return s.trim();
        }
        return "";
    }

    private static void collectMixins(Object value, Set<String> out) {
        if (value == null) return;
        if (value instanceof String s) {
            for (String part : s.split("[,;\\s]+")) if (!part.isBlank()) out.add(part.trim());
            return;
        }
        if (value instanceof Iterable<?> it) {
            for (Object item : it) {
                if (item instanceof String s && !s.isBlank()) out.add(s.trim());
                else if (item instanceof Map<?,?> map) {
                    Object config = null;
                    for (var e : map.entrySet()) if ("config".equalsIgnoreCase(String.valueOf(e.getKey()))) config = e.getValue();
                    if (config instanceof String s && !s.isBlank()) out.add(s.trim());
                }
            }
        }
    }
}
