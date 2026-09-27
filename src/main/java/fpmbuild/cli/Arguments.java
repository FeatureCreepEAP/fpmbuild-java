package fpmbuild.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Arguments {
    private final List<String> positionals = new ArrayList<>();
    private final Map<String, List<String>> options = new LinkedHashMap<>();

    public Arguments(String[] args, int from) {
        for (int i = from; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2); String value = "true";
                int eq = key.indexOf('=');
                if (eq >= 0) { value = key.substring(eq + 1); key = key.substring(0, eq); }
                else if (i + 1 < args.length && !args[i + 1].startsWith("--")) value = args[++i];
                options.computeIfAbsent(key, __ -> new ArrayList<>()).add(value);
            } else positionals.add(a);
        }
    }
    public String option(String key, String fallback){List<String> v=options.get(key);return v==null||v.isEmpty()?fallback:v.getLast();}
    public boolean flag(String key){return Boolean.parseBoolean(option(key,"false"));}
    public List<String> values(String key){return options.getOrDefault(key,List.of());}
    public List<String> positionals(){return List.copyOf(positionals);}
}
