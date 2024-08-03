package kami.lib.kamiblocky.commands;

import kami.lib.kamiblocky.party.Party;
import kami.lib.kamiblocky.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyAdminCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyAdminCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kamiblocky.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /partyadmin <party> <command>");
            return true;
        }

        String partyName = args[0];
        Party party = partyManager.getParty(partyName);

        if (party == null) {
            sender.sendMessage("Party not found.");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "delete":
                partyManager.removeParty(party);
                sender.sendMessage("Party " + partyName + " deleted.");
                break;

            case "kick":
                if (args.length >= 3) {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target != null) {
                        partyManager.kickMember((Player) sender, target);
                        sender.sendMessage(target.getName() + " has been kicked from party " + partyName);
                    } else {
                        sender.sendMessage("Player not found.");
                    }
                } else {
                    sender.sendMessage("Usage: /partyadmin <party> kick <player>");
                }
                break;

            case "demote":
                if (args.length >= 3) {
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target != null) {
                        partyManager.demoteMember((Player) sender, target);
                        sender.sendMessage(target.getName() + " has been demoted in party " + partyName);
                    } else {
                        sender.sendMessage("Player not found.");
                    }
                } else {
                    sender.sendMessage("Usage: /partyadmin <party> demote <player>");
                }
                break;

            default:
                sender.sendMessage("Unknown admin command.");
                break;
        }

        return true;
    }
}
