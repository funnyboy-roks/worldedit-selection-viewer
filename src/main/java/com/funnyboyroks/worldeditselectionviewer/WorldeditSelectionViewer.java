package com.funnyboyroks.worldeditselectionviewer;

import com.funnyboyroks.drawlib.renderer.ShapeRenderer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.farlandsmc.componentutils.ComponentColor;
import net.farlandsmc.componentutils.ComponentUtils;
import net.kyori.adventure.text.Component;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutionException;

public final class WorldeditSelectionViewer extends JavaPlugin {

    public static WorldeditSelectionViewer instance;
    public static WorldEdit                worldedit;

    public static NamespacedKey colourKey;
    public static NamespacedKey visKey;

    private static final Color DEFAULT_COLOUR = Color.YELLOW;
    private static final Visibility DEFAULT_VISIBLITY = Visibility.HOLDING_TOOL;

    public WorldeditSelectionViewer() {
        instance = this;
        worldedit = WorldEdit.getInstance();
        colourKey = NamespacedKey.fromString("selection-colour", this);
        visKey = NamespacedKey.fromString("selection-visibility", this);
    }

    @Override
    public void onEnable() {
        new Metrics(this, 16995);

        try {
            Util.isLatestVersion().thenAccept((latest) -> {
                if (!latest) {
                    this.getLogger().warning("Worldedit Selection Viewer has an update!");
                    this.getLogger().warning("Get it from https://modrinth.com/plugin/worldedit-selection-viewer");
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        loadCommands();

        ShapeRenderer defaultRenderer = new ShapeRenderer();
        defaultRenderer.setColor(DEFAULT_COLOUR);
        defaultRenderer.setForceShow(true);
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> {

            Bukkit.getOnlinePlayers().forEach(p -> {
                ShapeRenderer renderer = defaultRenderer;

                if (!p.hasPermission("worldedit-selection-viewer.view")) return;

                PersistentDataContainer pdc = p.getPersistentDataContainer();
                Visibility vis;
                if (pdc.has(visKey)) {
                    String str = pdc.get(visKey, PersistentDataType.STRING);
                    try {
                        vis = Visibility.valueOf(str);
                    } catch (IllegalArgumentException ex) {
                        this.getLogger().warning("Visibility invalid: " + str);
                        return;
                    }
                } else {
                    vis = Visibility.HOLDING_TOOL;
                }

                if (vis == Visibility.NEVER) {
                    return;
                }

                if (pdc.has(colourKey)) {
                    renderer = new ShapeRenderer();
                    renderer.setForceShow(true);
                    // Null doesn't matter because we check it above
                    Color colour = Util.colourFromString(pdc.get(colourKey, PersistentDataType.STRING));
                    if (colour == null) {
                        this.getLogger().warning("Colour invalid: " + pdc.get(colourKey, PersistentDataType.STRING));
                        return;
                    }
                    renderer.setColor(colour);
                }

                LocalSession session = Util.getSession(p);

                if (session == null) return;


                if (vis == Visibility.HOLDING_TOOL) {
                    Material wandMaterial;

                    try {
                        wandMaterial = Material.matchMaterial(session.getWandItem());
                    } catch (NullPointerException ex) { // Somehow, an NPE within WE causes this, so let's just default
                        wandMaterial = Material.WOODEN_AXE;
                    }

                    if (wandMaterial == null) return;

                    if (
                        p.getInventory().getItemInMainHand().getType() != wandMaterial // Main Hand
                        && p.getInventory().getItemInOffHand().getType() != wandMaterial // Offhand
                    ) {
                        return;
                    }
                }

                Region sel = Util.getSelection(p);

                if (sel == null) return;

                renderer.setReceivers(p);
                renderer.drawCuboid(
                    Util.asLocation(sel.getMinimumPoint(), p.getWorld()),
                    Util.asLocation(sel.getMaximumPoint(), p.getWorld()).add(1, 1, 1)
                );
            });

        }, 0, 2);
    }

    private static int commandWesvColourExecutor(CommandContext<CommandSourceStack> ctx) {
        final Color colour = ctx.getArgument("colour", Color.class);

        Entity executor = ctx.getSource().getExecutor();
        var sender = ctx.getSource().getSender();

        if (!(executor instanceof Player player)) {
            sender.sendPlainMessage("Only players may use this command.");
            return Command.SINGLE_SUCCESS;
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(WorldeditSelectionViewer.colourKey, PersistentDataType.STRING, Util.colourToString(colour));

        String humanName = Util.colourToString(colour);
        Component humanNameComp;

        if (humanName.startsWith("#")) {
            humanNameComp = ComponentColor.color(colour, humanName);
        } else {
            humanNameComp = ComponentUtils.hover(
                ComponentColor.color(colour, humanName),
                Component.text(Util.colourToHex(colour))
            );
        }

        sender.sendMessage(ComponentColor.green("Updated colour to {}!", humanNameComp));
        return Command.SINGLE_SUCCESS;
    }

    private static int commandWesvColourGetExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();

        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            sender.sendPlainMessage("Only players may use this command.");
            return Command.SINGLE_SUCCESS;
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();

        Color c;
        if (pdc.has(colourKey)) {
            c = Util.colourFromString(pdc.get(colourKey, PersistentDataType.STRING));
            if (c == null) c = DEFAULT_COLOUR;
        } else {
            c = DEFAULT_COLOUR;
        }

        String humanName = Util.colourToString(c);
        Component humanNameComp;

        if (humanName.startsWith("#")) {
            humanNameComp = ComponentColor.color(c, humanName);
        } else {
            humanNameComp = ComponentUtils.hover(
                ComponentColor.color(c, humanName),
                Component.text(Util.colourToHex(c))
            );
        }

        sender.sendMessage(ComponentColor.green("Colour is set to {}.", humanNameComp));
        return Command.SINGLE_SUCCESS;
    }

    private static int commandVisibility(CommandContext<CommandSourceStack> ctx, Visibility vis) {
        var sender = ctx.getSource().getSender();

        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            sender.sendPlainMessage("Only players may use this command.");
            return Command.SINGLE_SUCCESS;
        }

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(WorldeditSelectionViewer.visKey, PersistentDataType.STRING, vis.name());

        sender.sendMessage(ComponentColor.green("Updated selection visibility to {:aqua}!", vis));

        return Command.SINGLE_SUCCESS;
    }

    private void loadCommands() {
        var commandWesv = Commands.literal("wesv")
            .requires(p -> p.getSender().hasPermission("worldedit-selection-viewer.command"))
            .then(
                Commands.literal("colour").then(
                    Commands.argument("colour", new ColourArgument())
                        .executes(WorldeditSelectionViewer::commandWesvColourExecutor)
                )
                .executes(WorldeditSelectionViewer::commandWesvColourGetExecutor)
            )
            .then(
                Commands.literal("color").then(
                    Commands.argument("colour", new ColourArgument())
                        .executes(WorldeditSelectionViewer::commandWesvColourExecutor)
                )
                .executes(WorldeditSelectionViewer::commandWesvColourGetExecutor)
            )
            .then(
                Commands.literal("visibility")
                .then(Commands.literal("always").executes(ctx -> WorldeditSelectionViewer.commandVisibility(ctx, Visibility.ALWAYS)))
                .then(Commands.literal("never").executes(ctx -> WorldeditSelectionViewer.commandVisibility(ctx, Visibility.NEVER)))
                .then(Commands.literal("holding-tool").executes(ctx -> WorldeditSelectionViewer.commandVisibility(ctx, Visibility.HOLDING_TOOL)))
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();

                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        sender.sendPlainMessage("Only players may use this command.");
                        return Command.SINGLE_SUCCESS;
                    }

                    PersistentDataContainer pdc = player.getPersistentDataContainer();
                    Visibility vis;
                    if (pdc.has(visKey)) {
                        String str = pdc.get(visKey, PersistentDataType.STRING);
                        try {
                            vis = Visibility.valueOf(str);
                        } catch (IllegalArgumentException ex) {
                            this.getLogger().warning("Visibility invalid: " + str);
                            vis = DEFAULT_VISIBLITY;
                        }
                    } else {
                        vis = DEFAULT_VISIBLITY;
                    }

                    sender.sendMessage(ComponentColor.green("Visibility is set to {:aqua}.", vis));

                    return Command.SINGLE_SUCCESS;
                })
             )
            .build();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(commandWesv, "Configure worldedit-selection-viewer");
        });
    }

    @Override
    public void onDisable() {

    }

    public enum Visibility {
        ALWAYS, NEVER, HOLDING_TOOL
    }
}
