package kami.lib.kamiblocky.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CountdownCommand implements CommandExecutor, TabCompleter {

    private final Map<String, CountdownTask> activeCountdowns = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /countdown <time> [silent<int>] [players]");
            sender.sendMessage(ChatColor.RED + "Or: /countdown <action> [players]");
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
            default:
                try {
                    long totalSeconds = parseTime(action);
                    if (totalSeconds <= 0) {
                        sender.sendMessage(ChatColor.RED + "Invalid time. Time must be positive.");
                        return true;
                    }
                    String silentParam = args.length > 1 ? args[1] : null;
                    List<Player> targetPlayers = getTargetPlayers(args, silentParam != null ? 2 : 1);
                    startCountdown(sender, totalSeconds, targetPlayers, silentParam);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                }
                break;
        }
        return true;
    }

    private void startCountdown(CommandSender sender, long totalSeconds, List<Player> players, String silentParam) {
        String timeDisplay = formatTime(totalSeconds);
        boolean silentMode = false;
        int silentSeconds = 0;

        if (silentParam != null && silentParam.startsWith("silent")) {
            try {
                silentSeconds = silentParam.length() > 6 ? Integer.parseInt(silentParam.substring(6)) : 0;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid silent seconds format.");
                return;
            }
            silentMode = true;
        }

        for (Player player : players) {
            CountdownTask existingTask = activeCountdowns.get(player.getName());
            if (existingTask != null && existingTask.isRunning()) {
                sender.sendMessage(ChatColor.RED + "A countdown is already active for player: " + player.getName());
                continue;
            }

            CountdownTask task = new CountdownTask(totalSeconds, silentMode, silentSeconds, player);
            task.runTaskTimer(Bukkit.getPluginManager().getPlugin("KamiBlocky"), 0, 20);
            activeCountdowns.put(player.getName(), task);
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Countdown started for " + player.getName() + " with " + timeDisplay + ".");
        }
    }

    private void handleAction(CommandSender sender, String action, String[] args) {
        List<Player> targetPlayers = getTargetPlayers(args, 1);

        for (Player player : targetPlayers) {
            CountdownTask task = activeCountdowns.get(player.getName());

            if (task == null) {
                sender.sendMessage(ChatColor.RED + "No active countdown for " + player.getName() + ".");
                continue;
            }

            switch (action) {
                case "stop":
                    task.cancel();
                    activeCountdowns.remove(player.getName());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Countdown stopped for " + player.getName() + ".");
                    break;
                case "pause":
                    task.pause();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Countdown paused for " + player.getName() + ".");
                    break;
                case "continue":
                    task.continueCountdown();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Countdown continued for " + player.getName() + ".");
                    break;
                case "reset":
                    task.reset();
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Countdown reset for " + player.getName() + ".");
                    break;
            }
        }
    }

    private void showHelp(CommandSender sender) {
        ChatColor color = ChatColor.YELLOW;
        sender.sendMessage(color + "Usage: /countdown <time> [silent<int>] [players]");
        sender.sendMessage(color + "Or: /countdown <action> [players]");
        sender.sendMessage(color + "Actions: help, stop, pause, continue, reset");
        sender.sendMessage(color + "Time format: <number><d|h|m|s>, e.g., 1d2h3m4s");
        sender.sendMessage(color + "Players: @player1, @player2, or @a for all players");
        sender.sendMessage(color + "Silent mode: silent or silent<int> for specific seconds");
        sender.sendMessage(color + "Examples:");
        sender.sendMessage(color + "/countdown 1m30s");
        sender.sendMessage(color + "/countdown 2h @player1");
        sender.sendMessage(color + "/countdown stop @player1");
        sender.sendMessage(color + "/countdown pause @player1");
        sender.sendMessage(color + "/countdown continue @player1");
        sender.sendMessage(color + "/countdown reset @player1");
        sender.sendMessage(color + "/countdown 10s silent");
        sender.sendMessage(color + "/countdown 10s silent5");
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
                if (args[i].startsWith("@")) {
                    Player player = Bukkit.getPlayer(args[i].substring(1));
                    if (player != null) {
                        players.add(player);
                    } else {
                        Bukkit.getLogger().warning("Player not found: " + args[i]);
                    }
                }
            }
        }
        return players;
    }

    private long parseTime(String time) throws IllegalArgumentException {
        long totalSeconds = 0;
        Matcher matcher = Pattern.compile("(\\d+)([dhms])").matcher(time);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2).toLowerCase()) {
                case "d":
                    totalSeconds += value * 86400L;
                    break;
                case "h":
                    totalSeconds += value * 3600L;
                    break;
                case "m":
                    totalSeconds += value * 60L;
                    break;
                case "s":
                    totalSeconds += value;
                    break;
            }
        }

        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("Invalid time format. Use format like '1d2h3m4s'.");
        }

        return totalSeconds;
    }

    private String formatTime(long seconds) {
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("help");
            suggestions.add("stop");
            suggestions.add("pause");
            suggestions.add("continue");
            suggestions.add("reset");
            suggestions.add("3d");
            suggestions.add("3h");
            suggestions.add("3m");
            suggestions.add("3s");
        } else if (args.length == 2 && Arrays.asList("stop", "pause", "continue", "reset").contains(args[0])) {
            suggestions.add("@a");
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add("@" + player.getName());
            }
        } else if (args.length > 1 && args[0].matches("\\d+[dhms]")) {
            suggestions.add("silent");
            suggestions.add("silent3");
        }
        return suggestions;
    }

    private static class CountdownTask extends BukkitRunnable {
        private final long totalSeconds;
        private final boolean silentMode;
        private final int silentSeconds;
        private final Player player;
        private long remaining;
        private boolean paused;

        public CountdownTask(long totalSeconds, boolean silentMode, int silentSeconds, Player player) {
            this.totalSeconds = totalSeconds;
            this.silentMode = silentMode;
            this.silentSeconds = silentSeconds;
            this.player = player;
            this.remaining = totalSeconds;
            this.paused = false;
        }

        @Override
        public void run() {
            if (remaining < 0) {
                cancel();
                return;
            }

            if (paused) return;

            ChatColor color = getColorForRemaining(remaining);
            player.sendActionBar(color + formatTime(remaining));

            if (!silentMode || remaining <= silentSeconds) {
                if (remaining == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                }
            }

            remaining--;
        }

        public boolean isRunning() {
            return !paused && remaining >= 0;
        }

        public void pause() {
            this.paused = true;
        }

        public void continueCountdown() {
            this.paused = false;
        }

        public void reset() {
            this.remaining = totalSeconds;
        }

        private ChatColor getColorForRemaining(long remaining) {
            if (remaining <= 3) {
                return ChatColor.RED;
            } else if (remaining <= 10) {
                return ChatColor.GOLD;
            } else {
                return ChatColor.GREEN;
            }
        }

        private String formatTime(long seconds) {
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
}
