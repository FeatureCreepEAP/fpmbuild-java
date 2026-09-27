package fpmbuild.project;

public enum BuildTool {
    MAVEN, GRADLE, UNKNOWN;

    public static BuildTool parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("auto")) return UNKNOWN;
        return valueOf(value.strip().toUpperCase());
    }
}
