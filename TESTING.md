# Testing

FPMBuild requires JDK 25 for the production Maven build.

```sh
mvn clean test
```

The JUnit 5 suite covers spec parsing/macros/conditions, Maven and Gradle project discovery,
primary-JAR selection, JSON protocol behavior, and FPM/SFPM packaging.

For protocol smoke testing:

```sh
printf '%s\n' '{"id":1,"command":"capabilities"}' | java -jar target/fpmbuild-java-0.0.1-SNAPSHOT.jar agent --stdio
```

For Gradle projects, tests should include both Groovy and Kotlin DSL projects and at least one
multi-project layout with the wrapper in the parent directory.

## SuperInjection tests

`SuperInjectionTest` covers:

- explicit `FeatureCreep-Mod-Id` with multiple Mixin configs;
- fallback to the JBoss `<module name="...">` when `FeatureCreep-Mod-Id` is absent;
- `module.json` module-name fallback;
- no-op behavior when no Mixin config exists;
- generated NeoForge and Forge `@Mod` bridge classes;
- NeoForge mixin metadata;
- Forge/Sponge/Fabric discovery metadata;
- `MixinConfigs` manifest injection;
- preservation of the original FeatureCreep module descriptor;
- absence of Fabric entrypoints.

Run on JDK 25+:

```bash
mvn clean test
```

## SuperInjection ownership regression

`SuperInjectionTest` verifies that an injected JAR contains `META-INF/featurecreep/not-superloader-owned` and that the marker identifies the compatibility metadata as FeatureCreep-owned. The FeatureCreep Loader integration suite separately verifies that this marker overrides superloader classification while an unmarked foreign superloader JAR is still skipped.
