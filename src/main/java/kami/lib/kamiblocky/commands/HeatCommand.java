package kami.lib.kamiblocky.commands;

import kami.lib.kamiblocky.handlers.BlockHeatHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HeatCommand implements CommandExecutor, TabCompleter {

    private final BlockHeatHandler blockHeatHandler;

    public HeatCommand(BlockHeatHandler blockHeatHandler) {
        this.blockHeatHandler = blockHeatHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /heat <x> <y> <z> <temperature>");
            return false;
        }
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "Only players or console can use this command.");
            return false;
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            double temperature = Double.parseDouble(args[3]);

            // Get the world (assumes command is used in the world the player is in)
            Player player = (Player) sender;
            Block block = player.getWorld().getBlockAt(x, y, z);

            blockHeatHandler.handleBlockHeat(block, temperature);

            sender.sendMessage(ChatColor.GREEN + "Heat applied to block at (" + x + ", " + y + ", " + z + ") with temperature " + temperature + "°C.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        if (args.length == 1) {
            completions.add(String.valueOf(loc.getBlockX()));
            completions.add("~");
        } else if (args.length == 2) {
            completions.add(String.valueOf(loc.getBlockY()));
            completions.add("~");
        } else if (args.length == 3) {
            completions.add(String.valueOf(loc.getBlockZ()));
            completions.add("~");
        } else if (args.length == 4) {
            completions.add("100"); // Example temperature values
            completions.add("200");
            completions.add("300");
            completions.add("400");
            completions.add("500");
        }

        return completions;
    }
}
