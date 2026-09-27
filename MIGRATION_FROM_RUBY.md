# Migration from Ruby FPMBuild

The Ruby/JRuby runtime is no longer required.

Removed runtime components:

- `fpmbuildmain.rb`
- `spec_parser.rb`
- `jruby-complete-*.jar`

Replaced by the executable `fpmbuild-java` JAR.

## Compatibility retained

- `<root> -ba <spec>` invocation
- `Name`, `Version`, `Release`, `Summary`, `License`, `URL`, `Source0`
- `BuildRequires`, `Requires`
- `%description`, `%prep`, `%build`, `%install`, `%files`, `%changelog`
- `%ifos`, `%else`, `%endif`
- existing `%{?...}` macros
- `buildfpm_maven` and `buildfpm_gradle`
- `.fpm`, `.sfpm`, `BUILD`, `BUILD_ROOT`, `FPMS`, `SFPMS`, and `SOURCES` layout

## Intentional improvements

- safe condition parser instead of evaluating arbitrary Ruby
- nested conditions work predictably
- description parsing reads `%description`, not the `%build` region
- dist is configurable instead of hard-coded to `fc5`
- project artifacts are discovered from Maven/Gradle outputs
- packaging uses the actual built JAR
- source packages omit VCS/build cache directories
- Gradle wrapper, Kotlin DSL, multi-project output, and task support
- machine-readable CLI and JSON-lines agent mode

The historical AssistRemapper helper is not part of the core builder. Remapping belongs
in FCDependencies/project build logic; the future generic post-build modification layer
will use Java 25's Class-File API instead of embedding the old remapper into FPMBuild.
