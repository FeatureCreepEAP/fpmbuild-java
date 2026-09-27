package fpmbuild.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object value = p.value();
        p.ws();
        if (!p.end()) throw new IllegalArgumentException("Trailing JSON at offset " + p.i);
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder b = new StringBuilder();
        write(value, b);
        return b.toString();
    }

    private static void write(Object v, StringBuilder b) {
        if (v == null) { b.append("null"); return; }
        if (v instanceof String s) { string(s, b); return; }
        if (v instanceof Number || v instanceof Boolean) { b.append(v); return; }
        if (v instanceof Map<?, ?> m) {
            b.append('{'); boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) b.append(','); first = false;
                string(String.valueOf(e.getKey()), b); b.append(':'); write(e.getValue(), b);
            }
            b.append('}'); return;
        }
        if (v instanceof Iterable<?> it) {
            b.append('['); boolean first = true;
            for (Object e : it) { if (!first) b.append(','); first = false; write(e, b); }
            b.append(']'); return;
        }
        if (v.getClass().isArray()) {
            b.append('['); int len = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < len; i++) { if (i > 0) b.append(','); write(java.lang.reflect.Array.get(v, i), b); }
            b.append(']'); return;
        }
        string(v.toString(), b);
    }

    private static void string(String s, StringBuilder b) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int)c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
    }

    public static Map<String, Object> object(Object... kv) {
        if ((kv.length & 1) != 0) throw new IllegalArgumentException("key/value pairs required");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(String.valueOf(kv[i]), kv[i + 1]);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("JSON object required");
        return (Map<String, Object>) value;
    }

    private static final class Parser {
        final String s; int i;
        Parser(String s) { this.s = s; }
        boolean end() { return i >= s.length(); }
        void ws() { while (!end() && Character.isWhitespace(s.charAt(i))) i++; }
        Object value() {
            ws(); if (end()) throw err("Expected value"); char c = s.charAt(i);
            return switch (c) {
                case '{' -> object(); case '[' -> array(); case '"' -> str();
                case 't' -> lit("true", true); case 'f' -> lit("false", false); case 'n' -> lit("null", null);
                default -> { if (c == '-' || Character.isDigit(c)) yield number(); throw err("Unexpected '" + c + "'"); }
            };
        }
        Map<String, Object> object() {
            i++; Map<String, Object> out = new LinkedHashMap<>(); ws(); if (!end() && s.charAt(i)=='}') {i++; return out;}
            while (true) { ws(); if (end() || s.charAt(i)!='"') throw err("Object key required"); String k=str(); ws(); expect(':'); out.put(k,value()); ws(); if (take('}')) return out; expect(','); }
        }
        List<Object> array() {
            i++; List<Object> out = new ArrayList<>(); ws(); if (take(']')) return out;
            while (true) { out.add(value()); ws(); if (take(']')) return out; expect(','); }
        }
        String str() {
            expect('"'); StringBuilder b=new StringBuilder();
            while (!end()) { char c=s.charAt(i++); if (c=='"') return b.toString(); if (c!='\\') { b.append(c); continue; }
                if (end()) throw err("Bad escape"); char e=s.charAt(i++); switch(e) {
                    case '"','\\','/' -> b.append(e); case 'b'->b.append('\b'); case 'f'->b.append('\f'); case 'n'->b.append('\n'); case 'r'->b.append('\r'); case 't'->b.append('\t');
                    case 'u' -> { if (i+4>s.length()) throw err("Bad unicode escape"); b.append((char)Integer.parseInt(s.substring(i,i+4),16)); i+=4; }
                    default -> throw err("Bad escape"); }
            } throw err("Unterminated string");
        }
        Object lit(String word,Object value){ if(!s.startsWith(word,i))throw err("Expected "+word);i+=word.length();return value; }
        Number number(){ int st=i; if(s.charAt(i)=='-')i++; while(!end()&&Character.isDigit(s.charAt(i)))i++; boolean fp=false; if(!end()&&s.charAt(i)=='.'){fp=true;i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;} if(!end()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){fp=true;i++;if(!end()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;} String n=s.substring(st,i); if (fp) return Double.valueOf(n); return Long.valueOf(n); }
        boolean take(char c){ ws(); if(!end()&&s.charAt(i)==c){i++;return true;}return false; }
        void expect(char c){ ws(); if(end()||s.charAt(i)!=c)throw err("Expected '"+c+"'");i++; }
        IllegalArgumentException err(String m){ return new IllegalArgumentException(m+" at offset "+i); }
    }
}
