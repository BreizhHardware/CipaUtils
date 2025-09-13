package bzh.breizhhardware.cipautils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaystoneListener implements Listener {
    private final Main plugin;
    private final WaystoneManager waystoneManager;
    private final WaystoneGUI waystoneGUI;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    private final Map<UUID, Waystone> playerCurrentWaystone = new HashMap<>();
    private static final long INTERACTION_COOLDOWN = 3000; // 3 secondes

    public WaystoneListener(Main plugin, WaystoneManager waystoneManager) {
        this.plugin = plugin;
        this.waystoneManager = waystoneManager;
        this.waystoneGUI = new WaystoneGUI(plugin, waystoneManager);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        // Vérifier si c'est un bloc waystone craftable
        if (block.getType() == Material.LODESTONE && isWaystoneItem(item)) {
            // Créer une nouvelle waystone avec un nom par défaut
            String defaultName = generateWaystoneName(player);

            boolean success = waystoneManager.createWaystone(
                block.getLocation(),
                defaultName,
                player.getUniqueId().toString()
            );

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Waystone '" + defaultName + "' créée avec succès !");
                player.sendMessage(ChatColor.YELLOW + "Utilisez /waystone rename <nouveau_nom> pour la renommer");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "Impossible de créer une waystone ici !");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.LODESTONE) {
            Waystone waystone = waystoneManager.getWaystoneAt(block.getLocation());
            if (waystone != null) {
                // Vérifier si le joueur peut détruire cette waystone
                if (waystone.getOwner().equals(player.getUniqueId().toString()) ||
                    player.hasPermission("cipautils.waystone.admin")) {

                    waystoneManager.removeWaystone(block.getLocation());
                    player.sendMessage(ChatColor.YELLOW + "Waystone '" + waystone.getName() + "' supprimée");

                    // Donner un item waystone au joueur
                    ItemStack waystoneItem = createWaystoneItem();
                    player.getInventory().addItem(waystoneItem);
                } else {
                    player.sendMessage(ChatColor.RED + "Vous ne pouvez pas détruire cette waystone !");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.LODESTONE) {
            Waystone waystone = waystoneManager.getWaystoneAt(block.getLocation());
            if (waystone != null) {
                // Vérifier le cooldown
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                if (lastInteraction.containsKey(playerId) &&
                    currentTime - lastInteraction.get(playerId) < INTERACTION_COOLDOWN) {
                    return;
                }
                lastInteraction.put(playerId, currentTime);

                // Stocker la waystone actuelle du joueur
                playerCurrentWaystone.put(playerId, waystone);

                // Ouvrir l'interface graphique
                waystoneGUI.openWaystoneMenu(player, waystone);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals(ChatColor.DARK_PURPLE + "Waystones - Téléportation")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        // Gérer la fermeture
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Gérer la téléportation
        if (clickedItem.getType() == Material.ENDER_PEARL) {

            Waystone targetWaystone = waystoneGUI.getWaystoneFromMenuItem(clickedItem);

            if (targetWaystone != null) {
                Waystone currentWaystone = playerCurrentWaystone.get(player.getUniqueId());

                if (currentWaystone == null) {
                    player.sendMessage(ChatColor.RED + "Erreur: Waystone d'origine introuvable !");
                    player.closeInventory();
                    return;
                }

                // Vérifier si le joueur peut accéder à cette waystone
                if (!targetWaystone.isPublic() && !targetWaystone.getOwner().equals(player.getUniqueId().toString())) {
                    player.sendMessage(ChatColor.RED + "Vous n'avez pas accès à cette waystone !");
                    return;
                }

                // Vérifier la dimension
                if (!currentWaystone.getLocation().getWorld().getName().equals(targetWaystone.getLocation().getWorld().getName())) {
                    player.sendMessage(ChatColor.RED + "Impossible de se téléporter entre dimensions !");
                    player.closeInventory();
                    return;
                }

                // Téléporter le joueur
                Location teleportLocation = targetWaystone.getLocation().clone().add(0.5, 1, 0.5);

                player.closeInventory();
                boolean teleported = player.teleport(teleportLocation);

                if (teleported) {
                    player.sendMessage(ChatColor.GREEN + "Téléporté vers '" + targetWaystone.getName() + "' !");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatColor.RED + "Erreur lors de la téléportation !");
                }

                // Nettoyer la waystone actuelle stockée
                playerCurrentWaystone.remove(player.getUniqueId());
            } else {
                player.sendMessage(ChatColor.RED + "Erreur: Impossible de récupérer les informations de la waystone !");
                plugin.getLogger().warning("Impossible de récupérer la waystone depuis l'item menu");
            }
        }
    }

    private boolean isWaystoneItem(ItemStack item) {
        if (item == null || item.getType() != Material.LODESTONE) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.hasDisplayName() &&
               meta.getDisplayName().contains("Waystone") &&
               meta.hasLore();
    }

    public static ItemStack createWaystoneItem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Waystone Vierge");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Placez ce bloc pour créer",
            ChatColor.GRAY + "un point de téléportation",
            ChatColor.GOLD + "Craft: Ender Pearl + Diamants + Lodestone + Obsidienne",
            ChatColor.BLUE + "Clic droit après placement pour utiliser"
        ));

        item.setItemMeta(meta);
        return item;
    }

    public String generateWaystoneName(Player player) {
        String baseName = "Waystone de " + player.getName();
        String name = baseName;
        int suffix = 1;

        // Vérifier l'unicité du nom
        while (waystoneManager.getWaystoneByName(name) != null) {
            name = baseName + " (" + suffix + ")";
            suffix++;
        }

        return name;
    }
}
