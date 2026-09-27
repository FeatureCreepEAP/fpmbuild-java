package fpmbuild.spec;

import fpmbuild.build.BuildContext;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MacroExpander {
    private final Map<String, String> values = new LinkedHashMap<>();

    public MacroExpander(Spec spec, BuildContext context) {
        put("dist", context.dist());
        put("name", spec.name());
        put("version", spec.version());
        put("release", rawRelease(spec, context));
        put("summary", spec.summary());
        put("license", spec.license());
        put("licence", spec.license());
        put("url", spec.url());
        put("source0", spec.source0());
        put("build_requires", String.join(",", spec.headerValues("buildrequires")));
        put("requires", String.join(",", spec.headerValues("requires")));
        put("description", spec.description());
        put("fpmbuild_location", context.root().toString());
        put("sources_location", context.sources().toString());
        put("build_root", context.buildRoot().toString());
        put("fpm_dir", context.fpms().toString());
        put("sfpm_dir", context.sfpms().toString());
    }

    private static String rawRelease(Spec spec, BuildContext context) {
        return spec.release().replace("%{?dist}", context.dist()).replace("%{dist}", context.dist());
    }

    private void put(String key, String value) { values.put(key, value == null ? "" : value); }

    public String expand(String input) {
        String out = SpecParser.stripComment(input == null ? "" : input).strip();
        for (var e : values.entrySet()) {
            out = out.replace("%{?" + e.getKey() + "}", e.getValue());
            out = out.replace("%{" + e.getKey() + "}", e.getValue());
        }
        out = out.replace("$FPM_BUILD_ROOT", values.get("build_root"));
        return out;
    }

    public String value(String key) { return values.getOrDefault(key, ""); }
}
