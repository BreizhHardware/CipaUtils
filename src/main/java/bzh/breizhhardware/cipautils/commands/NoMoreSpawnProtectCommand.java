package bzh.breizhhardware.cipautils.commands;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NoMoreSpawnProtectCommand implements CommandExecutor {
    private final Main plugin;

    public NoMoreSpawnProtectCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return plugin.handleSpawnProtectCommand(sender, args);
    }
}

