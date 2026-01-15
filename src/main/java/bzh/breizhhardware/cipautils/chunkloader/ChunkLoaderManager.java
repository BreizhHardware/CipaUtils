package bzh.breizhhardware.cipautils.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChunkLoaderManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Location> playerChunkLoaders = new HashMap<>();
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private static final String CHUNKLOADER_NAME = "§bChunkloader";
    private static final Material CHUNKLOADER_BLOCK = Material.END_PORTAL_FRAME;

    public ChunkLoaderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chunkloaders.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack getChunkLoaderItem() {
        ItemStack item = new ItemStack(CHUNKLOADER_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CHUNKLOADER_NAME);
        meta.setLore(Arrays.asList("§7Place for chunkload 3x3 chunks", "§7Shift + Right Click to pick up"));
        item.setItemMeta(meta);
        return item;
    }

    private void loadData() {
        playerChunkLoaders.clear();
        if (dataConfig.contains("chunkloaders")) {
            for (String uuidStr : dataConfig.getConfigurationSection("chunkloaders").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String locStr = dataConfig.getString("chunkloaders." + uuidStr);
                String[] parts = locStr.split(",");
                World world = Bukkit.getWorld(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                playerChunkLoaders.put(uuid, new Location(world, x, y, z));
            }
        }
    }

    private void saveData() {
        dataConfig.set("chunkloaders", null);
        for (Map.Entry<UUID, Location> entry : playerChunkLoaders.entrySet()) {
            Location loc = entry.getValue();
            String value = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
            dataConfig.set("chunkloaders." + entry.getKey(), value);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isChunkLoaderBlock(Block block) {
        if (block.getType() != CHUNKLOADER_BLOCK) return false;
        // Check if the block location corresponds to a registered chunkloader
        return getOwner(block.getLocation()) != null;
    }

    private UUID getOwner(Location loc) {
        for (Map.Entry<UUID, Location> entry : playerChunkLoaders.entrySet()) {
            Location storedLoc = entry.getValue();
            if (storedLoc.getWorld().equals(loc.getWorld()) &&
                storedLoc.getBlockX() == loc.getBlockX() &&
                storedLoc.getBlockY() == loc.getBlockY() &&
                storedLoc.getBlockZ() == loc.getBlockZ()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void loadChunks(Location loc) {
        World world = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.getChunkAt(cx + dx, cz + dz);
                world.addPluginChunkTicket(chunk.getX(), chunk.getZ(), plugin);
            }
        }
    }

    private void unloadChunks(Location loc) {
        World world = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = world.getChunkAt(cx + dx, cz + dz);
                world.removePluginChunkTicket(chunk.getX(), chunk.getZ(), plugin);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();
        ItemMeta itemMeta = (itemInHand != null) ? itemInHand.getItemMeta() : null;
        if (itemMeta == null || !itemMeta.hasDisplayName() || !CHUNKLOADER_NAME.equals(itemMeta.getDisplayName())) {
            return;
        }
        if (playerChunkLoaders.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active chunkloader !");
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlockPlaced();
        if (block.getType() != CHUNKLOADER_BLOCK) return;
        block.setMetadata("chunkloader", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getUniqueId().toString()));
        playerChunkLoaders.put(player.getUniqueId(), block.getLocation());
        loadChunks(block.getLocation());
        saveData();
        player.sendMessage("§aChunkloader placed !");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != CHUNKLOADER_BLOCK) return;

        UUID uuid = getOwner(block.getLocation());
        if (uuid == null) return;

        unloadChunks(block.getLocation());
        playerChunkLoaders.remove(uuid);
        saveData();
        event.getPlayer().sendMessage("§cChunkloader deleted.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != CHUNKLOADER_BLOCK) return;

        UUID uuid = getOwner(block.getLocation());
        if (uuid == null) return;

        Player player = event.getPlayer();

        if (!player.getUniqueId().equals(uuid) && !player.isOp()) {
             player.sendMessage("§cThis chunkloader isn't yours.");
             return;
        }

        if (player.isSneaking() && event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            unloadChunks(block.getLocation());
            playerChunkLoaders.remove(uuid);
            saveData();

            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), getChunkLoaderItem());

            player.sendMessage("§aChunkloader retrieved.");
            event.setCancelled(true);
            return;
        }

        Location loc = block.getLocation();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        // Show particles
        showChunkBoundaries(event.getPlayer(), loc);

        StringBuilder sb = new StringBuilder("§eChunks loaded :\n");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sb.append("§7[").append(cx + dx).append(", ").append(cz + dz).append("] ");
            }
            sb.append("\n");
        }
        event.getPlayer().sendMessage(sb.toString());
    }

    private void showChunkBoundaries(Player player, Location center) {
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;

        double minX = (cx - 1) * 16;
        double minZ = (cz - 1) * 16;
        double maxX = (cx + 2) * 16;
        double maxZ = (cz + 2) * 16;

        double y = center.getY() + 1.2;

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count > 5) { // Run for a few seconds
                    this.cancel();
                    return;
                }

                for (double x = minX; x <= maxX; x += 2.0) {
                    player.spawnParticle(Particle.FLAME, x, y, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, x, y, maxZ, 1, 0, 0, 0, 0);
                }
                for (double z = minZ; z <= maxZ; z += 2.0) {
                    player.spawnParticle(Particle.FLAME, minX, y, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, maxX, y, z, 1, 0, 0, 0, 0);
                }
                count++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L); // Every 0.5s
    }

}
