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
