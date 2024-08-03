package kami.lib.kamiblocky.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TimerCommand implements CommandExecutor, TabCompleter {

    private final Map<String, TimerTask> activeTimers = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /timer <start|pause|continue|stop|reset|help> [player...]");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "help":
                showHelp(sender);
                break;
            case "stop":
            case "pause":
            case "continue":
            case "reset":
                handleAction(sender, action, args);
                break;
            case "start":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /timer start <player...>");
                    return true;
                }
                List<Player> targetPlayers = getTargetPlayers(args, 1);
                startTimer(sender, targetPlayers);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown action. Use /timer help for usage.");
                break;
        }
        return true;
    }

    private void startTimer(CommandSender sender, List<Player> players) {
        for (Player player : players) {
            TimerTask existingTask = activeTimers.get(player.getName());
            if (existingTask != null && existingTask.isRunning()) {
                sender.sendMessage(ChatColor.RED + "A timer is already active for player: " + player.getName());
                continue;
            }

            TimerTask task = new TimerTask(player);
            task.runTaskTimer(Bukkit.getPluginManager().getPlugin("KamiBlocky"), 0, 20);
            activeTimers.put(player.getName(), task);
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Timer started for " + player.getName() + ".");
        }
    }

    private void handleAction(CommandSender sender, String action, String[] args) {
        List<Player> targetPlayers = getTargetPlayers(args, 1);

        for (Player player : targetPlayers) {
            TimerTask task = activeTimers.get(player.getName());

            if (task == null) {
                sender.sendMessage(ChatColor.RED + "No active timer for " + player.getName() + ".");
                continue;
            }

            switch (action) {
                case "stop":
                    task.cancel();
                    activeTimers.remove(player.getName());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Timer stopped for " + player.getName() + ".");
                    break;
                case "pause":
                    task.pause();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Timer paused for " + player.getName() + ".");
                    break;
                case "continue":
                    task.continueTimer();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Timer continued for " + player.getName() + ".");
                    break;
                case "reset":
                    task.reset();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Timer reset for " + player.getName() + ".");
                    break;
            }
        }
    }

    private void showHelp(CommandSender sender) {
        ChatColor color = ChatColor.YELLOW;
        sender.sendMessage(color + "Usage: /timer <start|pause|continue|stop|reset|help> [player...]");
        sender.sendMessage(color + "Actions: start, pause, continue, stop, reset, help");
        sender.sendMessage(color + "Players: Specify player names or use @a for all online players.");
        sender.sendMessage(color + "Examples:");
        sender.sendMessage(color + "/timer start @a");
        sender.sendMessage(color + "/timer stop @player1");
        sender.sendMessage(color + "/timer pause @player2");
        sender.sendMessage(color + "/timer continue @player1");
        sender.sendMessage(color + "/timer reset @player1");
    }

    private List<Player> getTargetPlayers(String[] args, int startIndex) {
        List<Player> players = new ArrayList<>();
        if (args.length <= startIndex) {
            players.addAll(Bukkit.getOnlinePlayers());
        } else {
            for (int i = startIndex; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("@a")) {
                    players.addAll(Bukkit.getOnlinePlayers());
                    break;
                }
                Player player = Bukkit.getPlayer(args[i]);
                if (player != null) {
                    players.add(player);
                } else {
                    Bukkit.getLogger().warning("Player not found: " + args[i]);
                }
            }
        }
        return players;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("start");
            suggestions.add("pause");
            suggestions.add("continue");
            suggestions.add("stop");
            suggestions.add("reset");
            suggestions.add("help");
        } else if (args.length > 1 && Arrays.asList("start", "pause", "continue", "stop", "reset").contains(args[0])) {
            suggestions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }

    private static class TimerTask extends BukkitRunnable {
        private final Player player;
        private long elapsed;
        private boolean paused;

        public TimerTask(Player player) {
            this.player = player;
            this.elapsed = 0;
            this.paused = false;
        }

        @Override
        public void run() {
            if (paused) return;

            player.sendActionBar(formatTime(elapsed));
            elapsed++;
        }

        public boolean isRunning() {
            return !paused;
        }

        public void pause() {
            this.paused = true;
        }

        public void continueTimer() {
            this.paused = false;
        }

        public void reset() {
            this.elapsed = 0;
            this.paused = false;
        }
    }

    private static String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0) sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
