package bzh.breizhhardware.cipautils.waystone;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WaystoneManager {
    private final Main plugin;
    private final Map<String, Waystone> waystones = new HashMap<>();
    private File waystoneFile;
    private FileConfiguration waystoneConfig;

    public WaystoneManager(Main plugin) {
        this.plugin = plugin;
        setupWaystoneFile();
        loadWaystones();
    }

    private void setupWaystoneFile() {
        waystoneFile = new File(plugin.getDataFolder(), "waystones.yml");
        if (!waystoneFile.exists()) {
            plugin.saveResource("waystones.yml", false);
        }
        waystoneConfig = YamlConfiguration.loadConfiguration(waystoneFile);
    }

    public void saveWaystones() {
        waystoneConfig.set("waystones", null);

        for (Map.Entry<String, Waystone> entry : waystones.entrySet()) {
            String key = "waystones." + entry.getKey();
            Waystone waystone = entry.getValue();

            waystoneConfig.set(key + ".name", waystone.getName());
            waystoneConfig.set(key + ".owner", waystone.getOwner());
            waystoneConfig.set(key + ".world", waystone.getLocation().getWorld().getName());
            waystoneConfig.set(key + ".x", waystone.getLocation().getX());
            waystoneConfig.set(key + ".y", waystone.getLocation().getY());
            waystoneConfig.set(key + ".z", waystone.getLocation().getZ());
            waystoneConfig.set(key + ".yaw", waystone.getLocation().getYaw());
            waystoneConfig.set(key + ".pitch", waystone.getLocation().getPitch());
            waystoneConfig.set(key + ".public", waystone.isPublic());
        }

        try {
            waystoneConfig.save(waystoneFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder les waystones : " + e.getMessage());
        }
    }

    public void loadWaystones() {
        waystones.clear();
        ConfigurationSection waystoneSection = waystoneConfig.getConfigurationSection("waystones");

        if (waystoneSection == null) return;

        for (String key : waystoneSection.getKeys(false)) {
            try {
                String name = waystoneConfig.getString("waystones." + key + ".name");
                String owner = waystoneConfig.getString("waystones." + key + ".owner");
                String worldName = waystoneConfig.getString("waystones." + key + ".world");
                double x = waystoneConfig.getDouble("waystones." + key + ".x");
                double y = waystoneConfig.getDouble("waystones." + key + ".y");
                double z = waystoneConfig.getDouble("waystones." + key + ".z");
                float yaw = (float) waystoneConfig.getDouble("waystones." + key + ".yaw");
                float pitch = (float) waystoneConfig.getDouble("waystones." + key + ".pitch");
                boolean isPublic = waystoneConfig.getBoolean("waystones." + key + ".public", true);

                Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
                Waystone waystone = new Waystone(key, name, owner, location, isPublic);

                waystones.put(key, waystone);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du chargement de la waystone " + key + " : " + e.getMessage());
            }
        }

        plugin.getLogger().info("Chargé " + waystones.size() + " waystones");
    }

    public boolean createWaystone(Location location, String name, String owner) {
        if (getWaystoneAt(location) != null) {
            return false; // Une waystone existe déjà à cet endroit
        }

        String id = UUID.randomUUID().toString();
        Waystone waystone = new Waystone(id, name, owner, location, true);
        waystones.put(id, waystone);
        saveWaystones();
        return true;
    }

    public boolean removeWaystone(Location location) {
        Waystone waystone = getWaystoneAt(location);
        if (waystone != null) {
            waystones.remove(waystone.getId());
            saveWaystones();
            return true;
        }
        return false;
    }

    public Waystone getWaystoneAt(Location location) {
        for (Waystone waystone : waystones.values()) {
            Location waystoneLocation = waystone.getLocation();
            if (waystoneLocation.getWorld().equals(location.getWorld()) &&
                waystoneLocation.getBlockX() == location.getBlockX() &&
                waystoneLocation.getBlockY() == location.getBlockY() &&
                waystoneLocation.getBlockZ() == location.getBlockZ()) {
                return waystone;
            }
        }
        return null;
    }

    public List<Waystone> getAvailableWaystones(Player player) {
        List<Waystone> available = new ArrayList<>();
        for (Waystone waystone : waystones.values()) {
            if (waystone.isPublic() || waystone.getOwner().equals(player.getUniqueId().toString())) {
                available.add(waystone);
            }
        }
        return available;
    }

    public Waystone getWaystoneById(String id) {
        return waystones.get(id);
    }

    public Waystone getWaystoneByName(String name) {
        for (Waystone waystone : waystones.values()) {
            if (waystone.getName().equalsIgnoreCase(name)) {
                return waystone;
            }
        }
        return null;
    }

    public Collection<Waystone> getAllWaystones() {
        return waystones.values();
    }
}
