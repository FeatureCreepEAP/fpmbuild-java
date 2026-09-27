package fpmbuild.util;

import java.util.ArrayList;
import java.util.List;

public final class ShellWords {
    private ShellWords() {}

    public static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false, dbl = false, escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaped) { current.append(c); escaped = false; continue; }
            if (c == '\\' && !single) { escaped = true; continue; }
            if (c == '\'' && !dbl) { single = !single; continue; }
            if (c == '"' && !single) { dbl = !dbl; continue; }
            if (Character.isWhitespace(c) && !single && !dbl) {
                if (!current.isEmpty()) { out.add(current.toString()); current.setLength(0); }
            } else current.append(c);
        }
        if (escaped || single || dbl) throw new IllegalArgumentException("Unterminated shell quoting: " + line);
        if (!current.isEmpty()) out.add(current.toString());
        return out;
    }
}
