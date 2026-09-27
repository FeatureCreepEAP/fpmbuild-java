package fpmbuild.spec;

import fpmbuild.util.Os;

import java.util.Locale;
import java.util.Map;

public final class ConditionEvaluator {
    private final Os os;
    private final Map<String, String> env;

    public ConditionEvaluator(Os os, Map<String, String> env) {
        this.os = os;
        this.env = env;
    }

    public boolean evaluate(String expression) {
        if (expression == null || expression.isBlank()) return true;
        String e = expression.strip();
        if (e.toLowerCase(Locale.ROOT).startsWith("ifos ")) return os.matches(e.substring(5).strip());
        if (e.toLowerCase(Locale.ROOT).startsWith("if ")) e = e.substring(3).strip();
        return new BoolParser(e).parse();
    }

    private boolean atom(String token) {
        String t = token.strip();
        if (t.equalsIgnoreCase("true") || t.equals("1")) return true;
        if (t.equalsIgnoreCase("false") || t.equals("0")) return false;
        if (t.regionMatches(true, 0, "os==", 0, 4)) return os.matches(t.substring(4));
        if (t.regionMatches(true, 0, "os=", 0, 3)) return os.matches(t.substring(3));
        if (t.regionMatches(true, 0, "env:", 0, 4)) {
            String rest = t.substring(4);
            int eq = rest.indexOf('=');
            if (eq < 0) return env.containsKey(rest) && !env.getOrDefault(rest, "").isBlank();
            return env.getOrDefault(rest.substring(0, eq), "").equals(rest.substring(eq + 1));
        }
        return false;
    }

    private final class BoolParser {
        private final String text; private int i;
        BoolParser(String text) { this.text = text; }
        boolean parse() { boolean v = or(); ws(); if (i != text.length()) throw new IllegalArgumentException("Invalid condition: " + text); return v; }
        boolean or() { boolean v = and(); while (true) { ws(); if (take("||")) v = and() || v; else return v; } }
        boolean and() { boolean v = not(); while (true) { ws(); if (take("&&")) v = not() && v; else return v; } }
        boolean not() { ws(); if (take("!")) return !not(); return primary(); }
        boolean primary() { ws(); if (take("(")) { boolean v = or(); ws(); if (!take(")")) throw new IllegalArgumentException("Missing ')' in condition: " + text); return v; } int st=i; while(i<text.length()&&!Character.isWhitespace(text.charAt(i))&&"()!&|".indexOf(text.charAt(i))<0)i++; if(st==i)throw new IllegalArgumentException("Expected condition atom: "+text); return atom(text.substring(st,i)); }
        void ws(){while(i<text.length()&&Character.isWhitespace(text.charAt(i)))i++;}
        boolean take(String s){if(text.startsWith(s,i)){i+=s.length();return true;}return false;}
    }
}
