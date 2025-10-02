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

    public void openWaystoneMenu(Player player, Waystone currentWaystone, int page) {
        List<Waystone> availableWaystones = waystoneManager.getAvailableWaystones(player);
        availableWaystones.removeIf(w -> w.getId().equals(currentWaystone.getId()));
        if (availableWaystones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No other waystones available for teleportation.");
            return;
        }
        int waystonesPerPage = 45; // 5 rows of 9, last row for controls
        int totalPages = (int) Math.ceil((double) availableWaystones.size() / waystonesPerPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Waystones - Page " + (page+1) + "/" + totalPages);
        int start = page * waystonesPerPage;
        int end = Math.min(start + waystonesPerPage, availableWaystones.size());
        for (int i = start; i < end; i++) {
            Waystone waystone = availableWaystones.get(i);
            ItemStack item = createWaystoneMenuItem(waystone, player);
            gui.setItem(i - start, item);
        }
        addDecorationItems(gui, size);
        ItemStack currentInfo = createCurrentWaystoneInfo(currentWaystone);
        gui.setItem(size - 5, currentInfo);
        // Navigation buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prev.setItemMeta(prevMeta);
            gui.setItem(size - 9, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
            next.setItemMeta(nextMeta);
            gui.setItem(size - 8, next);
        }
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeItem.setItemMeta(closeMeta);
        gui.setItem(size - 1, closeItem);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 1.2f);
    }

    // Ancienne méthode conservée pour compatibilité
    public void openWaystoneMenu(Player player, Waystone currentWaystone) {
        openWaystoneMenu(player, currentWaystone, 0);
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
