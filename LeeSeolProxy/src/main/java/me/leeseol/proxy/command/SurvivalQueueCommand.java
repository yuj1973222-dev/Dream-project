package me.leeseol.proxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import me.leeseol.proxy.queue.SurvivalQueueController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SurvivalQueueCommand implements SimpleCommand {
    private final SurvivalQueueController queueController;

    public SurvivalQueueCommand(SurvivalQueueController queueController) {
        this.queueController = queueController;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text(queueController.settings().nonPlayerMessage(), NamedTextColor.RED));
            return;
        }

        queueController.requestSurvival(player);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}
