#!/bin/bash

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables de configuration
SERVER_DIR="test-server"
PLUGIN_JAR=CipaUtils-1.4.jar
PAPER_VERSION="1.21.8"
PAPER_BUILD="60"

echo "================================"
echo "    CipaUtils - Test Server"
echo "================================"
echo

# Fonction pour afficher les messages avec couleurs
print_step() {
    echo -e "${BLUE}[$1]${NC} $2"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Vérifier que Gradle est installé
if ! command -v ./gradlew &> /dev/null && ! command -v gradle &> /dev/null; then
    print_error "Gradle n'est pas installé ou pas dans le PATH !"
    exit 1
fi

# Vérifier que Java est installé
if ! command -v java &> /dev/null; then
    print_error "Java n'est pas installé ou pas dans le PATH !"
    exit 1
fi

# [1/4] Compilation du plugin avec Gradle
print_step "1/4" "Compilation du plugin avec Gradle..."
./gradlew clean build --no-daemon
if [ $? -ne 0 ]; then
    print_error "Erreur lors de la compilation Gradle !"
    exit 1
fi
print_success "Plugin compilé avec succès"

# [2/4] Préparation du serveur de test
print_step "2/4" "Préparation du serveur de test..."
mkdir -p "$SERVER_DIR/plugins"

# Copier le plugin compilé
cp -f "build/libs/$PLUGIN_JAR" "$SERVER_DIR/plugins/"
print_success "Plugin copié dans le dossier plugins du serveur"

# [3/4] Télécharger PaperMC si nécessaire
if [ ! -f "$SERVER_DIR/server.jar" ]; then
    print_warning "Téléchargement de PaperMC $PAPER_VERSION..."
    curl -o "$SERVER_DIR/server.jar" "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
    if [ $? -ne 0 ]; then
        print_error "Erreur lors du téléchargement de PaperMC !"
        exit 1
    fi
    print_success "PaperMC téléchargé"
else
    print_success "PaperMC déjà présent"
fi

# [4/4] Lancer le serveur
print_step "4/4" "Lancement du serveur..."
cd "$SERVER_DIR"
java -jar server.jar nogui

echo
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Serveur de test CipaUtils démarré !${NC}"
echo -e "${GREEN}========================================${NC}"
echo
echo "Instructions de test :"
echo "- Connectez-vous sur localhost:25565"
echo "- Mode créatif activé pour faciliter les tests"
echo "- Testez /waystone pour voir les commandes"
echo "- Craftez une waystone : Ender Pearl + Diamants + Lodestone + Obsidienne"
echo
echo -e "${YELLOW}Pour arrêter le serveur, tapez 'stop' dans la console${NC}"
echo

# Démarrer le serveur avec gestion des signaux
trap 'echo -e "\n${YELLOW}Arrêt du serveur en cours...${NC}"; exit 0' SIGINT SIGTERM

java -Xmx2G -Xms1G -XX:+UseG1GC -jar server.jar nogui

echo
echo -e "${YELLOW}Serveur arrêté. Retour au répertoire principal...${NC}"
cd ..

# Attendre une entrée utilisateur sur les systèmes interactifs
if [ -t 0 ]; then
    echo
    echo "Appuyez sur Entrée pour continuer..."
    read
fi
