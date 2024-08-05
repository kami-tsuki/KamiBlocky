package kami.lib.kamiblocky.handlers;

import kami.lib.kamiblocky.extensions.WorldSmeltingRecipes;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.slf4j.ILoggerFactory;

import java.util.Random;

public class BlockHeatHandler {

    private final JavaPlugin plugin;
    private static final Random RANDOM = new Random();

    public BlockHeatHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleBlockHeat(Block block, double temperature) {
        Material inputMaterial = block.getType();

        if (inputMaterial == Material.AIR || inputMaterial == Material.CAVE_AIR || inputMaterial == Material.VOID_AIR) {
            plugin.getLogger().info("Ignoring air block at " + block.getLocation());
            return; // Ignore air blocks
        }

        if (inputMaterial == Material.WATER || inputMaterial == Material.LAVA) {
            plugin.getLogger().info("Handling fluid block " + inputMaterial.name() + " at " + block.getLocation());
            handleFluidBlock(block, inputMaterial, temperature);
            return;
        }

        Material outputMaterial = WorldSmeltingRecipes.getOutput(inputMaterial, temperature);

        if (outputMaterial == null || outputMaterial == inputMaterial) {
            plugin.getLogger().info("No smelting recipe found for " + inputMaterial.name() + " at " + temperature + "°C.");
            return;
        }

        // Replace the block with the output material
        plugin.getLogger().info("Replaced " + inputMaterial.name() + " with " + outputMaterial.name() + ".");
        block.setType(outputMaterial);

        // Spawn particles and additional effects
        spawnHeatingParticles(block.getLocation());
        applyPotionEffects(block.getLocation());
    }

    private void handleFluidBlock(Block block, Material fluidMaterial, double temperature) {
        Material outputMaterial = WorldSmeltingRecipes.getOutput(fluidMaterial, temperature);

        if (outputMaterial != null) {
            block.setType(outputMaterial);
            plugin.getLogger().info("Replaced " + fluidMaterial.name() + " with " + outputMaterial.name() + ".");
        } else {
            plugin.getLogger().info("No smelting recipe found for fluid " + fluidMaterial.name() + " at " + temperature + "°C.");
        }
    }

    private void spawnHeatingParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.FLAME, location, 20);
        world.spawnParticle(Particle.LARGE_SMOKE, location, 10);
        new BukkitRunnable() {
            @Override
            public void run() {
                world.spawnParticle(Particle.SMOKE, location, 5);
            }
        }.runTaskLater(plugin, 20L);
    }

    private void applyPotionEffects(Location location) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) < 10) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 1));
            }
        }
    }
}
