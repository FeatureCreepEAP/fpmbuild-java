package fpmbuild.util;

import java.util.Locale;

public enum Os {
    WINDOWS, MACOS, LINUX, SOLARIS, AIX, OTHER;

    public static Os current() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) return WINDOWS;
        if (name.contains("mac") || name.contains("darwin")) return MACOS;
        if (name.contains("linux")) return LINUX;
        if (name.contains("sunos") || name.contains("solaris")) return SOLARIS;
        if (name.contains("aix")) return AIX;
        return OTHER;
    }

    public boolean matches(String value) {
        String v = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return switch (this) {
            case WINDOWS -> v.equals("windows") || v.equals("win");
            case MACOS -> v.equals("macos") || v.equals("mac") || v.equals("darwin") || v.equals("osx");
            case LINUX -> v.equals("linux");
            case SOLARIS -> v.equals("solaris") || v.equals("sunos");
            case AIX -> v.equals("aix");
            case OTHER -> v.equals("other");
        };
    }
}
