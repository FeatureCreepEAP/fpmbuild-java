package fpmbuild.spec;

import fpmbuild.util.Os;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpecParser {
    private static final List<String> SECTIONS = List.of("description", "prep", "build", "install", "files", "changelog");

    private final ConditionEvaluator conditions;

    public SpecParser() { this(new ConditionEvaluator(Os.current(), System.getenv())); }
    public SpecParser(ConditionEvaluator conditions) { this.conditions = conditions; }

    public Spec parse(Path path) throws IOException {
        List<String> lines = preprocess(Files.readAllLines(path, StandardCharsets.UTF_8));
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String section = null;
        for (String raw : lines) {
            String trimmed = raw.strip();
            if (trimmed.startsWith("%") && SECTIONS.contains(trimmed.substring(1).toLowerCase(Locale.ROOT))) {
                section = trimmed.substring(1).toLowerCase(Locale.ROOT);
                sections.computeIfAbsent(section, __ -> new ArrayList<>());
                continue;
            }
            if (section != null) {
                sections.get(section).add(raw);
                continue;
            }
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue;
            int colon = raw.indexOf(':');
            if (colon > 0) {
                String key = raw.substring(0, colon).strip().toLowerCase(Locale.ROOT);
                String value = stripComment(raw.substring(colon + 1)).strip();
                headers.computeIfAbsent(key, __ -> new ArrayList<>()).add(value);
            }
        }
        Spec spec = new Spec(path.toAbsolutePath().normalize(), headers, sections);
        if (spec.name().isBlank()) throw new IllegalArgumentException("Spec is missing Name: " + path);
        if (spec.version().isBlank()) throw new IllegalArgumentException("Spec is missing Version: " + path);
        return spec;
    }

    private List<String> preprocess(List<String> lines) {
        record Frame(boolean parentActive, boolean condition, boolean inElse) { Frame withElse(){return new Frame(parentActive,condition,true);} boolean active(){return parentActive && (inElse ? !condition : condition);} }
        Deque<Frame> stack = new ArrayDeque<>();
        List<String> out = new ArrayList<>();
        boolean active = true;
        for (String line : lines) {
            String t = line.strip();
            if (t.matches("(?i)^%\\s*if(?:os)?\\s+.*$")) {
                String expr = t.replaceFirst("(?i)^%\\s*", "");
                boolean cond = conditions.evaluate(expr);
                Frame f = new Frame(active, cond, false); stack.push(f); active = f.active(); continue;
            }
            if (t.matches("(?i)^%\\s*else\\s*$")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("%else without %if");
                Frame f = stack.pop().withElse(); stack.push(f); active = f.active(); continue;
            }
            if (t.matches("(?i)^%\\s*endif\\s*$")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("%endif without %if");
                stack.pop(); active = stack.isEmpty() ? true : stack.peek().active(); continue;
            }
            if (active) out.add(line);
        }
        if (!stack.isEmpty()) throw new IllegalArgumentException("Unclosed %if in spec");
        return out;
    }

    static String stripComment(String s) {
        boolean single=false,dbl=false;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(c=='\''&&!dbl)single=!single; else if(c=='"'&&!single)dbl=!dbl; else if(c=='#'&&!single&&!dbl)return s.substring(0,i);
        }
        return s;
    }
}
