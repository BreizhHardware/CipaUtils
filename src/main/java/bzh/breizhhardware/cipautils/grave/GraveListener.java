package bzh.breizhhardware.cipautils.grave;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Barrel;
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
    // Map pour stocker les inventaires custom des tombes
    private final Map<Location, Inventory> graveInventories = new HashMap<>();
    // Set pour stocker les joueurs qui ont désactivé le message de mort
    private final Set<UUID> deathMsgDisabled = new HashSet<>();

    public GraveListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        // Message d'information si activé
        if (!deathMsgDisabled.contains(player.getUniqueId())) {
            player.sendMessage("§eTip: Shift-right click your grave to automatically recover all your items!");
            player.sendMessage("§7(Use /toggledeathmsg to disable this message)");
        }

        Block barrelBlock = findBarrelLocation(deathLoc);
        if (barrelBlock == null) {
            barrelBlock = deathLoc.getBlock();
        }

        barrelBlock.setType(Material.BARREL);
        BlockState state = barrelBlock.getState();
        if (!(state instanceof Barrel)) return;

        // Création d'un inventaire custom de 54 cases
        Inventory graveInventory = Bukkit.createInventory(null, 54, "Grave de " + player.getName());
        graveInventories.put(barrelBlock.getLocation(), graveInventory);

        // Ajout des items de la mort
        for (ItemStack item : event.getDrops()) {
            if (item != null) graveInventory.addItem(item);
        }
        event.getDrops().clear();

        // Création du hologramme
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String customName = player.getName() + " - " + timeStr;
        Location hologramLoc = barrelBlock.getLocation().add(0.5, 1.2, 0.5);
        ArmorStand as = (ArmorStand) barrelBlock.getWorld().spawn(hologramLoc, ArmorStand.class);
        as.setVisible(false);
        as.setMarker(true);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.setCustomName(customName);
        as.setSmall(true);
        graves.put(barrelBlock.getLocation(), as.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.BARREL) return;
        Location loc = block.getLocation();
        Inventory graveInventory = graveInventories.get(loc);
        if (graveInventory == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        // Si le joueur fait un shift-clic droit
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            boolean allAdded = true;
            ItemStack[] contents = graveInventory.getContents();
            graveInventory.clear();
            for (ItemStack item : contents) {
                if (item != null) {
                    HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(item);
                    if (!notAdded.isEmpty()) {
                        allAdded = false;
                        // Remettre les items non ajoutés dans la tombe
                        for (ItemStack left : notAdded.values()) {
                            graveInventory.addItem(left);
                        }
                    }
                }
            }
            if (graveInventory.isEmpty()) {
                player.sendMessage("§aTous les items ont été transférés dans votre inventaire !");
                block.setType(Material.AIR);
                graveInventories.remove(loc);
                UUID holoId = graves.get(loc);
                graves.remove(loc);
                if (holoId != null && Bukkit.getEntity(holoId) != null) {
                    Bukkit.getEntity(holoId).remove();
                }
            } else {
                player.sendMessage("§eVotre inventaire est plein, certains items sont restés dans la tombe !");
            }
        } else {
            // Sinon, ouvrir l'inventaire normalement
            player.openInventory(graveInventory);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        // Vérifie si c'est un inventaire de tombe
        if (inv.getSize() != 54 || !event.getView().getTitle().startsWith("Grave de ")) return;
        Location loc = null;
        for (Map.Entry<Location, Inventory> entry : graveInventories.entrySet()) {
            if (entry.getValue().equals(inv)) {
                loc = entry.getKey();
                break;
            }
        }
        if (loc == null) return;
        UUID holoId = graves.get(loc);
        if (inv.isEmpty()) {
            loc.getBlock().setType(Material.AIR);
            graveInventories.remove(loc);
            graves.remove(loc);
            if (holoId != null && Bukkit.getEntity(holoId) != null) {
                Bukkit.getEntity(holoId).remove();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BARREL) return;
        Location loc = block.getLocation();
        UUID holoId = graves.get(loc);
        Inventory graveInventory = graveInventories.get(loc);
        if (graveInventory != null) {
            for (ItemStack item : graveInventory.getContents()) {
                if (item != null) block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
            graveInventories.remove(loc);
            graves.remove(loc);
            event.setDropItems(false);
            if (holoId != null && Bukkit.getEntity(holoId) != null) {
                Bukkit.getEntity(holoId).remove();
            }
        }
        // Si ce n'est pas une tombe, ne pas annuler les drops
    }

    @EventHandler
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        // Empêche la destruction des tombes (et pas tous les barrels) par explosion de bloc
        event.blockList().removeIf(block -> graveInventories.containsKey(block.getLocation()));
    }

    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        // Empêche la destruction des tombes (et pas tous les barrels) par explosion d'entité
        event.blockList().removeIf(block -> graveInventories.containsKey(block.getLocation()));
    }

    private Block findBarrelLocation(Location loc) {
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

    public boolean isDeathMsgDisabled(UUID uuid) {
        return deathMsgDisabled.contains(uuid);
    }

    public void setDeathMsgDisabled(UUID uuid, boolean disabled) {
        if (disabled) {
            deathMsgDisabled.add(uuid);
        } else {
            deathMsgDisabled.remove(uuid);
        }
    }

    public ToggleDeathMsgCommand getToggleDeathMsgCommand() {
        return new ToggleDeathMsgCommand(this);
    }
}
