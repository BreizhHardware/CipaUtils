package bzh.breizhhardware.cipautils;

import bzh.breizhhardware.cipautils.customRecipe.RecipeManager;
import bzh.breizhhardware.cipautils.grave.GraveListener;
import bzh.breizhhardware.cipautils.waystone.Waystone;
import bzh.breizhhardware.cipautils.waystone.WaystoneListener;
import bzh.breizhhardware.cipautils.waystone.WaystoneManager;
import bzh.breizhhardware.cipautils.waystone.WaystoneRecipe;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Main extends JavaPlugin {
    private boolean spawnProtectionDisabled;
    private WaystoneManager waystoneManager;
    private GraveListener graveListener;

    @Override
    public void onEnable() {
        // Save the default configuration
        saveDefaultConfig();

        // Load the configuration
        spawnProtectionDisabled = getConfig().getBoolean("disable-spawn-protection", true);

        // Initialize the waystone system
        waystoneManager = new WaystoneManager(this);
    WaystoneRecipe waystoneRecipe = new WaystoneRecipe(this);
    RecipeManager recipeManager = new RecipeManager(this);

    // Register recipes
    waystoneRecipe.registerRecipes();
    recipeManager.registerLightBlockRecipe();

        // Register listeners
        getServer().getPluginManager().registerEvents(new WaystoneListener(this, waystoneManager), this);
        // Register grave listener
        graveListener = new GraveListener(this);
        getServer().getPluginManager().registerEvents(graveListener, this);

        // Register commands
        getCommand("nomorespawnprotect").setExecutor(this);
        getCommand("waystone").setExecutor(this);
        getCommand("toggledeathmsg").setExecutor(graveListener.getToggleDeathMsgCommand());

        // Apply settings
        if (spawnProtectionDisabled) {
            disableSpawnProtection();
        }

        getLogger().info("NoMoreSpawnProtect is enabled!");
        getLogger().info("Waystone system initialized with crafting recipes!");
    }

    @Override
    public void onDisable() {
        if (waystoneManager != null) {
            waystoneManager.saveWaystones();
        }
        saveConfig();
        getLogger().info("NoMoreSpawnProtect is disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("nomorespawnprotect")) {
            return handleSpawnProtectCommand(sender, args);
        }

        if (cmd.getName().equalsIgnoreCase("waystone")) {
            return handleWaystoneCommand(sender, args);
        }

        return false;
    }

    public boolean handleSpawnProtectCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("toggle")) {
                spawnProtectionDisabled = !spawnProtectionDisabled;
                getConfig().set("disable-spawn-protection", spawnProtectionDisabled);
                saveConfig();

                if (spawnProtectionDisabled) {
                    disableSpawnProtection();
                    sender.sendMessage("§aLa protection du spawn est maintenant désactivée");
                } else {
                    sender.sendMessage("§aLa protection du spawn suivra les paramètres du serveur");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("status")) {
                String status = spawnProtectionDisabled ? "désactivée" : "activée";
                sender.sendMessage("§aLa protection du spawn est actuellement " + status);
                return true;
            }
        }

        sender.sendMessage("§e--- NoMoreSpawnProtect ---");
        sender.sendMessage("§f/nomorespawnprotect toggle §7- Activer/désactiver la protection du spawn");
        sender.sendMessage("§f/nomorespawnprotect status §7- Afficher l'état actuel");
        return true;
    }

    public boolean handleWaystoneCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur !");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendWaystoneHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleWaystoneGive(player, args);
            case "list":
                return handleWaystoneList(player);
            case "info":
                return handleWaystoneInfo(player, args);
            case "rename":
                return handleWaystoneRename(player, args);
            default:
                sendWaystoneHelp(player);
                return true;
        }
    }

    public boolean handleWaystoneGive(Player player, String[] args) {
        if (!player.hasPermission("cipautils.waystone.give")) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande !");
            return true;
        }

        ItemStack waystoneItem = WaystoneListener.createWaystoneItem();
        player.getInventory().addItem(waystoneItem);
        player.sendMessage(ChatColor.GREEN + "Waystone vierge ajoutée à votre inventaire !");
        player.sendMessage(ChatColor.YELLOW + "Vous pouvez aussi la crafter avec la recette !");
        return true;
    }

    public boolean handleWaystoneList(Player player) {
        List<Waystone> waystones = waystoneManager.getAvailableWaystones(player);

        if (waystones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucune waystone disponible !");
            player.sendMessage(ChatColor.GRAY + "Craftez une waystone et placez-la pour commencer !");
            return true;
        }

        player.sendMessage(ChatColor.BLUE + "=== Waystones disponibles ===");
        player.sendMessage(ChatColor.GRAY + "Utilisez une waystone pour vous téléporter vers les autres");
        for (Waystone waystone : waystones) {
            String ownerName = getPlayerName(waystone.getOwner());
            String locationStr = String.format("(%d, %d, %d)",
                waystone.getLocation().getBlockX(),
                waystone.getLocation().getBlockY(),
                waystone.getLocation().getBlockZ()
            );

            player.sendMessage(ChatColor.GREEN + "• " + waystone.getName() +
                ChatColor.GRAY + " - " + ownerName + " " + locationStr);
        }
        return true;
    }

    public boolean handleWaystoneInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /waystone info <nom>");
            return true;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String name = nameBuilder.toString().trim();

        Waystone waystone = waystoneManager.getWaystoneByName(name);
        if (waystone == null) {
            player.sendMessage(ChatColor.RED + "Waystone '" + name + "' introuvable !");
            return true;
        }

        player.sendMessage(ChatColor.BLUE + "=== Informations Waystone ===");
        player.sendMessage(ChatColor.GREEN + "Nom: " + waystone.getName());
        player.sendMessage(ChatColor.GREEN + "Propriétaire: " + getPlayerName(waystone.getOwner()));
        player.sendMessage(ChatColor.GREEN + "Publique: " + (waystone.isPublic() ? "Oui" : "Non"));
        player.sendMessage(ChatColor.GREEN + "Position: " +
            waystone.getLocation().getBlockX() + ", " +
            waystone.getLocation().getBlockY() + ", " +
            waystone.getLocation().getBlockZ());
        player.sendMessage(ChatColor.GREEN + "Monde: " + waystone.getLocation().getWorld().getName());
        return true;
    }

    public boolean handleWaystoneRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /waystone rename <nouveau_nom>");
            return true;
        }

        // Find the closest waystone to the player
        Waystone closestWaystone = null;
        double closestDistance = Double.MAX_VALUE;

        for (Waystone waystone : waystoneManager.getAllWaystones()) {
            if (waystone.getOwner().equals(player.getUniqueId().toString())) {
                if (!waystone.getLocation().getWorld().equals(player.getWorld())) {
                    continue; // Ignore waystones in another world
                }
                double distance = waystone.getLocation().distance(player.getLocation());
                if (distance < closestDistance && distance <= 5.0) { // Within a 5 block radius
                    closestDistance = distance;
                    closestWaystone = waystone;
                }
            }
        }

        if (closestWaystone == null) {
            player.sendMessage(ChatColor.RED + "Aucune de vos waystones trouvée à proximité !");
            return true;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String newName = nameBuilder.toString().trim();

        String oldName = closestWaystone.getName();
        closestWaystone.setName(newName);
        waystoneManager.saveWaystones();

        player.sendMessage(ChatColor.GREEN + "Waystone renommée de '" + oldName + "' vers '" + newName + "' !");
        return true;
    }

    public void sendWaystoneHelp(Player player) {
        player.sendMessage(ChatColor.BLUE + "=== Commandes Waystone ===");
        player.sendMessage(ChatColor.GOLD + "Recette de craft:");
        player.sendMessage(ChatColor.YELLOW + "  E D E    E = Ender Pearl");
        player.sendMessage(ChatColor.YELLOW + "  D L D    D = Diamond");
        player.sendMessage(ChatColor.YELLOW + "  O O O    L = Lodestone, O = Obsidian");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "/waystone list" + ChatColor.GRAY + " - Afficher toutes les waystones");
        player.sendMessage(ChatColor.GREEN + "/waystone info <nom>" + ChatColor.GRAY + " - Informations sur une waystone");
        player.sendMessage(ChatColor.GREEN + "/waystone rename <nom>" + ChatColor.GRAY + " - Renommer votre waystone proche");
        if (player.hasPermission("cipautils.waystone.give")) {
            player.sendMessage(ChatColor.GREEN + "/waystone give" + ChatColor.GRAY + " - Obtenir un item waystone");
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.BLUE + "Utilisation:");
        player.sendMessage(ChatColor.GRAY + "• Craftez et placez une waystone");
        player.sendMessage(ChatColor.GRAY + "• Clic droit sur une waystone pour ouvrir le menu de téléportation");
        player.sendMessage(ChatColor.GRAY + "• Vous ne pouvez vous téléporter QUE depuis une autre waystone");
    }

    public String getPlayerName(String uuid) {
        try {
            Player player = getServer().getPlayer(java.util.UUID.fromString(uuid));
            if (player != null) return player.getName();

            return getServer().getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
        } catch (Exception e) {
            return "Inconnu";
        }
    }

    private void disableSpawnProtection() {
        // Disable immediately
        getServer().setSpawnRadius(0);
        getLogger().info("Spawn protection disabled!");

        // Keep disabling
        new BukkitRunnable() {
            @Override
            public void run() {
                if (spawnProtectionDisabled && getServer().getSpawnRadius() > 0) {
                    getServer().setSpawnRadius(0);
                }
            }
        }.runTaskTimer(this, 1200L, 1200L); // Check every minute

        getLogger().info("For a permanent solution, set 'spawn-protection=0' in server.properties");
    }
}
