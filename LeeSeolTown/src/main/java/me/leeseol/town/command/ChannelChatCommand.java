package me.leeseol.town.command;

import me.leeseol.town.model.ChatMode;
import me.leeseol.town.service.TownService;
import me.leeseol.town.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class ChannelChatCommand implements CommandExecutor {
    private final TownService townService;
    private final ChatMode mode;

    public ChannelChatCommand(TownService townService, ChatMode mode) {
        this.townService = townService;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        if (args.length == 0) {
            townService.setChatMode(player, mode);
            return true;
        }

        Component message = Text.component(String.join(" ", Arrays.asList(args)));
        if (mode == ChatMode.TOWN) {
            townService.sendTownChat(player, message);
        } else if (mode == ChatMode.NATION) {
            townService.sendNationChat(player, message);
        }
        return true;
    }
}
