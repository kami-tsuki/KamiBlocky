package kami.lib.kamiblocky.party;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Party {
    private final String name;
    private final UUID owner;
    private final Map<UUID, PartyRole> members;

    public Party(String name, Player owner) {
        this.name = name;
        this.owner = owner.getUniqueId();
        this.members = new HashMap<>();
        this.members.put(owner.getUniqueId(), PartyRole.OWNER);
    }

    private Party(String name, UUID owner, Map<UUID, PartyRole> members) {
        this.name = name;
        this.owner = owner;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public Map<UUID, PartyRole> getMembers() {
        return members;
    }

    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(owner);
    }

    public void addMember(Player player, PartyRole role) {
        members.put(player.getUniqueId(), role);
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public boolean isMember(Player player) {
        return members.containsKey(player.getUniqueId());
    }

    public PartyRole getRole(Player player) {
        return members.get(player.getUniqueId());
    }

    public int countRoleMembers(PartyRole role) {
        return (int) members.values().stream().filter(r -> r == role).count();
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("owner", owner.toString());

        JsonArray membersArray = new JsonArray();
        for (Map.Entry<UUID, PartyRole> entry : members.entrySet()) {
            JsonObject memberObject = new JsonObject();
            memberObject.addProperty("uuid", entry.getKey().toString());
            memberObject.addProperty("role", entry.getValue().name());
            membersArray.add(memberObject);
        }
        jsonObject.add("members", membersArray);
        return jsonObject;
    }

    public static Party fromJson(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();
        UUID owner = UUID.fromString(jsonObject.get("owner").getAsString());

        Map<UUID, PartyRole> members = new HashMap<>();
        JsonArray membersArray = jsonObject.get("members").getAsJsonArray();
        for (JsonElement element : membersArray) {
            JsonObject memberObject = element.getAsJsonObject();
            UUID uuid = UUID.fromString(memberObject.get("uuid").getAsString());
            PartyRole role = PartyRole.valueOf(memberObject.get("role").getAsString());
            members.put(uuid, role);
        }

        return new Party(name, owner, members);
    }
}
