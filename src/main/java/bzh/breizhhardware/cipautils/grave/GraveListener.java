package bzh.breizhhardware.cipautils.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

public class GraveListener implements Listener {
    private final JavaPlugin plugin;
    // Map pour stocker location -> hologram UUID
    private final Map<Location, UUID> graves = new HashMap<>();

    public GraveListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        Block chestBlock = findChestLocation(deathLoc);
        if (chestBlock == null) {
            // Si on ne trouve pas d'emplacement correct, on essaie la position même
            chestBlock = deathLoc.getBlock();
        }

        // Remplacer le bloc par un coffre
        chestBlock.setType(Material.CHEST);
        BlockState state = chestBlock.getState();
        if (!(state instanceof Chest)) return;

        Chest chest = (Chest) state;
        Inventory inv = chest.getInventory();

        // Mettre les items de la mort dans le coffre
        for (ItemStack item : event.getDrops()) {
            if (item != null) inv.addItem(item);
        }
        event.getDrops().clear();

        // Créer le texte flottant (ArmorStand) au dessus du coffre
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String customName = player.getName() + " - " + timeStr;

        Location hologramLoc = chestBlock.getLocation().add(0.5, 1.2, 0.5);
        ArmorStand as = (ArmorStand) chestBlock.getWorld().spawn(hologramLoc, ArmorStand.class);
        as.setVisible(false);
        as.setMarker(true);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.setCustomName(customName);
        as.setSmall(true);

        // Stocker la référence
        Location key = chestBlock.getLocation();
        graves.put(key, as.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof Chest)) return;

        Chest chest = (Chest) inv.getHolder();
        Location loc = chest.getLocation();

        UUID holoId = graves.get(loc);
        if (holoId == null) return;

        if (inv.isEmpty()) {
            // Supprimer le coffre
            loc.getBlock().setType(Material.AIR);
            graves.remove(loc);
            // Supprimer le hologramme s'il existe
            if (Bukkit.getEntity(holoId) != null) {
                Bukkit.getEntity(holoId).remove();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;

        Location loc = block.getLocation();
        UUID holoId = graves.get(loc);
        if (holoId == null) return;

        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory inv = chest.getInventory();
            for (ItemStack item : inv.getContents()) {
                if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
            inv.clear();
        }

        graves.remove(loc);
        // Empêche le drop du coffre lui-même
        event.setDropItems(false);

        // Supprimer hologramme
        if (holoId != null && Bukkit.getEntity(holoId) != null) {
            Bukkit.getEntity(holoId).remove();
        }
    }

    private Block findChestLocation(Location loc) {
        // Cherche un bloc d'air au-dessus d'un bloc solide dans les 3 blocs au-dessus
        for (int y = 0; y <= 3; y++) {
            Block b = loc.clone().add(0, y, 0).getBlock();
            Block below = b.getRelative(BlockFace.DOWN);
            if (b.getType() == Material.AIR && below.getType().isSolid()) {
                return b;
            }
        }
        return null;
    }
}
