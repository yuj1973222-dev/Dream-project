package me.leeseol.town.listener;

import com.sk89q.worldedit.WorldEditException;
import java.io.IOException;
import java.util.Optional;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.structure.PlacementFailure;
import me.leeseol.town.structure.StructureBounds;
import me.leeseol.town.structure.StructureCoreItemService;
import me.leeseol.town.structure.StructureDefinition;
import me.leeseol.town.structure.StructurePlacementRules;
import me.leeseol.town.structure.StructureSelectionGui;
import me.leeseol.town.structure.StructureUndoEdit;
import me.leeseol.town.structure.StructureUndoRecord;
import me.leeseol.town.structure.WorldEditStructurePaster;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class StructurePlacementListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final StructureCoreItemService itemService;
    private final StructureSelectionGui gui;
    private final WorldEditStructurePaster paster;

    public StructurePlacementListener(
            LeeSeolTownPlugin plugin,
            StructureCoreItemService itemService,
            StructureSelectionGui gui,
            WorldEditStructurePaster paster
    ) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.gui = gui;
        this.paster = paster;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.structureRegistry().enabled() || !rightClick(event.getAction()) || !itemService.isCore(event.getItem())) {
            return;
        }
        String selected = itemService.selectedStructureId(event.getItem());
        if (selected == null || event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            gui.open(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof StructureSelectionGui.Holder holder)) {
            return;
        }
        event.setCancelled(true);
        String structureId = holder.structureIdAt(event.getRawSlot());
        if (structureId == null) {
            return;
        }
        StructureDefinition definition = plugin.structureRegistry().get(structureId);
        if (definition == null) {
            return;
        }
        if (!canUse(player, definition)) {
            player.sendMessage(plugin.msg("structure-no-permission"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!itemService.isCore(hand)) {
            player.sendMessage(plugin.msg("structure-core-required"));
            return;
        }
        ItemStack selected = itemService.select(hand, definition);
        if (selected == null) {
            player.sendMessage(plugin.msg("structure-missing-itemsadder-block").replace("%structure%", definition.id()));
            return;
        }
        player.getInventory().setItemInMainHand(selected);
        player.closeInventory();
        player.sendMessage(plugin.msg("structure-selected").replace("%structure%", definition.name()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.structureRegistry().enabled() || !itemService.isCore(item)) {
            return;
        }
        Player player = event.getPlayer();
        String structureId = itemService.selectedStructureId(item);
        StructureDefinition definition = plugin.structureRegistry().get(structureId);
        if (definition == null) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-select-first"));
            return;
        }
        if (!canUse(player, definition)) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-no-permission"));
            return;
        }

        boolean force = player.hasPermission("leeseoltown.structure.admin");
        Block block = event.getBlockPlaced();
        ClaimKey claim = ClaimKey.from(block.getLocation());
        Nation nation = plugin.townService().playerNation(player);
        String nationId = nation == null ? null : nation.id();
        boolean initialNationCore = definition.nationCore() && nation != null && nation.beaconClaim() == null;
        boolean createdNationCoreClaim = initialNationCore && plugin.townService().claimTown(claim) == null;

        if (definition.nationCore() && !force && !plugin.townService().canPlaceNationCoreStructure(player, claim)) {
            event.setCancelled(true);
            return;
        }

        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                definition,
                claim,
                nationId,
                plugin.townService()::nationIdForClaim,
                plugin.townService().nationHasOpenWar(nation),
                force,
                initialNationCore
        );
        if (failure.isPresent()) {
            event.setCancelled(true);
            player.sendMessage(messageFor(failure.get()));
            return;
        }
        if (!definition.hasSchematic()) {
            handleUnlinkedSchematic(event, player, definition, claim, initialNationCore);
            return;
        }
        if (!paster.schematicExists(definition)) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-missing-schematic").replace("%structure%", definition.schematic()));
            plugin.getLogger().warning("Configured schematic is missing for structure "
                    + definition.id() + ": " + definition.schematic());
            return;
        }
        if (!paster.itemsAdderBlockAvailable(definition)) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-missing-itemsadder-block").replace("%structure%", definition.itemsAdderBlock()));
            return;
        }

        int anchorX = StructureBounds.anchorBlockX(claim.x());
        int anchorZ = StructureBounds.anchorBlockZ(claim.z());
        int anchorY = block.getY();
        if (!paster.isAreaClear(block.getWorld(), definition, anchorX, anchorY, anchorZ, force, block)) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-obstructed"));
            return;
        }

        event.setCancelled(true);
        EquipmentSlot hand = event.getHand();
        plugin.getServer().getScheduler().runTask(plugin, () -> pasteLater(
                player,
                hand,
                block,
                definition,
                anchorX,
                anchorY,
                anchorZ,
                claim,
                nationId,
                initialNationCore,
                createdNationCoreClaim
        ));
    }

    private void handleUnlinkedSchematic(
            BlockPlaceEvent event,
            Player player,
            StructureDefinition definition,
            ClaimKey claim,
            boolean registerNationCore
    ) {
        if (!definition.nationCore()) {
            event.setCancelled(true);
            player.sendMessage(plugin.msg("structure-schematic-unlinked").replace("%structure%", definition.id()));
            plugin.getLogger().info("No linked schematic for structure " + definition.id() + "; skipping WorldEdit paste.");
            return;
        }
        player.sendMessage(plugin.msg("structure-schematic-unlinked").replace("%structure%", definition.id()));
        plugin.getLogger().info("No linked schematic for nation core; keeping core block placement and skipping WorldEdit paste.");
        if (registerNationCore) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.townService().registerNationCoreStructure(player, claim));
        }
    }

    private void pasteLater(
            Player player,
            EquipmentSlot hand,
            Block block,
            StructureDefinition definition,
            int anchorX,
            int anchorY,
            int anchorZ,
            ClaimKey claim,
            String nationId,
            boolean registerNationCore,
            boolean removeCreatedClaim
    ) {
        try {
            StructureUndoEdit undoEdit = paster.pasteWithUndo(block.getWorld(), definition, anchorX, anchorY, anchorZ);
            if (!paster.placeItemsAdderMarker(block.getWorld(), definition, anchorX, anchorY, anchorZ)) {
                undoAfterMarkerFailure(definition, undoEdit);
                player.sendMessage(plugin.msg("structure-missing-itemsadder-block").replace("%structure%", definition.itemsAdderBlock()));
                return;
            }
            boolean registeredNationCore = false;
            if (registerNationCore) {
                registeredNationCore = plugin.townService().registerNationCoreStructure(player, claim);
            }
            consumeOne(player, hand);
            plugin.structureUndoService().remember(new StructureUndoRecord(
                    player.getUniqueId(),
                    player.getName(),
                    definition.id(),
                    definition.name(),
                    registeredNationCore ? nationId : null,
                    registeredNationCore ? claim : null,
                    registeredNationCore && removeCreatedClaim,
                    undoEdit,
                    System.currentTimeMillis()
            ));
            player.sendMessage(plugin.msg("structure-placed").replace("%structure%", definition.name()));
        } catch (IOException | WorldEditException exception) {
            player.sendMessage(plugin.msg("structure-paste-failed").replace("%structure%", definition.id()));
            plugin.getLogger().warning("Failed to paste structure " + definition.id() + ": " + exception.getMessage());
        }
    }

    private void undoAfterMarkerFailure(StructureDefinition definition, StructureUndoEdit undoEdit) {
        try {
            undoEdit.undo();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Failed to undo structure after marker placement failure "
                    + definition.id() + ": " + exception.getMessage());
        }
    }

    private boolean canUse(Player player, StructureDefinition definition) {
        return player.hasPermission("leeseoltown.structure.admin")
                || definition.permission().isBlank()
                || player.hasPermission(definition.permission());
    }

    private void consumeOne(Player player, EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack current = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!itemService.isCore(current)) {
            return;
        }
        if (current.getAmount() <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
            return;
        }
        current.setAmount(current.getAmount() - 1);
    }

    private String messageFor(PlacementFailure failure) {
        return switch (failure) {
            case NOT_IN_NATION -> plugin.msg("structure-not-in-nation");
            case PLACEMENT_CHUNK_NOT_OWNED -> plugin.msg("structure-placement-chunk-not-owned");
            case NATION_IN_WAR -> plugin.msg("structure-nation-in-war");
            case NATION_CORE_OUTSIDE_CHUNK -> plugin.msg("structure-core-outside-chunk");
            case OUTSIDE_NATION_TERRITORY -> plugin.msg("structure-outside-territory");
            case OBSTRUCTED -> plugin.msg("structure-obstructed");
            case MISSING_SCHEMATIC -> plugin.msg("structure-missing-schematic");
            case NO_PERMISSION -> plugin.msg("structure-no-permission");
        };
    }

    private boolean rightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
