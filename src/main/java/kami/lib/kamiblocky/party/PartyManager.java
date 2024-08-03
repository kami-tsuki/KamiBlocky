package kami.lib.kamiblocky.party;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PartyManager {

    private final Map<String, Party> parties = new HashMap<>();
    private final Map<UUID, String> playerParties = new HashMap<>();
    private final Logger logger;
    private final File dataFile;
    private final Gson gson;

    public PartyManager(Logger logger, File dataFile) {
        this.logger = logger;
        this.dataFile = dataFile;
        this.gson = new Gson();
        loadParties();
    }

    public Party createParty(Player owner, String partyName) {
        if (playerParties.containsKey(owner.getUniqueId())) {
            owner.sendMessage("You are already in a party.");
            return null;
        }

        if (parties.containsKey(partyName.toLowerCase())) {
            owner.sendMessage("A party with that name already exists.");
            return null;
        }

        Party party = new Party(partyName, owner);
        parties.put(partyName.toLowerCase(), party);
        playerParties.put(owner.getUniqueId(), partyName.toLowerCase());
        owner.sendMessage("Party created: " + partyName);
        logger.info(owner.getName() + " created party: " + partyName);
        saveParties();
        return party;
    }

    public void deleteParty(Player player, String partyName) {
        Party party = getParty(partyName);
        if (party != null && party.isOwner(player)) {
            removeParty(party);
            player.sendMessage("Party " + partyName + " deleted.");
            logger.info(player.getName() + " deleted party: " + partyName);
        } else {
            player.sendMessage("You don't have permission to delete this party.");
        }
        saveParties();
    }

    public void removeParty(Party party) {
        for (UUID member : party.getMembers().keySet()) {
            playerParties.remove(member);
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                player.sendMessage("Party " + party.getName() + " has been disbanded.");
            }
        }
        parties.remove(party.getName().toLowerCase());
        logger.info("Party " + party.getName() + " removed from system.");
        saveParties();
    }

    public Party getParty(String name) {
        return parties.get(name.toLowerCase());
    }

    public Party getPlayerParty(Player player) {
        String partyName = playerParties.get(player.getUniqueId());
        return partyName != null ? parties.get(partyName.toLowerCase()) : null;
    }

    public boolean joinParty(Player player, String partyName) {
        Party party = getParty(partyName);
        if (party == null) {
            player.sendMessage("Party does not exist.");
            return false;
        }

        if (playerParties.containsKey(player.getUniqueId())) {
            player.sendMessage("You are already in a party.");
            return false;
        }

        party.addMember(player, PartyRole.MEMBER);
        playerParties.put(player.getUniqueId(), partyName.toLowerCase());
        player.sendMessage("You have joined the party: " + partyName);
        logger.info(player.getName() + " joined party: " + partyName);
        saveParties();
        return true;
    }

    public void leaveParty(Player player) {
        Party party = getPlayerParty(player);
        if (party != null) {
            party.removeMember(player);
            playerParties.remove(player.getUniqueId());
            player.sendMessage("You have left the party: " + party.getName());

            // If the owner leaves, disband the party
            if (party.isOwner(player)) {
                removeParty(party);
            }

            logger.info(player.getName() + " left party: " + party.getName());
            saveParties();
        } else {
            player.sendMessage("You are not in a party.");
        }
    }

    public void promoteMember(Player owner, Player target) {
        Party party = getPlayerParty(owner);
        if (party != null && party.isOwner(owner)) {
            if (party.isMember(target)) {
                PartyRole role = party.getRole(target);
                PartyRole nextRole = getNextRole(role);
                if (nextRole != null && party.countRoleMembers(nextRole) < nextRole.getLimit()) {
                    party.addMember(target, nextRole);
                    target.sendMessage("You have been promoted to " + nextRole.name());
                    logger.info(target.getName() + " was promoted to " + nextRole.name() + " in party: " + party.getName());
                    saveParties();
                } else {
                    owner.sendMessage("Cannot promote member to this role (role limit reached).");
                }
            } else {
                owner.sendMessage("Player is not in your party.");
            }
        } else {
            owner.sendMessage("You are not the owner of the party.");
        }
    }

    public void demoteMember(Player owner, Player target) {
        Party party = getPlayerParty(owner);
        if (party != null && party.isOwner(owner)) {
            if (party.isMember(target)) {
                PartyRole role = party.getRole(target);
                PartyRole prevRole = getPrevRole(role);
                if (prevRole != null) {
                    party.addMember(target, prevRole);
                    target.sendMessage("You have been demoted to " + prevRole.name());
                    logger.info(target.getName() + " was demoted to " + prevRole.name() + " in party: " + party.getName());
                    saveParties();
                } else {
                    owner.sendMessage("Cannot demote member further.");
                }
            } else {
                owner.sendMessage("Player is not in your party.");
            }
        } else {
            owner.sendMessage("You are not the owner of the party.");
        }
    }

    public void kickMember(Player owner, Player target) {
        Party party = getPlayerParty(owner);
        if (party != null && party.isOwner(owner)) {
            if (party.isMember(target)) {
                party.removeMember(target);
                playerParties.remove(target.getUniqueId());
                target.sendMessage("You have been kicked from the party: " + party.getName());
                logger.info(target.getName() + " was kicked from party: " + party.getName());
                saveParties();
            } else {
                owner.sendMessage("Player is not in your party.");
            }
        } else {
            owner.sendMessage("You are not the owner of the party.");
        }
    }

    public void partyInfo(Player player) {
        Party party = getPlayerParty(player);
        if (party != null) {
            player.sendMessage("Party: " + party.getName());
            player.sendMessage("Owner: " + Bukkit.getOfflinePlayer(party.getOwner()).getName());
            party.getMembers().forEach((uuid, role) ->
                    player.sendMessage("- " + Bukkit.getOfflinePlayer(uuid).getName() + ": " + role.name())
            );
        } else {
            player.sendMessage("You are not in a party.");
        }
    }

    public void listParties(Player player) {
        if (parties.isEmpty()) {
            player.sendMessage("There are no active parties.");
        } else {
            player.sendMessage("Active parties:");
            parties.keySet().forEach(partyName -> player.sendMessage("- " + partyName));
        }
    }

    private void saveParties() {
        JsonArray partiesArray = new JsonArray();
        for (Party party : parties.values()) {
            partiesArray.add(party.toJson());
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(partiesArray, writer);
        } catch (IOException e) {
            logger.severe("Failed to save party data: " + e.getMessage());
        }
    }

    private void loadParties() {
        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            JsonArray partiesArray = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : partiesArray) {
                Party party = Party.fromJson(element.getAsJsonObject());
                parties.put(party.getName().toLowerCase(), party);
                for (UUID member : party.getMembers().keySet()) {
                    playerParties.put(member, party.getName().toLowerCase());
                }
            }
            logger.info("Loaded " + parties.size() + " parties.");
        } catch (IOException e) {
            logger.severe("Failed to load party data: " + e.getMessage());
        }
    }

    private PartyRole getNextRole(PartyRole currentRole) {
        switch (currentRole) {
            case MEMBER:
                return PartyRole.MANAGER;
            case MANAGER:
                return PartyRole.OWNER;
            default:
                return null;
        }
    }

    private PartyRole getPrevRole(PartyRole currentRole) {
        switch (currentRole) {
            case OWNER:
                return PartyRole.MANAGER;
            case MANAGER:
                return PartyRole.MEMBER;
            default:
                return null;
        }
    }
}
