#!/bin/bash

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables de configuration
SERVER_DIR="test-server"
PLUGIN_JAR=CipaUtils-1.2.jar
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

# Vérifier que Maven est installé
if ! command -v mvn &> /dev/null; then
    print_error "Maven n'est pas installé ou pas dans le PATH !"
    exit 1
fi

# Vérifier que Java est installé
if ! command -v java &> /dev/null; then
    print_error "Java n'est pas installé ou pas dans le PATH !"
    exit 1
fi

# [1/4] Compilation du plugin
print_step "1/4" "Compilation du plugin..."
if mvn clean package -q; then
    print_success "Plugin compilé avec succès"
else
    print_error "Erreur lors de la compilation !"
    exit 1
fi

echo

# [2/4] Préparation du serveur de test
print_step "2/4" "Préparation du serveur de test..."

# Créer le dossier du serveur s'il n'existe pas
mkdir -p "$SERVER_DIR/plugins"

# Télécharger PaperMC si nécessaire
if [ ! -f "$SERVER_DIR/server.jar" ]; then
    print_warning "Téléchargement de PaperMC $PAPER_VERSION..."

    # Vérifier que curl ou wget est disponible
    if command -v curl &> /dev/null; then
        curl -L -o "$SERVER_DIR/server.jar" \
            "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
    elif command -v wget &> /dev/null; then
        wget -O "$SERVER_DIR/server.jar" \
            "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
    else
        print_error "Ni curl ni wget n'est disponible pour télécharger PaperMC !"
        exit 1
    fi

    if [ $? -eq 0 ]; then
        print_success "PaperMC téléchargé"
    else
        print_error "Erreur lors du téléchargement de PaperMC !"
        exit 1
    fi
else
    print_success "PaperMC déjà présent"
fi

# [3/4] Installation du plugin
print_step "3/4" "Installation du plugin..."
if cp "target/$PLUGIN_JAR" "$SERVER_DIR/plugins/$PLUGIN_JAR" 2>/dev/null; then
    print_success "Plugin installé"
else
    print_error "Erreur lors de la copie du plugin !"
    exit 1
fi

# Créer l'EULA si nécessaire
if [ ! -f "$SERVER_DIR/eula.txt" ]; then
    echo "eula=true" > "$SERVER_DIR/eula.txt"
    print_success "EULA accepté automatiquement"
fi

# Créer les propriétés du serveur avec des paramètres de test
if [ ! -f "$SERVER_DIR/server.properties" ]; then
    print_step "" "Configuration du serveur de test..."
    cat > "$SERVER_DIR/server.properties" << EOF
# Configuration serveur de test - CipaUtils
server-port=25565
gamemode=creative
difficulty=peaceful
spawn-protection=0
max-players=10
online-mode=false
enable-command-block=true
motd=CipaUtils - Serveur de Test
level-name=test-world
level-type=minecraft:flat
generate-structures=false
spawn-monsters=false
spawn-animals=false
pvp=false
EOF
    print_success "Configuration créée"
fi

echo

# [4/4] Démarrage du serveur
print_step "4/4" "Démarrage du serveur..."
print_warning "Le serveur va démarrer dans 3 secondes..."
sleep 3

cd "$SERVER_DIR"

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
