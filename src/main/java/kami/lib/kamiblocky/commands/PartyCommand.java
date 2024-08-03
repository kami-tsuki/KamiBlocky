package kami.lib.kamiblocky.commands;

import kami.lib.kamiblocky.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use party commands.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /party <command>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length >= 2) {
                    partyManager.createParty(player, args[1]);
                } else {
                    player.sendMessage("Usage: /party create <name>");
                }
                break;

            case "join":
                if (args.length >= 2) {
                    partyManager.joinParty(player, args[1]);
                } else {
                    player.sendMessage("Usage: /party join <name>");
                }
                break;

            case "leave":
                partyManager.leaveParty(player);
                break;

            case "delete":
                if (args.length >= 2) {
                    partyManager.deleteParty(player, args[1]);
                } else {
                    player.sendMessage("Usage: /party delete <name>");
                }
                break;

            case "info":
                partyManager.partyInfo(player);
                break;

            case "list":
                partyManager.listParties(player);
                break;

            case "promote":
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        partyManager.promoteMember(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                } else {
                    player.sendMessage("Usage: /party promote <player>");
                }
                break;

            case "demote":
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        partyManager.demoteMember(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                } else {
                    player.sendMessage("Usage: /party demote <player>");
                }
                break;

            case "kick":
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        partyManager.kickMember(player, target);
                    } else {
                        player.sendMessage("Player not found.");
                    }
                } else {
                    player.sendMessage("Usage: /party kick <player>");
                }
                break;

            default:
                player.sendMessage("Unknown command.");
                break;
        }

        return true;
    }
}
