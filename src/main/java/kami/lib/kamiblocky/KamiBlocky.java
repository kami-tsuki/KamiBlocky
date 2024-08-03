package kami.lib.kamiblocky;

import kami.lib.kamiblocky.commands.CountdownCommand;
import kami.lib.kamiblocky.commands.PartyAdminCommand;
import kami.lib.kamiblocky.commands.PartyCommand;
import kami.lib.kamiblocky.commands.ToggleCommand;
import kami.lib.kamiblocky.commands.completer.ToggleTabCompleter;
import kami.lib.kamiblocky.party.PartyManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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

        this.getCommand("party").setExecutor(new PartyCommand(partyManager));
        this.getCommand("partyadmin").setExecutor(new PartyAdminCommand(partyManager));
        this.getCommand("toggle").setExecutor(new ToggleCommand(this));

        this.getCommand("countdown").setExecutor(new CountdownCommand());
        this.getCommand("toggle").setTabCompleter(new ToggleTabCompleter());

        initGameModes();
        initPartySystem();
        initStorage();

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
}
