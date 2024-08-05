package kami.lib.kamiblocky.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TimerCommand implements CommandExecutor, TabCompleter {

    public final Map<String, TimerTask> activeTimers = new HashMap<>();
    private final Map<String, TimerCondition> timerConditions = new HashMap<>();
    public final Map<String, TimerConfig> timerConfigs = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /timer <start|pause|continue|stop|reset|help|configure> [options...]");
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
                    sender.sendMessage(ChatColor.RED + "Usage: /timer start <player...> [options...]");
                    return true;
                }
                List<Player> targetPlayers = getTargetPlayers(args, 1);
                ChatColor color = ChatColor.WHITE;
                boolean bold = false;
                boolean underline = false;
                boolean italic = false;
                boolean strikethrough = false;
                boolean magic = false;
                for (int i = targetPlayers.isEmpty() ? 1 : 2; i < args.length; i++) {
                    String option = args[i].toLowerCase();
                    switch (option) {
                        case "bold" -> bold = true;
                        case "underline" -> underline = true;
                        case "italic" -> italic = true;
                        case "strikethrough" -> strikethrough = true;
                        case "magic" -> magic = true;
                        default -> {
                            try {
                                color = ChatColor.valueOf(option.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(ChatColor.RED + "Unknown color: " + option);
                                return true;
                            }
                        }
                    }
                }

                startTimer(sender, targetPlayers, color, bold, underline, italic, strikethrough, magic);
                break;
            case "configure":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /timer configure <action> <condition> [options...]");
                    return true;
                }
                handleConfigure(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown action. Use /timer help for usage.");
                break;
        }
        return true;
    }

    private void startTimer(CommandSender sender, List<Player> players, ChatColor color, boolean bold, boolean underline, boolean italic, boolean strikethrough, boolean magic) {
        for (Player player : players) {
            TimerTask existingTask = activeTimers.get(player.getName());
            if (existingTask != null && existingTask.isRunning()) {
                sender.sendMessage(ChatColor.RED + "A timer is already active for player: " + player.getName());
                continue;
            }

            TimerTask task = new TimerTask(player, color, bold, underline, italic, strikethrough, magic);
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

    private void handleConfigure(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /timer configure <action> <condition> [options...]");
            return;
        }

        String action = args[1].toLowerCase();
        String condition = args[2].toLowerCase();

        if (!Arrays.asList("pause", "restart", "stop").contains(action)) {
            sender.sendMessage(ChatColor.RED + "Unknown action. Valid actions are: pause, restart, stop.");
            return;
        }

        TimerCondition timerCondition;
        try {
            timerCondition = TimerCondition.valueOf(condition.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown condition. Valid conditions are: onKill, onAchievement, onDead.");
            return;
        }

        TimerConfig config = new TimerConfig(timerCondition);
        String type = args[3].toLowerCase();
        if (timerCondition == TimerCondition.ON_KILL || timerCondition == TimerCondition.ON_ACHIEVEMENT) {
            config.setType(type);
        }
        if (args.length > 4) {
            String count = args[4];
            try {
                config.setCount(Integer.parseInt(count));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid count: " + count);
                return;
            }
        }
        if (args.length > 5) {
            String scope = args[5].toLowerCase();
            if (scope.equals("perplayer") || scope.equals("allplayers")) {
                config.setScope(scope);
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid scope: " + scope);
                return;
            }
        }

        timerConfigs.put(action, config);
        sender.sendMessage(ChatColor.GREEN + "Timer condition configured: " + action + " when " + condition + " with options: " + config);
    }

    private void showHelp(CommandSender sender) {
        var color = ChatColor.YELLOW;
        sender.sendMessage(color + "Usage: /timer <start|pause|continue|stop|reset|help|configure> [player...] [options...]");
        sender.sendMessage(color + "Actions: start, pause, continue, stop, reset, help, configure");
        sender.sendMessage(color + "Players: Specify player names or use @a for all online players.");
        sender.sendMessage(color + "Options: Specify color (e.g., red, blue) and formatting (bold, underline, italic, strikethrough, magic)");
        sender.sendMessage(color + "Examples:");
        sender.sendMessage(color + "/timer start @a bold underline pink");
        sender.sendMessage(color + "/timer start @a bold purple");
        sender.sendMessage(color + "/timer start @a red");
        sender.sendMessage(color + "/timer stop @player1");
        sender.sendMessage(color + "/timer pause @player2");
        sender.sendMessage(color + "/timer continue @player1");
        sender.sendMessage(color + "/timer reset @player1");
        sender.sendMessage(color + "/timer configure pause onKill warden 5 perPlayer");
        sender.sendMessage(color + "/timer configure pause onAchievement 5 perPlayer");
        sender.sendMessage(color + "/timer configure pause onKill Player <name>");
        sender.sendMessage(color + "/timer configure pause onDead <player>");
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
            suggestions.add("configure");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("configure")) {
            suggestions.add("pause");
            suggestions.add("restart");
            suggestions.add("stop");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("configure")) {
            String action = args[1].toLowerCase();
            if (action.equals("pause") || action.equals("restart") || action.equals("stop")) {
                suggestions.add("onKill");
                suggestions.add("onAchievement");
                suggestions.add("onDead");
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure")) {
            String condition = args[2].toLowerCase();
            switch (condition) {
                case "onKill" -> {
                    suggestions.add("player");
                    for (EntityType entity : EntityType.values()) {
                        suggestions.add(entity.name());
                    }
                }
                case "onAchievement" -> {
                    // Optionally, add achievement-related suggestions here.
                }
                case "onDead" -> {
                    suggestions.add("@a");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        suggestions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("configure")) {
            String condition = args[2].toLowerCase();
            if (condition.equals("onKill") || condition.equals("onAchievement")) {
                suggestions.add("perPlayer");
                suggestions.add("allPlayers");
            }
        }

        return suggestions.stream().distinct().collect(Collectors.toList());
    }

    private enum TimerCondition {
        ON_KILL, ON_ACHIEVEMENT, ON_DEAD
    }

    public static class TimerConfig {
        private final TimerCondition condition;
        private String type;
        private int count = -1;
        private String scope;

        public TimerConfig(TimerCondition condition) {
            this.condition = condition;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "TimerConfig{" +
                    "condition=" + condition +
                    ", type='" + type + '\'' +
                    ", count=" + count +
                    ", scope='" + scope + '\'' +
                    '}';
        }

        public Object getScope() {
            return scope;
        }

        public String getType() {
            return type;
        }
    }

    public static class TimerTask extends BukkitRunnable {
        private final Player player;
        private final ChatColor color;
        private final boolean bold;
        private final boolean underline;
        private final boolean italic;
        private final boolean strikethrough;
        private final boolean magic;
        private long elapsed;
        private boolean paused;

        public TimerTask(Player player, ChatColor color, boolean bold, boolean underline, boolean italic, boolean strikethrough, boolean magic) {
            this.player = player;
            this.color = color;
            this.bold = bold;
            this.underline = underline;
            this.italic = italic;
            this.strikethrough = strikethrough;
            this.magic = magic;
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

            String timeString = sb.toString().trim();
            if (bold) timeString = ChatColor.BOLD + timeString;
            if (underline) timeString = ChatColor.UNDERLINE + timeString;
            if (italic) timeString = ChatColor.ITALIC + timeString;
            if (strikethrough) timeString = ChatColor.STRIKETHROUGH + timeString;
            if (magic) timeString = ChatColor.MAGIC + timeString;
            return color + timeString;
        }
    }
}
