package bzh.breizhhardware.cipautils.waystone;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class WaystoneGUI {
    private final Main plugin;
    private final WaystoneManager waystoneManager;

    public WaystoneGUI(Main plugin, WaystoneManager waystoneManager) {
        this.plugin = plugin;
        this.waystoneManager = waystoneManager;
    }

    public void openWaystoneMenu(Player player, Waystone currentWaystone) {
        List<Waystone> availableWaystones = waystoneManager.getAvailableWaystones(player);
        
        // Remove the current waystone from the list
        availableWaystones.removeIf(w -> w.getId().equals(currentWaystone.getId()));
        
        if (availableWaystones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No other waystones available for teleportation.");
            return;
        }

        // Calculate the required size (leave one row for controls)
        int waystoneSlots = availableWaystones.size();
        int lines = Math.max(2, (waystoneSlots + 8) / 9 + 1); // +1 row for controls
        int size = Math.min(54, lines * 9);

        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Waystones - Select a destination");
        
        // Add all available waystones (reserve only the last row)
        int maxWaystoneSlots = size - 9; // Reserve the last row for controls
        for (int i = 0; i < availableWaystones.size() && i < maxWaystoneSlots; i++) {
            Waystone waystone = availableWaystones.get(i);
            ItemStack item = createWaystoneMenuItem(waystone, player);
            gui.setItem(i, item);
        }

        // Add decoration items
        addDecorationItems(gui, size);
        
        // Add an info item about the current waystone
        ItemStack currentInfo = createCurrentWaystoneInfo(currentWaystone);
        gui.setItem(size - 5, currentInfo);
        
        // Close item
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeItem.setItemMeta(closeMeta);
        gui.setItem(size - 1, closeItem);
        
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 1.2f);
    }

    private ItemStack createWaystoneMenuItem(Waystone waystone, Player player) {
        ItemStack item;
        if (waystone.getCustomItem() != null && !waystone.getCustomItem().getType().isAir()) {
            item = waystone.getCustomItem().clone();
        } else {
            item = new ItemStack(Material.ENDER_PEARL);
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + waystone.getName());
        String ownerName = getPlayerName(waystone.getOwner());
        String locationStr = String.format("(%d, %d, %d)", 
            waystone.getLocation().getBlockX(),
            waystone.getLocation().getBlockY(),
            waystone.getLocation().getBlockZ());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Owner: " + ChatColor.WHITE + ownerName,
            ChatColor.GRAY + "Position: " + ChatColor.WHITE + locationStr,
            ChatColor.GRAY + "World: " + ChatColor.WHITE + waystone.getLocation().getWorld().getName(),
            "",
            ChatColor.GREEN + "Click to teleport !",
            ChatColor.GOLD + "ID: " + waystone.getId()
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentWaystoneInfo(Waystone waystone) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Current Waystone");
        
        String ownerName = getPlayerName(waystone.getOwner());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Name: " + ChatColor.WHITE + waystone.getName(),
            ChatColor.GRAY + "Owner: " + ChatColor.WHITE + ownerName,
            ChatColor.GRAY + "Position: " + ChatColor.WHITE + 
                waystone.getLocation().getBlockX() + ", " +
                waystone.getLocation().getBlockY() + ", " +
                waystone.getLocation().getBlockZ()
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    private void addDecorationItems(Inventory gui, int size) {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        // Remplir la dernière ligne avec du verre
        for (int i = size - 9; i < size; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }
    }

    private String getPlayerName(String uuid) {
        try {
            Player player = plugin.getServer().getPlayer(java.util.UUID.fromString(uuid));
            if (player != null) return player.getName();
            
            return plugin.getServer().getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public Waystone getWaystoneFromMenuItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_PEARL) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();

        for (String line : lore) {
            // Nettoyer la ligne de tous les codes couleur pour la comparaison
            String cleanLine = ChatColor.stripColor(line);

            if (cleanLine.startsWith("ID: ")) {
                String id = cleanLine.substring(4);
                Waystone waystone = waystoneManager.getWaystoneById(id);
                return waystone;
            }
        }

        return null;
    }
}
