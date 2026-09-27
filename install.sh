#!/bin/sh
set -eu
ROOT=${1:-.}
JAR=target/fpmbuild-java-0.0.1-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then echo "Build first: mvn clean package" >&2; exit 1; fi
mkdir -p "$ROOT/.fpmbuild"
cp "$JAR" "$ROOT/.fpmbuild/fpmbuild-java.jar"
cat > "$ROOT/.fpmbuild/fpmbuild" <<'SH'
#!/bin/sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -jar "$DIR/fpmbuild-java.jar" "$@"
SH
chmod +x "$ROOT/.fpmbuild/fpmbuild"
echo "Installed Java FPMBuild into $ROOT/.fpmbuild"
