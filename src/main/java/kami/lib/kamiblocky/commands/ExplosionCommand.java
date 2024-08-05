package kami.lib.kamiblocky.commands;

import kami.lib.kamiblocky.handlers.BlockHeatHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExplosionCommand implements CommandExecutor, TabCompleter {

    private final int MAX_BLOCKS_PER_TICK = 500;
    private final int MAX_PREPROCESS_BLOCKS = 2000;
    private final long DELAY_BETWEEN_TICKS = 1L;
    private final long LIQUID_CHECK_INTERVAL = 100L; // Tick interval for checking liquids
    private final BlockHeatHandler blockHeatHandler;

    public ExplosionCommand(BlockHeatHandler blockHeatHandler) {
        this.blockHeatHandler = blockHeatHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;
        Location playerLocation = player.getLocation();

        double x = playerLocation.getX();
        double y = playerLocation.getY();
        double z = playerLocation.getZ();
        int seconds = 30;
        int radius = 10;
        int damage = 100;
        int dropChance = 50;
        boolean nuke = false;

        try {
            if (args.length >= 1) x = Double.parseDouble(args[0]);
            if (args.length >= 2) y = Double.parseDouble(args[1]);
            if (args.length >= 3) z = Double.parseDouble(args[2]);
            if (args.length >= 4) seconds = Integer.parseInt(args[3]);
            if (args.length >= 5) radius = Integer.parseInt(args[4]);
            if (args.length >= 6) damage = Integer.parseInt(args[5]);
            if (args.length >= 7) dropChance = Integer.parseInt(args[6]);
            if (args.length == 8) nuke = Boolean.parseBoolean(args[7]);

            if (dropChance < 0 || dropChance > 100) {
                sender.sendMessage("Drop chance must be between 0 and 100.");
                return false;
            }

            if (damage < 0 || damage > 100000) {
                sender.sendMessage("Damage must be between 0 and 100000.");
                return false;
            }

            Location explosionLocation = new Location(player.getWorld(), x, y, z);
            startExplosion(explosionLocation, seconds, radius, damage, dropChance, nuke, player);

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number format. Please check your inputs.");
            return false;
        }

        return true;
    }

    private void startExplosion(Location location, int seconds, int radius, int damage, int dropChance, boolean nuke, Player player) {
        player.sendMessage("§aExplosion starting at (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ") in " + seconds + " seconds!");

        removeNonSourceLiquids(location, radius); // Remove non-source liquids at the beginning

        new BukkitRunnable() {
            @Override
            public void run() {
                location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 1.0f);
                location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0, 0, 0, 0.1);

                Map<Integer, Queue<Location>> blocksByYLevel = getBlocksGroupedByYLevel(location, radius);
                preprocessLiquids(blocksByYLevel, location, radius, damage);
                new BlockProcessorTask(blocksByYLevel, damage, dropChance, nuke, radius, location).runTaskTimer(Bukkit.getPluginManager().getPlugin("KamiBlocky"), 0L, DELAY_BETWEEN_TICKS);
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("KamiBlocky"), 20L * seconds);

        new BukkitRunnable() {
            @Override
            public void run() {
                removeNonSourceLiquids(location, radius); // Remove non-source liquids at the end
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("KamiBlocky"), 20L * (seconds + 1)); // Run after the explosion duration
    }

    private void removeNonSourceLiquids(Location center, int radius) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Location loc = center.clone().add(x, y, z);
                            if (loc.distance(center) <= radius) {
                                Block block = loc.getBlock();
                                if (isNonSourceLiquid(block)) {
                                    block.setType(Material.AIR); // Remove the block
                                }
                            }
                        }
                    }
                }
            }
        }.runTask(Bukkit.getPluginManager().getPlugin("KamiBlocky"));
    }

    private boolean isNonSourceLiquid(Block block) {
        Material type = block.getType();
        return (type == Material.WATER || type == Material.LAVA) && !block.getBlockData().getAsString().contains("level=0");
    }

    private void preprocessLiquids(Map<Integer, Queue<Location>> blocksByYLevel, @NotNull Location explosionCenter, int radius, int damage) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // remove non source blocks:
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Location loc = explosionCenter.clone().add(x, y, z);
                            if (loc.distance(explosionCenter) <= radius) {
                                Block block = loc.getBlock();
                                if (isLiquid(block) && !block.getBlockData().getAsString().contains("level=0")) {
                                    block.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            }
        }.runTask(Bukkit.getPluginManager().getPlugin("KamiBlocky"));
    }


    private boolean isLiquid(Block block) {
        Material type = block.getType();
        return type == Material.WATER || type == Material.LAVA;
    }

    private Map<Integer, Queue<Location>> getBlocksGroupedByYLevel(Location center, int radius) {
        Map<Integer, Queue<Location>> blocksByYLevel = new TreeMap<>(Collections.reverseOrder());

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) <= radius) {
                        Block block = loc.getBlock();
                        if (shouldProcessBlock(block)) {
                            int yLevel = loc.getBlockY();
                            blocksByYLevel.computeIfAbsent(yLevel, k -> new LinkedList<>()).add(loc);
                        }
                    }
                }
            }
        }

        return blocksByYLevel;
    }

    private boolean shouldProcessBlock(Block block) {
        Material type = block.getType();
        if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
            return false;
        }

        if (type == Material.WATER || type == Material.LAVA) {
            return block.getBlockData().getAsString().contains("level=0");
        }

        return true;
    }

    private class BlockProcessorTask extends BukkitRunnable {
        private final Map<Integer, Queue<Location>> blocksByYLevel;
        private final int damage;
        private final int dropChance;
        private final boolean nuke;
        private final int radius;
        private final Location explosionCenter;
        private final int maxBlocksPerTick;
        private final long liquidCheckInterval;

        private Iterator<Map.Entry<Integer, Queue<Location>>> yLevelIterator;
        private Queue<Location> currentLevelBlocks;
        private long tickCount;

        private BlockProcessorTask(Map<Integer, Queue<Location>> blocksByYLevel, int damage, int dropChance, boolean nuke, int radius, Location explosionCenter) {
            this.blocksByYLevel = blocksByYLevel;
            this.damage = damage;
            this.dropChance = dropChance;
            this.nuke = nuke;
            this.radius = radius;
            this.explosionCenter = explosionCenter;
            this.maxBlocksPerTick = MAX_BLOCKS_PER_TICK;
            this.liquidCheckInterval = LIQUID_CHECK_INTERVAL;
            this.yLevelIterator = blocksByYLevel.entrySet().iterator();
            this.currentLevelBlocks = yLevelIterator.hasNext() ? yLevelIterator.next().getValue() : null;
            this.tickCount = 0;
        }

        @Override
        public void run() {
            if (tickCount % liquidCheckInterval == 0) {
                removeNonSourceLiquids(explosionCenter, radius); // Periodic removal of non-source liquids
            }

            if (currentLevelBlocks == null || currentLevelBlocks.isEmpty()) {
                if (yLevelIterator.hasNext()) {
                    currentLevelBlocks = yLevelIterator.next().getValue();
                } else {
                    cancel();
                    return;
                }
            }

            int processed = 0;
            while (processed < maxBlocksPerTick && currentLevelBlocks != null && !currentLevelBlocks.isEmpty()) {
                Location location = currentLevelBlocks.poll();
                Block block = location.getBlock();
                if (shouldProcessBlock(block)) {
                    double distance = location.distance(explosionCenter);
                    double temperature = calculateTemperature(distance, radius, damage);
                    blockHeatHandler.handleBlockHeat(block, temperature);
                    if (nuke) {
                        triggerNukeEffects(location);
                    }
                    applyHeatToSurroundingBlocks(location, temperature);

                    location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0.5, 0.5, 0.5, 0.05);
                }
                processed++;
            }

            if (currentLevelBlocks == null || currentLevelBlocks.isEmpty()) {
                if (!yLevelIterator.hasNext()) {
                    cancel();
                } else {
                    currentLevelBlocks = yLevelIterator.next().getValue();
                }
            }

            tickCount++;
        }

        private void applyHeatToSurroundingBlocks(Location center, double temperature) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = center.clone().add(x, y, z);
                        Block block = loc.getBlock();
                        if (shouldProcessBlock(block)) {
                            blockHeatHandler.handleBlockHeat(block, temperature);
                        }
                    }
                }
            }
        }
    }

    private double calculateTemperature(double distance, int radius, int damage) {
        double maxTemperature = damage;
        double maxDistance = radius;
        double distanceFactor = Math.min(distance / maxDistance, 1.0);
        return maxTemperature * (1 - distanceFactor);
    }

    private void triggerNukeEffects(Location location) {
        location.getWorld().spawnParticle(Particle.FLAME, location, 100, 3, 3, 3, 0.2);
        location.getWorld().createExplosion(location, 1.0F, true, true);
        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0f, 1.0f);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("~");
            completions.add(String.valueOf(((Player) sender).getLocation().getBlockX()));
        } else if (args.length == 2) {
            completions.add("~");
            completions.add(String.valueOf(((Player) sender).getLocation().getBlockY()));
        } else if (args.length == 3) {
            completions.add("~");
            completions.add(String.valueOf(((Player) sender).getLocation().getBlockZ()));
        } else if (args.length == 4) {
            completions.addAll(Arrays.asList("10", "20", "30", "60", "120"));
        } else if (args.length == 5) {
            completions.addAll(Arrays.asList("5", "10", "15", "20", "30"));
        } else if (args.length == 6) {
            completions.addAll(Arrays.asList("100", "200", "300", "400", "500", "600", "700", "800", "900", "1000"));
        } else if (args.length == 7) {
            completions.addAll(Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "100"));
        } else if (args.length == 8) {
            completions.addAll(Arrays.asList("true", "false"));
        }
        return completions;
    }
}
