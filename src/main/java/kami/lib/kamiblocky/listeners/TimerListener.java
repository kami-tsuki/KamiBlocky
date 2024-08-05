package kami.lib.kamiblocky.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import kami.lib.kamiblocky.commands.TimerCommand;

import java.util.Map;

public class TimerListener implements Listener {

    private final TimerCommand timerCommand;

    public TimerListener(TimerCommand timerCommand) {
        this.timerCommand = timerCommand;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        String entityName = entity.getType().name();
        checkCondition("onKill", entityName);
    }

    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        checkCondition("onAchievement", player.getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // You might want to check on player join for some reason.
    }

    private void checkCondition(String action, String entityName) {
        TimerCommand.TimerConfig config = timerCommand.timerConfigs.get(action);
        if (config != null && config.getType().equalsIgnoreCase(entityName)) {
            // Handle timer condition logic
            for (Map.Entry<String, TimerCommand.TimerTask> entry : timerCommand.activeTimers.entrySet()) {
                TimerCommand.TimerTask task = entry.getValue();
                // Example logic, adjust according to your needs
                if (config.getScope().equals("allPlayers")) {
                    task.reset();
                }
                // Handle more specific cases here
            }
        }
    }
}
