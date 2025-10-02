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

import java.util.*;
import java.util.stream.Collectors;

public class AdvancementsListener implements Listener, CommandExecutor {
    private static final String TRIGGER_MESSAGE = "/advancements";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player!");
            return true;
        }

        Player player = (Player) sender;

        // Check for subcommands
        if (args.length > 0 && args[0].equalsIgnoreCase("leaderboard")) {
            displayLeaderboard(player);
            return true;
        }

        // Default behavior: show player's own stats
        PlayerAdvancements playerAdvancements = gatherAdvancementProgress(player);
        displayAdvancementStats(player, playerAdvancements);
        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.equalsIgnoreCase(TRIGGER_MESSAGE)) {
            event.setCancelled(true);
            PlayerAdvancements playerAdvancements = gatherAdvancementProgress(player);
            displayAdvancementStats(player, playerAdvancements);
        }
    }

    private void displayLeaderboard(Player requester) {
        Map<String, PlayerAdvancements> leaderboardData = new HashMap<>();

        // Gather advancement data for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerAdvancements advancements = gatherAdvancementProgress(player);
            leaderboardData.put(player.getName(), advancements);
        }

        // Sort players by completed advancements (descending)
        List<Map.Entry<String, PlayerAdvancements>> sortedEntries = leaderboardData.entrySet()
                .stream()
                .sorted((e1, e2) -> Integer.compare(
                        e2.getValue().getCompletedAdvancements(),
                        e1.getValue().getCompletedAdvancements()))
                .collect(Collectors.toList());

        // Display leaderboard
        requester.sendMessage("§6§l=== Advancements Leaderboard (Top 10) ===");
        requester.sendMessage("");

        int position = 1;
        int limit = Math.min(10, sortedEntries.size());

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, PlayerAdvancements> entry = sortedEntries.get(i);
            String playerName = entry.getKey();
            PlayerAdvancements advancements = entry.getValue();

            double percentage = (advancements.getCompletedAdvancements() * 100.0) /
                    Math.max(1, advancements.getTotalAdvancements());

            // Highlight the requester's position
            String color = playerName.equals(requester.getName()) ? "§e§l" : "§7";
            String medal = "";

            // Add medals for top 3
            switch (position) {
                case 1: medal = "§6🥇 "; break;
                case 2: medal = "§f🥈 "; break;
                case 3: medal = "§c🥉 "; break;
                default: medal = "§7" + position + ". "; break;
            }

            requester.sendMessage(medal + color + playerName + " §8- §f" +
                    advancements.getCompletedAdvancements() + "§7/§f" +
                    advancements.getTotalAdvancements() + " §7(" +
                    String.format("%.1f%%", percentage) + "§7)");

            position++;
        }

        requester.sendMessage("");
        requester.sendMessage("§7Use §e/advancements §7to see your detailed stats");
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

        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);

            String key = advancement.getKey().toString();
            if (!key.contains("recipes/")) {
                AdvancementDisplay display = advancement.getDisplay();

                if (display != null) {
                    totalAdvancements++;

                    boolean isCompleted = progress.isDone();
                    if (isCompleted) {
                        completedAdvancements++;
                    }

                    AdvancementDisplay.Frame frame = display.frame();

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
        if (advancementKey.contains("/")) {
            String[] parts = advancementKey.split("/");
            if (parts.length >= 2 && parts[parts.length - 1].equals("root")) {
                return parts[parts.length - 2];
            }
        }
        return null;
    }
}