package bzh.breizhhardware.cipautils.commands;

import bzh.breizhhardware.cipautils.waystone.Waystone;
import bzh.breizhhardware.cipautils.waystone.WaystoneListener;
import bzh.breizhhardware.cipautils.waystone.WaystoneManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WaystoneCommand implements CommandExecutor {
    private final WaystoneManager waystoneManager;
    private final JavaPlugin plugin;

    public WaystoneCommand(JavaPlugin plugin, WaystoneManager waystoneManager) {
        this.plugin = plugin;
        this.waystoneManager = waystoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is reserved for players.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendWaystoneHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give":
                return handleWaystoneGive(player, args);
            case "list":
                return handleWaystoneList(player);
            case "info":
                return handleWaystoneInfo(player, args);
            case "rename":
                return handleWaystoneRename(player, args);
            case "setitem":
                return handleWaystoneSetItem(player);
            default:
                sendWaystoneHelp(player);
                return true;
        }
    }

    private boolean handleWaystoneGive(Player player, String[] args) {
        if (!player.hasPermission("cipautils.waystone.give")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        ItemStack waystoneItem = WaystoneListener.createWaystoneItem();
        player.getInventory().addItem(waystoneItem);
        player.sendMessage(ChatColor.GREEN + "Empty waystone added to your inventory !");
        player.sendMessage(ChatColor.YELLOW + "You can also craft and place it to create a new waystone.");
        return true;
    }

    private boolean handleWaystoneList(Player player) {
        List<Waystone> waystones = waystoneManager.getAvailableWaystones(player);
        if (waystones.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No waystones available !");
            player.sendMessage(ChatColor.GRAY + "Craft and place a waystone to create one.");
            return true;
        }
        player.sendMessage(ChatColor.BLUE + "=== Waystones available ===");
        player.sendMessage(ChatColor.GRAY + "Use a waystone to teleport to another one.");
        for (Waystone waystone : waystones) {
            String ownerName = getPlayerName(waystone.getOwner());
            String locationStr = String.format("(%d, %d, %d)",
                waystone.getLocation().getBlockX(),
                waystone.getLocation().getBlockY(),
                waystone.getLocation().getBlockZ()
            );
            player.sendMessage(ChatColor.GREEN + "• " + waystone.getName() +
                ChatColor.GRAY + " - " + ownerName + " " + locationStr);
        }
        return true;
    }

    private boolean handleWaystoneInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /waystone info <name>");
            return true;
        }
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String name = nameBuilder.toString().trim();
        Waystone waystone = waystoneManager.getWaystoneByName(name);
        if (waystone == null) {
            player.sendMessage(ChatColor.RED + "Waystone '" + name + "' not found !");
            return true;
        }
        player.sendMessage(ChatColor.BLUE + "=== Waystone Information ===");
        player.sendMessage(ChatColor.GREEN + "Name: " + waystone.getName());
        player.sendMessage(ChatColor.GREEN + "Owner: " + getPlayerName(waystone.getOwner()));
        player.sendMessage(ChatColor.GREEN + "Public: " + (waystone.isPublic() ? "Yes" : "No"));
        player.sendMessage(ChatColor.GREEN + "Position: " +
            waystone.getLocation().getBlockX() + ", " +
            waystone.getLocation().getBlockY() + ", " +
            waystone.getLocation().getBlockZ());
        player.sendMessage(ChatColor.GREEN + "World: " + waystone.getLocation().getWorld().getName());
        return true;
    }

    private boolean handleWaystoneRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /waystone rename <new_name>");
            return true;
        }
        Waystone closestWaystone = null;
        double closestDistance = Double.MAX_VALUE;
        for (Waystone waystone : waystoneManager.getAllWaystones()) {
            if (waystone.getOwner().equals(player.getUniqueId().toString())) {
                if (!waystone.getLocation().getWorld().equals(player.getWorld())) {
                    continue;
                }
                double distance = waystone.getLocation().distance(player.getLocation());
                if (distance < closestDistance && distance <= 5.0) {
                    closestDistance = distance;
                    closestWaystone = waystone;
                }
            }
        }
        if (closestWaystone == null) {
            player.sendMessage(ChatColor.RED + "No owned waystone found within 5 blocks.");
            return true;
        }
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String newName = nameBuilder.toString().trim();
        String oldName = closestWaystone.getName();
        closestWaystone.setName(newName);
        waystoneManager.saveWaystones();
        player.sendMessage(ChatColor.GREEN + "Waystone rename from '" + oldName + "' to '" + newName + "' !");
        return true;
    }

    private boolean handleWaystoneSetItem(Player player) {
        Waystone closestWaystone = null;
        double closestDistance = Double.MAX_VALUE;
        for (Waystone waystone : waystoneManager.getAllWaystones()) {
            if (waystone.getOwner().equals(player.getUniqueId().toString())) {
                if (!waystone.getLocation().getWorld().equals(player.getWorld())) {
                    continue;
                }
                double distance = waystone.getLocation().distance(player.getLocation());
                if (distance < closestDistance && distance <= 5.0) {
                    closestDistance = distance;
                    closestWaystone = waystone;
                }
            }
        }
        if (closestWaystone == null) {
            player.sendMessage(ChatColor.RED + "No owned waystone found within 5 blocks.");
            return true;
        }
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must hold an item in your main hand to set it as the waystone's item.");
            return true;
        }
        ItemStack customItem = itemInHand.clone();
        customItem.setAmount(1);
        closestWaystone.setCustomItem(customItem);
        waystoneManager.saveWaystones();
        player.sendMessage(ChatColor.GREEN + "Waystone item updated successfully!");
        return true;
    }

    private void sendWaystoneHelp(Player player) {
        player.sendMessage(ChatColor.BLUE + "=== Waystone Command ===");
        player.sendMessage(ChatColor.GOLD + "Craft recipe:");
        player.sendMessage(ChatColor.YELLOW + "  E D E    E = Ender Pearl");
        player.sendMessage(ChatColor.YELLOW + "  D L D    D = Diamond");
        player.sendMessage(ChatColor.YELLOW + "  O O O    L = Lodestone, O = Obsidian");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "/waystone list" + ChatColor.GRAY + " - List all waystones");
        player.sendMessage(ChatColor.GREEN + "/waystone info <nom>" + ChatColor.GRAY + " - Info about a waystone");
        player.sendMessage(ChatColor.GREEN + "/waystone rename <nom>" + ChatColor.GRAY + " - Rename your closest waystone (within 5 blocks)");
        player.sendMessage(ChatColor.GREEN + "/waystone setitem" + ChatColor.GRAY + " - Set the item of your closest waystone (within 5 blocks, must hold an item)");
        if (player.hasPermission("cipautils.waystone.give")) {
            player.sendMessage(ChatColor.GREEN + "/waystone give" + ChatColor.GRAY + " - Get an empty waystone item");
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.BLUE + "Usage:");
        player.sendMessage(ChatColor.GRAY + "• Craft and place a waystone to create one");
        player.sendMessage(ChatColor.GRAY + "• Right-click a waystone to open the teleport menu");
        player.sendMessage(ChatColor.GRAY + "• You can only teleport to waystones in the same world");
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
}
