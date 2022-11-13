package com.funnyboyroks.worldeditselectionviewer;

import com.funnyboyroks.drawlib.renderer.ShapeRenderer;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldeditSelectionViewer extends JavaPlugin {

    public static WorldeditSelectionViewer instance;
    public static WorldEdit                worldedit;

    public WorldeditSelectionViewer() {
        instance = this;
        worldedit = WorldEdit.getInstance();
    }

    @Override
    public void onEnable() {
        Material wandMaterial = Material.matchMaterial(worldedit.getConfiguration().wandItem);
        ShapeRenderer renderer = new ShapeRenderer();
        renderer.setColor(Color.YELLOW);
        renderer.setForceShow(true);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, () -> {

            Bukkit.getOnlinePlayers().forEach(p -> {

                if (!p.hasPermission("worldedit-selection-viewer.view")) return;

                if (p.getInventory().getItemInMainHand().getType() != wandMaterial && p.getInventory().getItemInOffHand().getType() != wandMaterial) {
                    return;
                }

                Region sel = Util.getSelection(p);

                if(sel == null) return;

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
