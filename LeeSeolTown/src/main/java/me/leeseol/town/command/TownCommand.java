package me.leeseol.town.command;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ChatMode;
import me.leeseol.town.model.NationType;
import me.leeseol.town.model.Town;
import me.leeseol.town.service.TownService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TownCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolTownPlugin plugin;
    private final TownService townService;

    public TownCommand(LeeSeolTownPlugin plugin, TownService townService) {
        this.plugin = plugin;
        this.townService = townService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseoltown.use")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> create(sender, args);
            case "invite" -> invite(sender, args);
            case "accept" -> accept(sender, args);
            case "deny", "reject" -> deny(sender, args);
            case "join" -> join(sender, args);
            case "leave" -> playerOnly(sender, player -> townService.leaveTown(player));
            case "disband" -> playerOnly(sender, player -> townService.disbandTown(player));
            case "transfer" -> transfer(sender, args);
            case "kick" -> kick(sender, args);
            case "claim" -> playerOnly(sender, player -> townService.claimChunk(player));
            case "unclaim" -> playerOnly(sender, player -> townService.unclaimChunk(player));
            case "info" -> info(sender, args);
            case "me", "status", "소속" -> playerOnly(sender, player -> {
                townService.sendSelfInfo(player);
                return true;
            });
            case "chat" -> chat(sender, args);
            case "nation" -> nation(sender, args);
            case "federation" -> federation(sender, args);
            case "war" -> war(sender, args);
            case "reload" -> reload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party create <name>");
            return;
        }
        playerOnly(sender, player -> townService.createTown(player, args[1]));
    }

    private void invite(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party invite <player>");
            return;
        }
        playerOnly(sender, player -> townService.invite(player, args[1]));
    }

    private void join(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party join <party>");
            return;
        }
        playerOnly(sender, player -> townService.joinTown(player, args[1]));
    }

    private void accept(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party accept <party>");
            return;
        }
        playerOnly(sender, player -> townService.acceptInvite(player, args[1]));
    }

    private void deny(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party deny <party>");
            return;
        }
        playerOnly(sender, player -> townService.denyInvite(player, args[1]));
    }

    private void transfer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party transfer <player>");
            return;
        }
        playerOnly(sender, player -> townService.transferLeader(player, args[1]));
    }

    private void kick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party kick <player>");
            return;
        }
        playerOnly(sender, player -> townService.kickMember(player, args[1]));
    }

    private void info(CommandSender sender, String[] args) {
        Town town = null;
        if (args.length >= 2) {
            String id = me.leeseol.town.storage.TownStore.idFromName(args[1]);
            town = townService.towns().stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
        } else if (sender instanceof Player player) {
            town = townService.playerTown(player);
        }

        if (town == null) {
            sender.sendMessage(plugin.msg("town-not-found").replace("%town%", args.length >= 2 ? args[1] : "-"));
            return;
        }
        sender.sendMessage(townService.info(town));
    }

    private void chat(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/party chat <global|party|nation>");
            return;
        }
        ChatMode mode = ChatMode.parse(args[1]);
        if (mode == null) {
            sender.sendMessage("/party chat <global|party|nation>");
            return;
        }
        playerOnly(sender, player -> {
            townService.setChatMode(player, mode);
            return true;
        });
    }

    private void nation(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("disband")) {
            playerOnly(sender, player -> townService.disbandNation(player));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("pvp")) {
            Boolean enabled = parseToggle(args[2]);
            if (enabled == null) {
                sender.sendMessage("/party nation pvp <on|off>");
                return;
            }
            playerOnly(sender, player -> townService.setNationPvp(player, enabled));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("build")) {
            Boolean enabled = parseToggle(args[2]);
            if (enabled == null) {
                sender.sendMessage("/party nation build <on|off>");
                return;
            }
            playerOnly(sender, player -> townService.setNationBuildProtection(player, enabled));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("treasury")) {
            playerOnly(sender, player -> townService.sendNationTreasury(player));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("deposit")) {
            Double amount = parseAmount(args[2]);
            if (amount == null) {
                sender.sendMessage("/party nation deposit <amount>");
                return;
            }
            playerOnly(sender, player -> townService.depositNationTreasury(player, amount));
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("create")) {
            sender.sendMessage("/party nation create <name> <republic|empire> [party...]");
            sender.sendMessage("/party nation disband");
            sender.sendMessage("/party nation pvp <on|off>");
            sender.sendMessage("/party nation build <on|off>");
            sender.sendMessage("/party nation treasury");
            sender.sendMessage("/party nation deposit <amount>");
            return;
        }
        NationType type = NationType.parse(args[3]);
        List<String> extraParties = args.length <= 4 ? List.of() : Arrays.asList(args).subList(4, args.length);
        playerOnly(sender, player -> townService.createNation(player, args[2], type, extraParties));
    }

    private void federation(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("disband")) {
            playerOnly(sender, player -> townService.disbandNation(player));
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("create")) {
            sender.sendMessage("/party federation create <name> <party1> [party2] [party3] [...]");
            sender.sendMessage("/party federation disband");
            return;
        }
        playerOnly(sender, player -> townService.createFederation(player, args[2], Arrays.asList(args).subList(3, args.length)));
    }

    private void war(CommandSender sender, String[] args) {
        if (args.length >= 3 && args[1].equalsIgnoreCase("declare")) {
            playerOnly(sender, player -> townService.declareWar(player, args[2]));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("accept")) {
            playerOnly(sender, player -> townService.acceptWar(player, args[2]));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("surrender")) {
            playerOnly(sender, player -> townService.surrenderWar(player, args[2]));
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("release")) {
            playerOnly(sender, player -> townService.releaseWarProtection(player, args[2]));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("paydebt")) {
            playerOnly(sender, player -> townService.payWarDebt(player));
            return;
        }
        if (args.length >= 4 && args[1].equalsIgnoreCase("finish")) {
            playerOnly(sender, player -> townService.finishWar(player, args[2], args[3]));
            return;
        }
        sender.sendMessage("/party war declare <nation>");
        sender.sendMessage("/party war accept <attackerNation>");
        sender.sendMessage("/party war surrender <enemyNation>");
        sender.sendMessage("/party war release <enemyNation>");
        sender.sendMessage("/party war paydebt");
        if (sender.hasPermission("leeseoltown.admin")) {
            sender.sendMessage("/party war finish <winnerNation> <loserNation>");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("leeseoltown.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(plugin.msg("reloaded"));
    }

    private void playerOnly(CommandSender sender, PlayerAction action) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return;
        }
        action.run(player);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/party create <name>");
        sender.sendMessage("/party invite <player>");
        sender.sendMessage("/party accept <party>");
        sender.sendMessage("/party deny <party>");
        sender.sendMessage("/party join <party>");
        sender.sendMessage("/party leave");
        sender.sendMessage("/party transfer <player>");
        sender.sendMessage("/party kick <player>");
        sender.sendMessage("/party claim");
        sender.sendMessage("/party unclaim");
        sender.sendMessage("/party me");
        sender.sendMessage("/party chat <global|party|nation>");
        sender.sendMessage("/party nation create <name> <republic|empire> [party...]");
        sender.sendMessage("/party nation disband");
        sender.sendMessage("/party nation pvp <on|off>");
        sender.sendMessage("/party nation build <on|off>");
        sender.sendMessage("/party nation treasury");
        sender.sendMessage("/party nation deposit <amount>");
        sender.sendMessage("/party war declare <nation>");
        sender.sendMessage("/party war accept <attackerNation>");
        sender.sendMessage("/party war surrender <enemyNation>");
        sender.sendMessage("/party war release <enemyNation>");
        sender.sendMessage("/party war paydebt");
        if (sender.hasPermission("leeseoltown.admin")) {
            sender.sendMessage("/party federation create <name> <party1> [party2] [party3] [...]");
            sender.sendMessage("/party federation disband");
            sender.sendMessage("/party war finish <winnerNation> <loserNation>");
            sender.sendMessage("/party reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("create", "invite", "accept", "deny", "join", "leave", "disband", "transfer", "kick", "claim", "unclaim", "info", "me", "chat", "nation", "war"));
            if (sender.hasPermission("leeseoltown.admin")) {
                options.addAll(List.of("federation", "reload"));
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && List.of("invite", "transfer", "kick").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> starts(name, args[1])).toList();
        }
        if (args.length == 2 && List.of("join", "accept", "deny", "info").contains(args[0].toLowerCase())) {
            return townService.towns().stream().map(Town::name).filter(name -> starts(name, args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("chat")) {
            return filter(List.of("global", "party", "town", "nation"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("nation")) {
            return filter(List.of("create", "disband", "pvp", "build", "treasury", "deposit"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("nation") && List.of("pvp", "build").contains(args[1].toLowerCase())) {
            return filter(List.of("on", "off"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("nation") && args[1].equalsIgnoreCase("create")) {
            return filter(List.of("republic", "empire"), args[3]);
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("nation") && args[1].equalsIgnoreCase("create")) {
            return townService.towns().stream().map(Town::name).filter(name -> starts(name, args[args.length - 1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("federation")) {
            return filter(List.of("create", "disband"), args[1]);
        }
        if (args.length >= 4 && args[0].equalsIgnoreCase("federation") && args[1].equalsIgnoreCase("create")) {
            return townService.towns().stream().map(Town::name).filter(name -> starts(name, args[args.length - 1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("war")) {
            List<String> options = new ArrayList<>(List.of("declare", "accept", "surrender", "release", "paydebt"));
            if (sender.hasPermission("leeseoltown.admin")) {
                options.add("finish");
            }
            return filter(options, args[1]);
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("war") && List.of("declare", "accept", "surrender", "release", "finish").contains(args[1].toLowerCase())) {
            return townService.nations().stream().map(nation -> nation.name()).filter(name -> starts(name, args[args.length - 1])).toList();
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        return values.stream().filter(value -> starts(value, input)).toList();
    }

    private boolean starts(String value, String input) {
        return value.toLowerCase().startsWith(input.toLowerCase());
    }

    private Boolean parseToggle(String input) {
        if (input == null) {
            return null;
        }
        String value = input.toLowerCase();
        if (List.of("on", "true", "enable", "enabled", "켜기").contains(value)) {
            return true;
        }
        if (List.of("off", "false", "disable", "disabled", "끄기").contains(value)) {
            return false;
        }
        return null;
    }

    private Double parseAmount(String input) {
        try {
            double amount = Double.parseDouble(input.replace(",", ""));
            return amount > 0.0D ? amount : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @FunctionalInterface
    private interface PlayerAction {
        boolean run(Player player);
    }
}
