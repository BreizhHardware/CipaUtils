@echo off
setlocal enabledelayedexpansion

echo ================================
echo    CipaUtils - Test Server
echo ================================
echo.

:: Variables de configuration
set SERVER_DIR=test-server
set PLUGIN_JAR=CipaUtils-1.4.jar
set PAPER_VERSION=1.21.8
set PAPER_BUILD=60

:: Compilation du plugin avec Gradle
call gradlew.bat clean build
if %errorlevel% neq 0 (
    echo [91mErreur lors de la compilation Gradle ![0m
    pause
    exit /b 1
)
echo [92m✓ Plugin compilé avec succès[0m

echo.
echo [94m[2/4][0m Préparation du serveur de test...
if not exist "%SERVER_DIR%" mkdir "%SERVER_DIR%"
if not exist "%SERVER_DIR%\plugins" mkdir "%SERVER_DIR%\plugins"

:: Copier le plugin compilé
copy /Y "build\libs\%PLUGIN_JAR%" "%SERVER_DIR%\plugins\%PLUGIN_JAR%"
echo [92m✓ Plugin copié dans le dossier plugins du serveur[0m

echo.
echo [94m[3/4][0m Vérification de PaperMC...
if not exist "%SERVER_DIR%\server.jar" (
    echo [93mTéléchargement de PaperMC %PAPER_VERSION%...[0m
    curl -o "%SERVER_DIR%\server.jar" "https://api.papermc.io/v2/projects/paper/versions/%PAPER_VERSION%/builds/%PAPER_BUILD%/downloads/paper-%PAPER_VERSION%-%PAPER_BUILD%.jar"
    if %errorlevel% neq 0 (
        echo [91mErreur lors du téléchargement de PaperMC ![0m
        pause
        exit /b 1
    )
    echo [92m✓ PaperMC téléchargé[0m
) else (
    echo [92m✓ PaperMC déjà présent[0m
)

echo.
echo [94m[4/4][0m Lancement du serveur...
cd "%SERVER_DIR%"
java -jar server.jar
