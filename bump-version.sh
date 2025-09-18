#!/bin/bash
# bump-version.sh
# Script to update the project version in pom.xml, plugin.yml, start-test-server.bat, and start-test-server.sh

set -e

read -p "Enter the new version (e.g. 1.5): " NEW_VERSION

# Update pom.xml
mvn versions:set -DnewVersion=${NEW_VERSION}
rm -f pom.xml.versionsBackup
echo "Updated pom.xml to version ${NEW_VERSION}"

# Update plugin.yml
sed -i '' "s/^version: .*/version: ${NEW_VERSION}/" src/main/resources/plugin.yml
echo "Updated plugin.yml to version ${NEW_VERSION}"

# Update start-test-server.sh
sed -i '' "s/^PLUGIN_JAR=.*/PLUGIN_JAR=CipaUtils-${NEW_VERSION}.jar/" start-test-server.sh
echo "Updated start-test-server.sh to use CipaUtils-${NEW_VERSION}.jar"

# Update start-test-server.bat
sed -i '' "s/^set PLUGIN_JAR=.*/set PLUGIN_JAR=CipaUtils-${NEW_VERSION}.jar/" start-test-server.bat
echo "Updated start-test-server.bat to use CipaUtils-${NEW_VERSION}.jar"

echo "Version bump complete!"
