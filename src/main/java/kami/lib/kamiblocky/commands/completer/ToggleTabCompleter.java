package kami.lib.kamiblocky.commands.completer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return suggestions; // No suggestions for non-players
        }

        if (args.length == 1) {
            suggestions.add("hideHandValue");
            // Add other feature names here as needed
        }

        return suggestions;
    }
}
