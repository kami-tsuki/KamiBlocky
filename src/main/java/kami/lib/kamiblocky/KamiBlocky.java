package kami.lib.kamiblocky;

import kami.lib.kamiblocky.commands.*;
import kami.lib.kamiblocky.commands.completer.ToggleTabCompleter;
import kami.lib.kamiblocky.extensions.WorldSmeltingRecipes;
import kami.lib.kamiblocky.handlers.BlockHeatHandler;
import kami.lib.kamiblocky.listeners.TimerListener;
import kami.lib.kamiblocky.party.PartyManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class KamiBlocky extends JavaPlugin {

    private FileConfiguration config;
    private PartyManager partyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!getConfig().contains("features.hideHandValue")) {
            getConfig().set("features.hideHandValue", false);
            saveConfig();
        }
        this.config = getConfig();
        File dataFile = new File(getDataFolder(), "parties.json");
        partyManager = new PartyManager(getLogger(), dataFile);
        copyResource("recipes/worldsmelting.yml", "recipes/worldsmelting.yml");
        WorldSmeltingRecipes.loadRecipes(new File(getDataFolder(), "recipes/worldsmelting.yml"));

        this.getCommand("party").setExecutor(new PartyCommand(partyManager));
        this.getCommand("partyadmin").setExecutor(new PartyAdminCommand(partyManager));
        this.getCommand("toggle").setExecutor(new ToggleCommand(this));
        this.getCommand("toggle").setTabCompleter(new ToggleTabCompleter());

        var blockHeatHandler = new BlockHeatHandler(this);
        getCommand("heat").setExecutor(new HeatCommand(blockHeatHandler));

        CountdownCommand countdownCommand = new CountdownCommand();
        this.getCommand("countdown").setExecutor(countdownCommand);
        this.getCommand("countdown").setTabCompleter(countdownCommand);

        TimerCommand timerCommand = new TimerCommand();
        getCommand("timer").setExecutor(timerCommand);
        getCommand("timer").setTabCompleter(timerCommand);

        ExplosionCommand explCommand = new ExplosionCommand(blockHeatHandler);
        this.getCommand("explosion").setExecutor(explCommand);
        this.getCommand("explosion").setTabCompleter(explCommand);

        initGameModes();
        initPartySystem();
        initStorage();

        getServer().getPluginManager().registerEvents(new TimerListener(timerCommand), this);
        getServer().getPluginManager().registerEvents(new kami.lib.kamiblocky.listeners.HandValueListener(this), this);
    }

    @Override
    public void onDisable() {
        this.saveConfig();
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    private void initGameModes() {
        // Placeholder for game mode initialization
    }

    private void initPartySystem() {
        // Placeholder for party system initialization
    }

    private void initStorage() {
        // Placeholder for storage initialization
    }

    private void copyResource(String resourcePath, String targetPath) {
        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Resource not found: " + resourcePath);
                return;
            }
            Path target = Paths.get(getDataFolder().toString(), targetPath);
            if (!Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            getLogger().severe("Failed to copy resource: " + resourcePath);
            e.printStackTrace();
        }
    }


}
