#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# Gradle 8.14 does not support very new JDKs (e.g. 27), so prefer JDK 21.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    export JAVA_HOME
fi

# Separate jars are built for every supported BungeeCord and Velocity API version.
# BungeeCord API versions >= 1.21 need Java 11, Velocity 3.4 needs Java 17,
# Velocity 3.5 needs Java 21.
BUNGEE_VERSIONS="1.16-R0.1 1.16-R0.2 1.16-R0.3 1.16-R0.4 1.20-R0.1 1.20-R0.2 1.21-R0.1 1.21-R0.2 1.21-R0.3 1.21-R0.4"
VELOCITY_VERSIONS="3.0.0 3.0.1 3.1.0 3.1.1 3.4.0 3.5.1"

VERSION=$(grep -oE "^ *version '[^']+'" build.gradle | head -1 | cut -d"'" -f2)

rm -rf dist
mkdir -p dist

for v in $BUNGEE_VERSIONS; do
    ./gradlew :bungee:shadowJar -PbungeeApiVersion="$v" -q
    cp "bungee/build/libs/bungee-${VERSION}-${v}.jar" "dist/NanoLimbo-Bungee-${VERSION}-${v}.jar"
    echo "built Bungee ${v}"
done

for v in $VELOCITY_VERSIONS; do
    ./gradlew :velocity:shadowJar -PvelocityApiVersion="$v" -q
    cp "velocity/build/libs/velocity-${VERSION}-${v}.jar" "dist/NanoLimbo-Velocity-${VERSION}-${v}.jar"
    echo "built Velocity ${v}"
done

./gradlew :api:shadowJar -q
cp "api/build/libs/api-${VERSION}-all.jar" "dist/NanoLimbo-${VERSION}.jar"

echo
echo "Artifacts in dist/:"
ls -1 dist
