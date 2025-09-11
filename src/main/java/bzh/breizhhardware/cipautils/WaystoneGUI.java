package bzh.breizhhardware.cipautils;

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
        
        // Retirer la waystone actuelle de la liste
        availableWaystones.removeIf(w -> w.getId().equals(currentWaystone.getId()));
        
        if (availableWaystones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucune autre waystone disponible pour la téléportation !");
            return;
        }

        // Calculer la taille nécessaire (laisser une ligne pour les contrôles)
        int waystoneSlots = availableWaystones.size();
        int lines = Math.max(2, (waystoneSlots + 8) / 9 + 1); // +1 ligne pour les contrôles
        int size = Math.min(54, lines * 9);

        Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Waystones - Téléportation");
        
        // Ajouter toutes les waystones disponibles (réserver seulement la dernière ligne)
        int maxWaystoneSlots = size - 9; // Réserver la dernière ligne pour les contrôles
        for (int i = 0; i < availableWaystones.size() && i < maxWaystoneSlots; i++) {
            Waystone waystone = availableWaystones.get(i);
            ItemStack item = createWaystoneMenuItem(waystone, player);
            gui.setItem(i, item);
        }
        
        // Debug: afficher le nombre de waystones chargées
        plugin.getLogger().info("Waystones disponibles: " + availableWaystones.size());
        plugin.getLogger().info("Taille du GUI: " + size + " slots");
        plugin.getLogger().info("Slots pour waystones: " + maxWaystoneSlots);

        // Ajouter des items de décoration
        addDecorationItems(gui, size);
        
        // Ajouter un item d'information sur la waystone actuelle
        ItemStack currentInfo = createCurrentWaystoneInfo(currentWaystone);
        gui.setItem(size - 5, currentInfo);
        
        // Item de fermeture
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Fermer");
        closeItem.setItemMeta(closeMeta);
        gui.setItem(size - 1, closeItem);
        
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 1.2f);
    }

    private ItemStack createWaystoneMenuItem(Waystone waystone, Player player) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + waystone.getName());
        
        String ownerName = getPlayerName(waystone.getOwner());
        String locationStr = String.format("(%d, %d, %d)", 
            waystone.getLocation().getBlockX(),
            waystone.getLocation().getBlockY(),
            waystone.getLocation().getBlockZ());
        
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Propriétaire: " + ChatColor.WHITE + ownerName,
            ChatColor.GRAY + "Position: " + ChatColor.WHITE + locationStr,
            ChatColor.GRAY + "Monde: " + ChatColor.WHITE + waystone.getLocation().getWorld().getName(),
            "",
            ChatColor.GREEN + "Cliquez pour vous téléporter !",
            ChatColor.GOLD + "ID: " + waystone.getId()
        ));
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentWaystoneInfo(Waystone waystone) {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Waystone Actuelle");
        
        String ownerName = getPlayerName(waystone.getOwner());
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Nom: " + ChatColor.WHITE + waystone.getName(),
            ChatColor.GRAY + "Propriétaire: " + ChatColor.WHITE + ownerName,
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
            return "Inconnu";
        }
    }

    public Waystone getWaystoneFromMenuItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_PEARL) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        for (int i = 0; i < lore.size(); i++) {
            plugin.getLogger().info("  Ligne " + i + ": '" + lore.get(i) + "'");
        }

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
