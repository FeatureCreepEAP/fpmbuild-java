# Gradle support

FPMBuild treats Gradle as a first-class build tool.

Default build:

```sh
fpmbuild build-project --path project --tool gradle
```

This runs `clean build` using `gradlew`/`gradlew.bat` when available.

Custom tasks:

```sh
fpmbuild build-project --path project --tool gradle --task clean --task shadowJar
```

Spec form:

```spec
%build
buildfpm_gradle %{?sources_location}/project --build --task clean --task build
```

Primary JAR scoring prefers ordinary/shaded output over `-plain`, source, Javadoc,
or test JARs. For a multi-project aggregate, FPMBuild can discover JARs below child
`build/libs` directories. For precise control, build the desired subproject and point
`buildfpm_gradle` at that subproject directory.

## FCDependencies Gradle plugin detection

`inspect-project` reports how a Gradle project wires FeatureCreep in `details`:

- `fcdependenciesPlugin`: true when the build applies `com.asbestosstar.fcdependencies`.
- `loomPlugin`: true when the build still applies Fabric Loom (`net.fabricmc.fabric-loom` or `net.fabricmc.loom`). Reported for migration only.
- `minecraftVersion`: the value of `minecraftVersion = "..."` (or `minecraftVersion("...")`) from the root or direct child build scripts, or `""` when absent.
- `supportsRunTasks`: true only when `minecraftVersion` is 26.1.2 or newer. The plugin's `runClient` and `runServer` tasks are available only for these versions.
