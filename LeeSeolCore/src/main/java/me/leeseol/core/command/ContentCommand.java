package me.leeseol.core.command;

import me.leeseol.core.LeeSeolCorePlugin;
import me.leeseol.core.content.ContentArea;
import me.leeseol.core.content.ContentEntry;
import me.leeseol.core.content.ContentService;
import me.leeseol.core.content.ContentSpawn;
import me.leeseol.core.content.ContentType;
import me.leeseol.core.portal.PortalCuboidSelection;
import me.leeseol.core.portal.WorldEditSelectionProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ContentCommand implements TabExecutor {
    private final LeeSeolCorePlugin plugin;
    private final ContentService contentService;

    public ContentCommand(LeeSeolCorePlugin plugin, ContentService contentService) {
        this.plugin = plugin;
        this.contentService = contentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sender.sendMessage(color("&c권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (action) {
                case "add" -> add(sender, args);
                case "remove" -> remove(sender, args);
                case "rename" -> rename(sender, args);
                case "setradius" -> setRadius(sender, args);
                case "setspawn" -> setSpawn(sender, args);
                case "list" -> list(sender, args);
                case "tp" -> teleport(sender, args);
                default -> {
                    sendHelp(sender);
                    yield true;
                }
            };
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(color("&c" + exception.getMessage()));
            return true;
        } catch (IllegalStateException exception) {
            sender.sendMessage(color("&c" + exception.getMessage()));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return matching(args[0], List.of("add", "remove", "rename", "setradius", "setspawn", "list", "tp"));
        }
        if (args.length == 2 && usesType(args[0])) {
            return matching(args[1], typeKeys());
        }
        if (args.length == 3 && usesExistingId(args[0])) {
            Optional<ContentType> type = ContentType.fromKey(args[1]);
            return type.map(contentType -> matching(args[2], contentService.list(contentType).stream()
                    .map(ContentEntry::id)
                    .toList())).orElseGet(Collections::emptyList);
        }
        return Collections.emptyList();
    }

    private boolean add(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&c게임 안에서만 사용할 수 있습니다."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("/content add <type> <id> [displayName]");
            return true;
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            sender.sendMessage(color("&cWorldEdit 플러그인이 없어 콘텐츠 등록을 사용할 수 없습니다."));
            return true;
        }
        if (!contentService.regionServiceAvailable()) {
            sender.sendMessage(color("&cWorldGuard 플러그인이 없어 보호 region을 만들 수 없습니다."));
            return true;
        }

        ContentType type = parseType(args[1]);
        String id = args[2];
        String displayName = joinTail(args, 3);
        PortalCuboidSelection selection = resolveSelection(player);
        if (selection == null) {
            sender.sendMessage(color("&cWorldEdit 영역을 먼저 선택하세요."));
            return true;
        }

        ContentEntry entry = contentService.add(
                type,
                id,
                displayName,
                area(selection),
                spawn(player.getLocation())
        );
        sender.sendMessage(color("&a콘텐츠 등록 완료: &f" + entry.type().key() + "/" + entry.id()));
        sender.sendMessage(color("&7region: &f" + entry.regionId() + " &7radius: &f" + entry.radius()));
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/content remove <type> <id>");
            return true;
        }
        if (!contentService.regionServiceAvailable()) {
            sender.sendMessage(color("&cWorldGuard 플러그인이 없어 보호 region을 삭제할 수 없습니다."));
            return true;
        }

        ContentType type = parseType(args[1]);
        String id = args[2];
        Optional<ContentEntry> removed = contentService.remove(type, id);
        if (removed.isEmpty()) {
            sender.sendMessage(color("&c콘텐츠를 찾을 수 없습니다: &f" + type.key() + "/" + id));
            return true;
        }
        sender.sendMessage(color("&a콘텐츠 삭제 완료: &f" + removed.get().type().key() + "/" + removed.get().id()));
        return true;
    }

    private boolean rename(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/content rename <type> <id> <displayName>");
            return true;
        }

        ContentEntry entry = contentService.rename(parseType(args[1]), args[2], joinTail(args, 3));
        sender.sendMessage(color("&a표시 이름 변경 완료: &f" + entry.displayName()));
        return true;
    }

    private boolean setRadius(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("/content setradius <type> <id> <radius>");
            return true;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(color("&c반경은 숫자로 입력하세요."));
            return true;
        }

        ContentEntry entry = contentService.setRadius(parseType(args[1]), args[2], radius);
        sender.sendMessage(color("&a반경 변경 완료: &f" + entry.type().key() + "/" + entry.id()
                + " &7radius=&f" + entry.radius()));
        return true;
    }

    private boolean setSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&c게임 안에서만 사용할 수 있습니다."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("/content setspawn <type> <id>");
            return true;
        }

        ContentEntry entry = contentService.setSpawn(parseType(args[1]), args[2], spawn(player.getLocation()));
        sender.sendMessage(color("&a도착 지점 변경 완료: &f" + entry.type().key() + "/" + entry.id()));
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        List<ContentEntry> entries;
        if (args.length >= 2) {
            entries = contentService.list(parseType(args[1]));
        } else {
            entries = contentService.list();
        }

        if (entries.isEmpty()) {
            sender.sendMessage(color("&e등록된 콘텐츠가 없습니다."));
            return true;
        }

        sender.sendMessage(color("&a등록 콘텐츠:"));
        for (ContentEntry entry : entries) {
            sender.sendMessage(color("&7- &f" + entry.type().key() + "/" + entry.id()
                    + " &e" + entry.displayName()
                    + " &8(" + entry.world() + " "
                    + entry.centerX() + ", " + entry.centerY() + ", " + entry.centerZ()
                    + " / r=" + entry.radius() + ")"));
        }
        return true;
    }

    private boolean teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&c게임 안에서만 사용할 수 있습니다."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("/content tp <type> <id>");
            return true;
        }

        ContentEntry entry = contentService.find(parseType(args[1]), args[2])
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠를 찾을 수 없습니다."));
        World world = Bukkit.getWorld(entry.spawn().world());
        if (world == null) {
            sender.sendMessage(color("&c월드를 찾을 수 없습니다: &f" + entry.spawn().world()));
            return true;
        }

        ContentSpawn spawn = entry.spawn();
        player.teleport(new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
        sender.sendMessage(color("&a이동 완료: &f" + entry.type().key() + "/" + entry.id()));
        return true;
    }

    private PortalCuboidSelection resolveSelection(Player player) {
        try {
            return WorldEditSelectionProvider.getSelection(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ContentType parseType(String rawType) {
        return ContentType.fromKey(rawType)
                .orElseThrow(() -> new IllegalArgumentException("지원 type: neutral, casino, dungeon"));
    }

    private ContentArea area(PortalCuboidSelection selection) {
        return new ContentArea(
                selection.getWorldName(),
                selection.getMinX(),
                selection.getMinY(),
                selection.getMinZ(),
                selection.getMaxX(),
                selection.getMaxY(),
                selection.getMaxZ()
        );
    }

    private ContentSpawn spawn(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("현재 월드를 찾을 수 없습니다.");
        }
        return new ContentSpawn(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/content add <type> <id> [displayName]");
        sender.sendMessage("/content remove <type> <id>");
        sender.sendMessage("/content rename <type> <id> <displayName>");
        sender.sendMessage("/content setradius <type> <id> <radius>");
        sender.sendMessage("/content setspawn <type> <id>");
        sender.sendMessage("/content list [type]");
        sender.sendMessage("/content tp <type> <id>");
    }

    private boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("leeseolcore.content.admin") || sender.hasPermission("leeseolcore.admin");
    }

    private boolean usesType(String action) {
        return List.of("add", "remove", "rename", "setradius", "setspawn", "list", "tp")
                .contains(action.toLowerCase(Locale.ROOT));
    }

    private boolean usesExistingId(String action) {
        return List.of("remove", "rename", "setradius", "setspawn", "tp")
                .contains(action.toLowerCase(Locale.ROOT));
    }

    private List<String> typeKeys() {
        return Arrays.stream(ContentType.values()).map(ContentType::key).toList();
    }

    private List<String> matching(String token, List<String> values) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private String joinTail(String[] args, int start) {
        if (args.length <= start) {
            return null;
        }
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
