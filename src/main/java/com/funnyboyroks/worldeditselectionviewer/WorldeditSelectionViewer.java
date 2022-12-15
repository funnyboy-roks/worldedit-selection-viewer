package com.funnyboyroks.worldeditselectionviewer;

import com.funnyboyroks.drawlib.renderer.ShapeRenderer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutionException;

public final class WorldeditSelectionViewer extends JavaPlugin {

    public static WorldeditSelectionViewer instance;
    public static WorldEdit                worldedit;

    public WorldeditSelectionViewer() {
        instance = this;
        worldedit = WorldEdit.getInstance();
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

//        Material wandMaterial = Material.matchMaterial(worldedit.getConfiguration().wandItem);
        ShapeRenderer renderer = new ShapeRenderer();
        renderer.setColor(Color.YELLOW);
        renderer.setForceShow(true);
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(instance, () -> {

            Bukkit.getOnlinePlayers().forEach(p -> {

                if (!p.hasPermission("worldedit-selection-viewer.view")) return;

                LocalSession session = Util.getSession(p);
                if (session == null) return;

                Material wandMaterial = Material.matchMaterial(session.getWandItem());

                if (wandMaterial == null) return;

                if (
                    p.getInventory().getItemInMainHand().getType() != wandMaterial // Main Hand
                    && p.getInventory().getItemInOffHand().getType() != wandMaterial // Offhand
                ) {
                    return;
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

    @Override
    public void onDisable() {

    }

}
