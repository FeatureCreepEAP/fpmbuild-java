package fpmbuild.superinject;

import java.util.Locale;

final class ModId {
    private ModId() {}

    static String normalize(String value) {
        String input = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder();
        for (int i=0;i<input.length();i++) {
            char c=input.charAt(i);
            if (c>='a'&&c<='z'||c>='0'&&c<='9'||c=='_') b.append(c);
            else b.append('_');
        }
        while (b.indexOf("__") >= 0) {
            int p=b.indexOf("__"); b.deleteCharAt(p);
        }
        while (!b.isEmpty() && b.charAt(0)=='_') b.deleteCharAt(0);
        if (b.isEmpty() || !Character.isLetter(b.charAt(0))) b.insert(0,"fc_");
        if (b.length() < 2) b.append("_mod");
        if (b.length() > 64) b.setLength(64);
        while (!b.isEmpty() && b.charAt(b.length()-1)=='_') b.setLength(b.length()-1);
        if (b.length() < 2) b.append("fc");
        return b.toString();
    }

    static String javaSegment(String normalized) {
        String id = normalize(normalized);
        return Character.isJavaIdentifierStart(id.charAt(0)) ? id : "m_" + id;
    }
}
