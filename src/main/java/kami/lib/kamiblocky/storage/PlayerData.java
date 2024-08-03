package kami.lib.kamiblocky.storage;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final Map<UUID, Map<String, Object>> playerData = new HashMap<>();

    public void setPlayerData(Player player, String key, Object value) {
        playerData.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(key, value);
    }

    public Object getPlayerData(Player player, String key) {
        return playerData.getOrDefault(player.getUniqueId(), new HashMap<>()).get(key);
    }

    public boolean hasPlayerData(Player player, String key) {
        return playerData.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(key);
    }
}
