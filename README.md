# FPMBuild Java

FPMBuild Java is the Java 25 replacement for the old Ruby/JRuby FPMBuild implementation.
There is no JRuby runtime and no Ruby spec parser in the new tool.

## Requirements

- Java 25+
- Maven or Gradle only when the project being built needs it
- Maven/Gradle wrappers are preferred automatically when present

Build FPMBuild itself with:

```sh
mvn clean package
```

Run it with:

```sh
java -jar target/fpmbuild-java-0.0.1-SNAPSHOT.jar help
```

or use `bin/fpmbuild` / `bin/fpmbuild.cmd`.

## Ruby compatibility

The old invocation remains accepted:

```sh
fpmbuild <fpmbuild-root> -ba SPECS/package.spec
```

The following legacy spec commands remain supported:

```text
buildfpm_maven <project>
buildfpm_gradle <project>
```

The new generic form is:

```text
buildfpm <project>
```

It auto-detects Maven or Gradle.

Unlike the old Ruby implementation, the Java implementation does not hard-code `fc5`.
Use `--dist`, `FPMBUILD_DIST`, or the current default `fc12`.

## Modern CLI

```sh
fpmbuild capabilities --json
fpmbuild inspect-spec --spec SPECS/foo.spec --root . --json
fpmbuild inspect-project --path SOURCES/my-mod --json
fpmbuild build-project --path SOURCES/my-mod --json
fpmbuild artifacts --path SOURCES/my-mod --json
fpmbuild inspect-jar --path SOURCES/my-mod --json
fpmbuild bytecode-context --path SOURCES/my-mod --json
fpmbuild build --root . --spec SPECS/foo.spec --json
```

### Building from a spec

The old style still works:

```spec
%build
cd %{?sources_location}/my-mod
mvn clean package
cd /dev/null
buildfpm_maven %{?sources_location}/my-mod
```

The Java builder also allows the build directive itself to build the project:

```spec
%build
buildfpm %{?sources_location}/my-mod --build
```

For Gradle, an explicit task can be supplied:

```spec
%build
buildfpm_gradle %{?sources_location}/my-mod --build --task clean --task shadowJar
```

## Better Gradle support

The Java implementation:

- detects `build.gradle` and `build.gradle.kts`
- recognizes Gradle multi-project settings files
- searches upward for `gradlew` / `gradlew.bat`
- prefers the Gradle wrapper over a system Gradle
- supports arbitrary `--task` arguments
- scans `build/libs` for actual produced JARs
- can find subproject JARs for aggregate projects
- deprioritizes `-plain.jar`
- excludes sources, javadocs, and test JARs from primary-artifact selection
- detects common Shadow and Loom build markers
- detects projects that reference FCDependencies

The package is created from the selected built JAR rather than assuming that
`build/classes/java/main` is the final product.

## Maven support

Maven projects are read from `pom.xml`, including group/artifact/version,
compiler release, packaging, wrapper presence, output JARs, and FCDependencies usage.
Maven wrapper scripts are preferred when present.

## AI-agent CLI

Run:

```sh
fpmbuild agent --stdio
```

The protocol is JSON Lines: one request object per line, one response object per line.
Build logs are written to stderr so stdout remains machine-readable.

Example request:

```json
{"id":1,"command":"inspect-project","params":{"path":"/work/mod"}}
```

Example response:

```json
{"id":1,"ok":true,"result":{"tool":"gradle","primaryArtifact":"/work/mod/build/libs/mod.jar"}}
```

`args` can also be supplied as a raw CLI-argument array:

```json
{"id":2,"command":"build-project","args":["--path","/work/mod","--task","build"]}
```

Useful agent commands are `capabilities`, `inspect-spec`, `inspect-project`,
`build-project`, `artifacts`, `inspect-jar`, `bytecode-context`, `build`, and `shutdown`.

## Future Java 25 Class-File API integration

FPMBuild now treats the Maven/Gradle project and its primary output JAR as first-class
objects. `bytecode-context` resolves the exact JAR, reports its SHA-256, class count,
class-file versions, FCDependencies usage, and whether `java.lang.classfile` is available.

No JAR mutation is performed yet. The intended future implementation is Java's
`java.lang.classfile` API with a copy-on-write workflow:

1. inspect the project and select the real built JAR;
2. record the input SHA-256;
3. create a transformed temporary JAR;
4. validate it;
5. atomically replace or publish it only if the original hash still matches.

This prevents an AI agent from accidentally editing a stale or unrelated artifact.

## Tests

JUnit 5 tests cover:

- spec parsing, `%ifos`, sections, and macro expansion;
- Maven project/JAR discovery;
- Gradle wrapper and artifact selection;
- JSON agent protocol primitives;
- FPM/SFPM packaging from a built project artifact;
- machine-readable capabilities.

Run on JDK 25 with:

```sh
mvn clean test
```

## SuperInjection

`SuperInjection` is an opt-in spec header for post-build Minecraft superloader compatibility:

```spec
SuperInjection: true
```

It runs after Maven/Gradle has produced the selected primary JAR and before FPMBuild packages that JAR. It never changes source files or the FeatureCreep `module.xml`/`module.json`.

Injection only occurs when the final JAR contains both:

1. a Mixin config declaration (`MixinConfigs`, `SpongeMixinConfig`, or `FeatureCreep-Mixin-Configs`), and
2. a mod ID. `FeatureCreep-Mod-Id` takes priority; if it is absent, the JBoss module descriptor `name` is used as the mod ID.

`module.xml` is preferred. `module.json` is also accepted for compatibility.

When enabled and eligible, the final JAR receives:

- `META-INF/neoforge.mods.toml`, including the mod ID and `[[mixins]]` entries.
- An empty Java class annotated with `net.neoforged.fml.common.Mod("<modid>")`.
- `META-INF/mods.toml` for MinecraftForge.
- An empty Java class annotated with `net.minecraftforge.fml.common.Mod("<modid>")`.
- `META-INF/sponge_plugins.json` with a generated empty entrypoint class.
- `MixinConfigs: ...` in `META-INF/MANIFEST.MF`.
- `fabric.mod.json` with ID/version/mixins and **no Fabric entrypoints**.

Generated bridge classes are built with the Java 25 Class-File API. The final JAR is rewritten atomically. Existing JAR signing sidecars under `META-INF` are removed because post-build mutation invalidates those signatures.

Direct command for testing a built JAR:

```bash
fpmbuild superinject --spec SPECS/my-mod.spec --jar build/libs/my-mod.jar --json
```

## SuperInjection ownership marker

When `SuperInjection: true` actually injects superloader compatibility metadata, FPMBuild also writes:

```text
META-INF/featurecreep/not-superloader-owned
```

The file marks the JAR as **FeatureCreep-owned**. Fabric/Forge/NeoForge/Sponge metadata in the same JAR exists only to expose Sponge Mixin configuration to those superloaders. FeatureCreep Loader v12 checks this marker before `GameProvider.isSuperLoaderModZip(...)`, so the JAR is still discovered and loaded as a JBoss module.

The marker currently contains a small UTF-8 properties-style payload with the format version, owner, compatibility-only purpose, injected mod ID, and source module descriptor.
