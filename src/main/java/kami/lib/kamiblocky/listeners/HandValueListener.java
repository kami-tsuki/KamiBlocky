package kami.lib.kamiblocky.listeners;

import kami.lib.kamiblocky.KamiBlocky;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class HandValueListener implements Listener {

    private final KamiBlocky plugin;
    private final Set<Player> hiddenHandPlayers = new HashSet<>();

    public HandValueListener(KamiBlocky plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("features.hideHandValue", false)) {
            hiddenHandPlayers.add(player);
            new HandUpdateTask(player).runTaskTimer(plugin, 0L, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hiddenHandPlayers.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("features.hideHandValue", false)) {
            hiddenHandPlayers.add(player);
        }
    }

    private void updateHandVisibility(Player player) {
        ItemStack air = new ItemStack(Material.AIR);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.sendEquipmentChange(player, EquipmentSlot.HAND, air);
            }
        }
    }

    private class HandUpdateTask extends BukkitRunnable {
        private final Player player;

        public HandUpdateTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (!hiddenHandPlayers.contains(player) || !player.isOnline()) {
                cancel();
                return;
            }
            updateHandVisibility(player);
        }
    }
}
