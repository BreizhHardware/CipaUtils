package bzh.breizhhardware.cipautils.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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
        meta.setLore(Collections.singletonList("§7Place for chunkload 3x3 chunks"));
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
        if (!block.hasMetadata("chunkloader")) return false;
        return true;
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
        if (!event.getItemInHand().hasItemMeta() || !event.getItemInHand().getItemMeta().getDisplayName().equals(CHUNKLOADER_NAME)) return;
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
        if (!isChunkLoaderBlock(block)) return;
        String uuidStr = block.getMetadata("chunkloader").get(0).asString();
        UUID uuid = UUID.fromString(uuidStr);
        unloadChunks(block.getLocation());
        playerChunkLoaders.remove(uuid);
        saveData();
        event.getPlayer().sendMessage("§cChunkloader deleted.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!isChunkLoaderBlock(block)) return;
        String uuidStr = block.getMetadata("chunkloader").get(0).asString();
        UUID uuid = UUID.fromString(uuidStr);
        if (!event.getPlayer().getUniqueId().equals(uuid)) {
            event.getPlayer().sendMessage("§cThis chunkloader isn't yours.");
            return;
        }
        Location loc = block.getLocation();
        World world = loc.getWorld();
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        StringBuilder sb = new StringBuilder("§eChunks loaded :\n");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sb.append("§7[").append(cx + dx).append(", ").append(cz + dz).append("] ");
            }
            sb.append("\n");
        }
        event.getPlayer().sendMessage(sb.toString());
    }
}
