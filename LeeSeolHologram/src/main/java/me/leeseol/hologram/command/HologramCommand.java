package me.leeseol.hologram.command;

import me.leeseol.hologram.LeeSeolHologramPlugin;
import me.leeseol.hologram.model.Hologram;
import me.leeseol.hologram.service.HologramService;
import me.leeseol.hologram.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HologramCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolHologramPlugin plugin;
    private final HologramService hologramService;

    public HologramCommand(LeeSeolHologramPlugin plugin, HologramService hologramService) {
        this.plugin = plugin;
        this.hologramService = hologramService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolhologram.admin")) {
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
            case "delete", "remove" -> delete(sender, args);
            case "movehere", "move" -> moveHere(sender, args);
            case "addline", "add" -> addLine(sender, args);
            case "setline", "set" -> setLine(sender, args);
            case "insertline", "insert" -> insertLine(sender, args);
            case "removeline", "delline" -> removeLine(sender, args);
            case "spacing" -> spacing(sender, args);
            case "info" -> info(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/holo create <id> [text]");
            return;
        }
        playerOnly(sender, player -> hologramService.create(player, args[1], join(args, 2)));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/holo delete <id>");
            return;
        }
        playerOnly(sender, player -> hologramService.delete(player, args[1]));
    }

    private void moveHere(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/holo movehere <id>");
            return;
        }
        playerOnly(sender, player -> hologramService.moveHere(player, args[1]));
    }

    private void addLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/holo addline <id> <text>");
            return;
        }
        playerOnly(sender, player -> hologramService.addLine(player, args[1], join(args, 2)));
    }

    private void setLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/holo setline <id> <line> <text>");
            return;
        }
        Integer line = parseInt(sender, args[2]);
        if (line == null) {
            return;
        }
        playerOnly(sender, player -> hologramService.setLine(player, args[1], line, join(args, 3)));
    }

    private void insertLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/holo insertline <id> <line> <text>");
            return;
        }
        Integer line = parseInt(sender, args[2]);
        if (line == null) {
            return;
        }
        playerOnly(sender, player -> hologramService.insertLine(player, args[1], line, join(args, 3)));
    }

    private void removeLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/holo removeline <id> <line>");
            return;
        }
        Integer line = parseInt(sender, args[2]);
        if (line == null) {
            return;
        }
        playerOnly(sender, player -> hologramService.removeLine(player, args[1], line));
    }

    private void spacing(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/holo spacing <id> <value>");
            return;
        }
        Double spacing = parseDouble(sender, args[2]);
        if (spacing == null) {
            return;
        }
        playerOnly(sender, player -> hologramService.setSpacing(player, args[1], spacing));
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/holo info <id>");
            return;
        }
        Hologram hologram = hologramService.hologram(args[1]);
        if (hologram == null) {
            sender.sendMessage(plugin.msg("not-found").replace("%id%", args[1]));
            return;
        }
        sender.sendMessage(Text.color("&#BEEBFF[홀로그램] &f" + hologram.id()));
        sender.sendMessage(Text.color("&7월드: &f" + hologram.worldName()
                + " &7좌표: &f" + String.format("%.2f %.2f %.2f", hologram.x(), hologram.y(), hologram.z())));
        sender.sendMessage(Text.color("&7줄 간격: &f" + String.format("%.2f", hologram.lineSpacing())
                + " &7줄 수: &f" + hologram.lines().size()));
        for (int index = 0; index < hologram.lines().size(); index++) {
            sender.sendMessage(Text.color("&8" + (index + 1) + ". &f" + hologram.lines().get(index)));
        }
    }

    private void list(CommandSender sender) {
        if (hologramService.holograms().isEmpty()) {
            sender.sendMessage(plugin.msg("list-empty"));
            return;
        }
        sender.sendMessage(Text.color("&#BEEBFF등록된 홀로그램"));
        for (Hologram hologram : hologramService.holograms()) {
            sender.sendMessage(Text.color("&7- &f" + hologram.id()
                    + " &8(" + hologram.worldName() + ", " + hologram.lines().size() + "줄)"));
        }
    }

    private void reload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(plugin.msg("reloaded"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&#BEEBFF/holo create <id> [text]"));
        sender.sendMessage(Text.color("&7/holo addline <id> <text>"));
        sender.sendMessage(Text.color("&7/holo setline <id> <line> <text>"));
        sender.sendMessage(Text.color("&7/holo insertline <id> <line> <text>"));
        sender.sendMessage(Text.color("&7/holo removeline <id> <line>"));
        sender.sendMessage(Text.color("&7/holo movehere <id>"));
        sender.sendMessage(Text.color("&7/holo spacing <id> <value>"));
        sender.sendMessage(Text.color("&7/holo delete <id>"));
        sender.sendMessage(Text.color("&7/holo list"));
        sender.sendMessage(Text.color("&7RGB 예시: &#8FD9A8어서오세요 또는 <#8EC5FF>LeeSeol"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("leeseolhologram.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("create", "delete", "movehere", "addline", "setline", "insertline", "removeline", "spacing", "info", "list", "reload"), args[0]);
        }
        if (args.length == 2 && List.of("delete", "remove", "movehere", "move", "addline", "add", "setline", "set", "insertline", "insert", "removeline", "delline", "spacing", "info").contains(args[0].toLowerCase())) {
            return hologramService.holograms().stream()
                    .map(Hologram::id)
                    .filter(value -> starts(value, args[1]))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void playerOnly(CommandSender sender, PlayerAction action) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return;
        }
        action.run(player);
    }

    private Integer parseInt(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return null;
        }
    }

    private Double parseDouble(CommandSender sender, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return null;
        }
    }

    private String join(String[] args, int start) {
        if (args.length <= start) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < args.length; index++) {
            if (index > start) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (starts(value, input)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private boolean starts(String value, String input) {
        return value.toLowerCase().startsWith(input.toLowerCase());
    }

    @FunctionalInterface
    private interface PlayerAction {
        boolean run(Player player);
    }
}
