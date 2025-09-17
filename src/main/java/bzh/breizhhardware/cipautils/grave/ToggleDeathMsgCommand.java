package bzh.breizhhardware.cipautils.grave;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleDeathMsgCommand implements CommandExecutor {
    private final GraveListener graveListener;

    public ToggleDeathMsgCommand(GraveListener graveListener) {
        this.graveListener = graveListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is reserved for players.");
            return true;
        }
        Player player = (Player) sender;
        boolean isDisabled = graveListener.isDeathMsgDisabled(player.getUniqueId());
        if (isDisabled) {
            graveListener.setDeathMsgDisabled(player.getUniqueId(), false);
            player.sendMessage("§aDeath message is now enabled!");
        } else {
            graveListener.setDeathMsgDisabled(player.getUniqueId(), true);
            player.sendMessage("§cDeath message is now disabled!");
        }
        return true;
    }
}
