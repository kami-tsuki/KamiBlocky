package kami.lib.kamiblocky.commands;

import kami.lib.kamiblocky.KamiBlocky;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {

    private final KamiBlocky plugin;

    public ToggleCommand(KamiBlocky plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /toggle <feature>");
            return true;
        }

        Player player = (Player) sender;
        String feature = args[0].toLowerCase();

        switch (feature) {
            case "hidehandvalue":
                boolean hideHandValue = !plugin.getConfig().getBoolean("features.hideHandValue", false);
                plugin.getConfig().set("features.hideHandValue", hideHandValue);
                plugin.saveConfig();
                player.sendMessage("Hide hand value feature is now " + (hideHandValue ? "enabled" : "disabled"));
                break;

            default:
                player.sendMessage("Unknown feature: " + feature);
                break;
        }
        return true;
    }
}
