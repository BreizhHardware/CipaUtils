package bzh.breizhhardware.cipautils;

import bzh.breizhhardware.cipautils.chunkloader.ChunkLoaderManager;
import bzh.breizhhardware.cipautils.recipeManager.RecipeManager;
import bzh.breizhhardware.cipautils.commands.NoMoreSpawnProtectCommand;
import bzh.breizhhardware.cipautils.commands.WaystoneCommand;
import bzh.breizhhardware.cipautils.customRecipe.RecipeManager;
import bzh.breizhhardware.cipautils.grave.GraveListener;
import bzh.breizhhardware.cipautils.waystone.WaystoneListener;
import bzh.breizhhardware.cipautils.waystone.WaystoneManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class Main extends JavaPlugin implements Listener {
    private boolean spawnProtectionDisabled;
    private WaystoneManager waystoneManager;
    private GraveListener graveListener;
    private ChunkLoaderManager chunkLoaderManager;

    @Override
    public void onEnable() {
        // Save the default configuration
        saveDefaultConfig();

        // Load the configuration
        spawnProtectionDisabled = getConfig().getBoolean("disable-spawn-protection", true);

        // Initialize the waystone system
        initWaystoneSystem();
        // Initialize the grave system
        initGraveSystem();
        // Register custom recipes
        initRecipes();
        // Register event listeners
        registerListeners();
        // Register commands
        registerCommands();

        // Apply settings
        if (spawnProtectionDisabled) {
            disableSpawnProtection();
        }

        getLogger().info("NoMoreSpawnProtect is enabled!");
        getLogger().info("Waystone system initialized with crafting recipes!");
    }

    private void initWaystoneSystem() {
        waystoneManager = new WaystoneManager(this);
    }

    private void initGraveSystem() {
        graveListener = new GraveListener(this);
    }

    private void initRecipes() {
        RecipeManager recipeManager = new RecipeManager(this);
        chunkLoaderManager = new ChunkLoaderManager(this);

        // Register recipes
        recipeManager.registerCustomRecipes();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new WaystoneListener(this, waystoneManager), this);
        // Register grave listener
        getServer().getPluginManager().registerEvents(graveListener, this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerCommands() {
        getCommand("nomorespawnprotect").setExecutor(new NoMoreSpawnProtectCommand(this));
        getCommand("waystone").setExecutor(new WaystoneCommand(this, waystoneManager));
        getCommand("toggledeathmsg").setExecutor(graveListener.getToggleDeathMsgCommand());
        getCommand("chunkloader").setExecutor(this);
        getCommand("cipautils").setExecutor(this);

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nomorespawnprotect")) {
            return handleSpawnProtectCommand(sender, args);
        }

        if (command.getName().equalsIgnoreCase("waystone")) {
            return handleWaystoneCommand(sender, args);
        }

        if (command.getName().equalsIgnoreCase("chunkloader")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Commande réservée aux joueurs.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                player.getInventory().addItem(chunkLoaderManager.getChunkLoaderItem());
                player.sendMessage(ChatColor.AQUA + "Vous avez reçu un chunkloader !");
                return true;
            }
            player.sendMessage(ChatColor.YELLOW + "/chunkloader give");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("cipautils")) {
            // if arg is bug send bug report link
            if (args.length > 0 && args[0].equalsIgnoreCase("bug")) {
                sender.sendMessage("§eTo report a bug, please visit: §9https://github.com/BreizhHardware/CipaUtils/issues/new");
                return true;
            }
            // if no args is provided send all available commands
            sender.sendMessage("§e--- CipaUtils Commands ---");
            sender.sendMessage("§f/cipautils bug §7- Report a bug");
            return true;
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
                    sender.sendMessage("§aThe spawn protection is now disabled");
                } else {
                    sender.sendMessage("§aThe spawn protection will follow the server settings");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("status")) {
                String status = spawnProtectionDisabled ? "disabled" : "enabled";
                sender.sendMessage("§aThe spawn protection is currently " + status);
                return true;
            }
        }

        sender.sendMessage("§e--- NoMoreSpawnProtect ---");
        sender.sendMessage("§f/nomorespawnprotect toggle §7- Enable/disable the spawn protection");
        sender.sendMessage("§f/nomorespawnprotect status §7- Show the current status");
        return true;
    }

    public boolean handleWaystoneCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur !");
            return true;
        }

    public void disableSpawnProtection() {
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
