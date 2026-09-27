package fpmbuild.spec;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Spec(Path path, Map<String, List<String>> headers, Map<String, List<String>> sections) {
    public Spec {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        sections = Collections.unmodifiableMap(new LinkedHashMap<>(sections));
    }

    public String header(String name) {
        List<String> values = headers.getOrDefault(name.toLowerCase(), List.of());
        return values.isEmpty() ? "" : values.getFirst();
    }

    public List<String> headerValues(String name) {
        return headers.getOrDefault(name.toLowerCase(), List.of());
    }

    public List<String> section(String name) {
        return sections.getOrDefault(name.toLowerCase(), List.of());
    }

    public String name() { return header("name"); }
    public String version() { return header("version"); }
    public String release() { return header("release"); }
    public String summary() { return header("summary"); }
    public String license() { return header("license"); }
    public String url() { return header("url"); }
    public String source0() { return header("source0"); }
    public boolean superInjection() {
        String value = header("superinjection").trim().toLowerCase(java.util.Locale.ROOT);
        return value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("1");
    }
    public String description() { return String.join(System.lineSeparator(), section("description")).strip(); }
}
