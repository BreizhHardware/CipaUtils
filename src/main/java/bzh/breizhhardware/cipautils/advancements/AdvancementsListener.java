
package bzh.breizhhardware.cipautils.advancements;

import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

public class AdvancementsListener implements Listener, CommandExecutor {
    private static final String TRIGGER_MESSAGE = "/advancements"; // Change this to your desired trigger

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player!");
            return true;
        }

        Player player = (Player) sender;
        PlayerAdvancements playerAdvancements = gatherAdvancementProgress(player);

        // Send the results to the player
        displayAdvancementStats(player, playerAdvancements);
        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.equalsIgnoreCase(TRIGGER_MESSAGE)) {
            // Message doesn't appear in chat
            event.setCancelled(true);

            PlayerAdvancements playerAdvancements = gatherAdvancementProgress(player);

            // Send the results to the player
            displayAdvancementStats(player, playerAdvancements);
        }
    }

    private void displayAdvancementStats(Player player, PlayerAdvancements playerAdvancements) {
        player.sendMessage("§6=== Your Advancement Progress ===");
        player.sendMessage("§eCompleted: §f" + playerAdvancements.getCompletedAdvancements() +
                "§e/§f" + playerAdvancements.getTotalAdvancements());
        player.sendMessage("§eProgress: §f" +
                String.format("%.1f%%",
                        (playerAdvancements.getCompletedAdvancements() * 100.0) /
                                Math.max(1, playerAdvancements.getTotalAdvancements())));
        player.sendMessage("");

        // Display by type
        player.sendMessage("§7Regular (Task): §f" + playerAdvancements.getCompletedTasks() +
                "§7/§f" + playerAdvancements.getTotalTasks());
        player.sendMessage("§aGoals: §f" + playerAdvancements.getCompletedGoals() +
                "§a/§f" + playerAdvancements.getTotalGoals());
        player.sendMessage("§dChallenges: §f" + playerAdvancements.getCompletedChallenges() +
                "§d/§f" + playerAdvancements.getTotalChallenges());
    }

    private PlayerAdvancements gatherAdvancementProgress(Player player) {
        int totalAdvancements = 0;
        int completedAdvancements = 0;

        int totalTasks = 0;
        int completedTasks = 0;
        int totalGoals = 0;
        int completedGoals = 0;
        int totalChallenges = 0;
        int completedChallenges = 0;

        // Iterate through all registered advancements
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);

            // Count total advancements (excluding recipe advancements)
            String key = advancement.getKey().toString();
            if (!key.contains("recipes/")) {
                AdvancementDisplay display = advancement.getDisplay();

                // Only count advancements that have a display (shown in UI)
                if (display != null) {
                    totalAdvancements++;

                    boolean isCompleted = progress.isDone();
                    if (isCompleted) {
                        completedAdvancements++;
                    }

                    // Get the advancement frame type
                    AdvancementDisplay.Frame frame = display.frame();

                    // Count by type
                    switch (frame) {
                        case TASK:
                            totalTasks++;
                            if (isCompleted) {
                                completedTasks++;
                            }
                            break;
                        case GOAL:
                            totalGoals++;
                            if (isCompleted) {
                                completedGoals++;
                            }
                            break;
                        case CHALLENGE:
                            totalChallenges++;
                            if (isCompleted) {
                                completedChallenges++;
                            }
                            break;
                    }
                }
            }
        }

        PlayerAdvancements playerAdvancements = new PlayerAdvancements(totalAdvancements, completedAdvancements);
        playerAdvancements.setTotalTasks(totalTasks);
        playerAdvancements.setCompletedTasks(completedTasks);
        playerAdvancements.setTotalGoals(totalGoals);
        playerAdvancements.setCompletedGoals(completedGoals);
        playerAdvancements.setTotalChallenges(totalChallenges);
        playerAdvancements.setCompletedChallenges(completedChallenges);

        return playerAdvancements;
    }

    private String getTabKey(String advancementKey) {
        // Extract tab name from advancement key (e.g., "minecraft:story/root" -> "story")
        if (advancementKey.contains("/")) {
            String[] parts = advancementKey.split("/");
            if (parts.length >= 2 && parts[parts.length - 1].equals("root")) {
                return parts[parts.length - 2];
            }
        }
        return null;
    }
}