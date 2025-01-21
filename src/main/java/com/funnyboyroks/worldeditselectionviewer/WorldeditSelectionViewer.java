package com.funnyboyroks.worldeditselectionviewer;

import com.funnyboyroks.drawlib.renderer.ShapeRenderer;
import com.funnyboyroks.worldeditselectionviewer.CommandWESV.Visibility;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutionException;

public final class WorldeditSelectionViewer extends JavaPlugin {

    public static WorldeditSelectionViewer instance;
    public static WorldEdit                worldedit;

    public static NamespacedKey colourKey;
    public static NamespacedKey visKey;
    private static int counter;

    public WorldeditSelectionViewer() {
        instance = this;
        worldedit = WorldEdit.getInstance();
        colourKey = NamespacedKey.fromString("selection-colour", this);
        visKey = NamespacedKey.fromString("selection-visibility", this);
        counter = 0;
    }

    @Override
    public void onEnable() {
        Metrics metrics = new Metrics(this, 16995);

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
        defaultRenderer.setColor(Color.YELLOW);
        defaultRenderer.setForceShow(true);
        defaultRenderer.setStepSize(0.2);
        defaultRenderer.setOptimize(true);
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            counter++;
            counter &= 3;

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

                if (counter != 0) return;
                // draw 1/4 freq.

                renderer.setReceivers(p);
                renderer.drawCuboid(
                    Util.asLocation(sel.getMinimumPoint(), p.getWorld()),
                    Util.asLocation(sel.getMaximumPoint(), p.getWorld()).add(1, 1, 1)
                );


            });

        }, 0, 2);
    }

    private void loadCommands() {
        this.getCommand("wesv").setExecutor(new CommandWESV());
    }

    @Override
    public void onDisable() {

    }

}
