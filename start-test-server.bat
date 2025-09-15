@echo off
setlocal enabledelayedexpansion

echo ================================
echo    CipaUtils - Test Server
echo ================================
echo.

:: Variables de configuration
set SERVER_DIR=test-server
set PLUGIN_JAR=CipaUtils-1.3.jar
set PAPER_VERSION=1.21.8
set PAPER_BUILD=60

:: Couleurs (si supportées)
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set BLUE=[94m
set RESET=[0m

echo %BLUE%[1/4]%RESET% Compilation du plugin...
call mvn clean package -q
if %errorlevel% neq 0 (
    echo %RED%Erreur lors de la compilation !%RESET%
    pause
    exit /b 1
)
echo %GREEN%✓ Plugin compilé avec succès%RESET%

echo.
echo %BLUE%[2/4]%RESET% Préparation du serveur de test...

:: Créer le dossier du serveur s'il n'existe pas
if not exist "%SERVER_DIR%" mkdir "%SERVER_DIR%"
if not exist "%SERVER_DIR%\plugins" mkdir "%SERVER_DIR%\plugins"

:: Télécharger PaperMC si nécessaire
if not exist "%SERVER_DIR%\server.jar" (
    echo %YELLOW%Téléchargement de PaperMC %PAPER_VERSION%...%RESET%
    curl -o "%SERVER_DIR%\server.jar" "https://api.papermc.io/v2/projects/paper/versions/%PAPER_VERSION%/builds/%PAPER_BUILD%/downloads/paper-%PAPER_VERSION%-%PAPER_BUILD%.jar"
    if %errorlevel% neq 0 (
        echo %RED%Erreur lors du téléchargement de PaperMC !%RESET%
        pause
        exit /b 1
    )
    echo %GREEN%✓ PaperMC téléchargé%RESET%
) else (
    echo %GREEN%✓ PaperMC déjà présent%RESET%
)

:: Copier le plugin
echo %BLUE%[3/4]%RESET% Installation du plugin...
copy "target\%PLUGIN_JAR%" "%SERVER_DIR%\plugins\%PLUGIN_JAR%" >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%Erreur lors de la copie du plugin !%RESET%
    pause
    exit /b 1
)
echo %GREEN%✓ Plugin installé%RESET%

:: Créer l'EULA si nécessaire
if not exist "%SERVER_DIR%\eula.txt" (
    echo eula=true > "%SERVER_DIR%\eula.txt"
    echo %GREEN%✓ EULA accepté automatiquement%RESET%
)

:: Créer les propriétés du serveur avec des paramètres de test
if not exist "%SERVER_DIR%\server.properties" (
    echo %BLUE%Configuration du serveur de test...%RESET%
    (
        echo # Configuration serveur de test - CipaUtils
        echo server-port=25565
        echo gamemode=creative
        echo difficulty=peaceful
        echo spawn-protection=0
        echo max-players=10
        echo online-mode=false
        echo enable-command-block=true
        echo motd=CipaUtils - Serveur de Test
        echo level-name=test-world
        echo level-type=minecraft:flat
        echo generate-structures=false
        echo spawn-monsters=false
        echo spawn-animals=false
        echo pvp=false
    ) > "%SERVER_DIR%\server.properties"
    echo %GREEN%✓ Configuration créée%RESET%
)

echo.
echo %BLUE%[4/4]%RESET% Démarrage du serveur...
echo %YELLOW%Le serveur va démarrer dans 3 secondes...%RESET%
timeout /t 3 /nobreak >nul

cd "%SERVER_DIR%"
echo.
echo %GREEN%========================================%RESET%
echo %GREEN%  Serveur de test CipaUtils démarré !%RESET%
echo %GREEN%========================================%RESET%
echo.
echo Instructions de test :
echo - Connectez-vous sur localhost:25565
echo - Mode créatif activé pour faciliter les tests
echo - Testez /waystone pour voir les commandes
echo - Craftez une waystone : Ender Pearl + Diamants + Lodestone + Obsidienne
echo.
echo %YELLOW%Pour arrêter le serveur, tapez 'stop' dans la console%RESET%
echo.

java -Xmx2G -Xms1G -XX:+UseG1GC -jar server.jar nogui

echo.
echo %YELLOW%Serveur arrêté. Retour au répertoire principal...%RESET%
cd ..
pause
