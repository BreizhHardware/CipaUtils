#!/bin/bash
# bump-version.sh
# Script pour mettre à jour la version du projet dans build.gradle, plugin.yml, start-test-server.bat et start-test-server.sh

set -e

read -p "Enter the new version (e.g. 1.5): " NEW_VERSION

# Mettre à jour build.gradle
sed -i '' "s/^version = .*/version = '${NEW_VERSION}'/" build.gradle
echo "Updated build.gradle to version ${NEW_VERSION}"

# (Optionnel) Mettre à jour settings.gradle si tu veux afficher la version dans le nom du projet
# sed -i '' "s/^rootProject.name = .*/rootProject.name = 'noMoreSpawnProtect'/" settings.gradle

# Mettre à jour plugin.yml
sed -i '' "s/^version: .*/version: ${NEW_VERSION}/" src/main/resources/plugin.yml
echo "Updated plugin.yml to version ${NEW_VERSION}"

# Mettre à jour start-test-server.sh
sed -i '' "s/^PLUGIN_JAR=.*/PLUGIN_JAR=CipaUtils-${NEW_VERSION}.jar/" start-test-server.sh
echo "Updated start-test-server.sh to use CipaUtils-${NEW_VERSION}.jar"

# Mettre à jour start-test-server.bat
sed -i '' "s/^set PLUGIN_JAR=.*/set PLUGIN_JAR=CipaUtils-${NEW_VERSION}.jar/" start-test-server.bat
echo "Updated start-test-server.bat to use CipaUtils-${NEW_VERSION}.jar"

echo "Version bump complete!"
