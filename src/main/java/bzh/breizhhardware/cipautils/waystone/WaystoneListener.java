package bzh.breizhhardware.cipautils.waystone;

import bzh.breizhhardware.cipautils.Main;
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
    private static final long INTERACTION_COOLDOWN = 3000; // 3 seconds

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

        // Check if it's a craftable waystone block
        if (block.getType() == Material.LODESTONE && isWaystoneItem(item)) {
            // Create a new waystone with a default name
            String defaultName = generateWaystoneName(player);

            boolean success = waystoneManager.createWaystone(
                block.getLocation(),
                defaultName,
                player.getUniqueId().toString()
            );

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Waystone '" + defaultName + "' created with success !");
                player.sendMessage(ChatColor.YELLOW + "Use /waystone rename <new_name> to rename");
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "Unable to create a waystone here !");
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
                // Check if the player can destroy this waystone
                if (waystone.getOwner().equals(player.getUniqueId().toString()) ||
                    player.hasPermission("cipautils.waystone.admin")) {

                    waystoneManager.removeWaystone(block.getLocation());
                    player.sendMessage(ChatColor.YELLOW + "Waystone '" + waystone.getName() + "' deleted");

                    // Prevent natural drop of the lodestone
                    event.setDropItems(false);

                    // Give a waystone item to the player
                    ItemStack waystoneItem = createWaystoneItem();
                    player.getInventory().addItem(waystoneItem);
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot delete this waystone !");
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
                // Check cooldown
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                if (lastInteraction.containsKey(playerId) &&
                    currentTime - lastInteraction.get(playerId) < INTERACTION_COOLDOWN) {
                    player.sendMessage(ChatColor.RED + "Please wait before interacting with the waystone again.");
                    return;
                }
                lastInteraction.put(playerId, currentTime);

                // Store the player's current waystone
                playerCurrentWaystone.put(playerId, waystone);

                // Open the GUI
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

        if (!title.equals(ChatColor.DARK_PURPLE + "Waystones - Select a destination")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        // Handle closing
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Handle teleportation
        if (clickedItem.getType() == Material.ENDER_PEARL) {

            Waystone targetWaystone = waystoneGUI.getWaystoneFromMenuItem(clickedItem);

            if (targetWaystone != null) {
                Waystone currentWaystone = playerCurrentWaystone.get(player.getUniqueId());

                if (currentWaystone == null) {
                    player.sendMessage(ChatColor.RED + "Error: Waystone information lost !");
                    player.closeInventory();
                    return;
                }

                // Check if the player can access this waystone
                if (!targetWaystone.isPublic() && !targetWaystone.getOwner().equals(player.getUniqueId().toString())) {
                    player.sendMessage(ChatColor.RED + "You cannot access this waystone !");
                    return;
                }

                // Check dimension
                if (!currentWaystone.getLocation().getWorld().getName().equals(targetWaystone.getLocation().getWorld().getName())) {
                    player.sendMessage(ChatColor.RED + "Impossible to teleport between different dimensions !");
                    player.closeInventory();
                    return;
                }

                // Teleport the player
                Location teleportLocation = targetWaystone.getLocation().clone().add(0.5, 1, 0.5);

                player.closeInventory();
                boolean teleported = player.teleport(teleportLocation);

                if (teleported) {
                    player.sendMessage(ChatColor.GREEN + "Teleported to '" + targetWaystone.getName() + "' !");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatColor.RED + "Error during teleportation !");
                }

                // Clean up the stored current waystone
                playerCurrentWaystone.remove(player.getUniqueId());
            } else {
                player.sendMessage(ChatColor.RED + "Error: Impossible to get the information from the waystone !");
                plugin.getLogger().warning("Impossible to get the waystone from the item menu");
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

        meta.setDisplayName(ChatColor.AQUA + "Empty Waystone");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Place this bloc to create",
            ChatColor.GRAY + "a point of teleportation.",
            ChatColor.GOLD + "Craft: Ender Pearl + Diamants + Lodestone + Obsidian",
            ChatColor.BLUE + "Right-click a placed waystone to use it"
        ));

        item.setItemMeta(meta);
        return item;
    }

    public String generateWaystoneName(Player player) {
        String baseName = "Waystone of " + player.getName();
        String name = baseName;
        int suffix = 1;

        // Check name uniqueness
        while (waystoneManager.getWaystoneByName(name) != null) {
            name = baseName + " (" + suffix + ")";
            suffix++;
        }

        return name;
    }
}
