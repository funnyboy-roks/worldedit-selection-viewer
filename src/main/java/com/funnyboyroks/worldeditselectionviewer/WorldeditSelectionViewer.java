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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutionException;

public final class WorldeditSelectionViewer extends JavaPlugin {

    public static WorldeditSelectionViewer instance;
    public static WorldEdit                worldedit;

    public static NamespacedKey colourKey;
    public static NamespacedKey visKey;
    //public static NamespacedKey refreshRateKey;

    private static int s_counter;
    private static final int MAX_COUNTER = 60;

    private static Color defaultColour;
    private static Visibility defaultVisibility;
    private static int defaultRefreshRate;
    private static double defaultStepSize;
    private static boolean defaultOptimize;

    public WorldeditSelectionViewer() {
        instance = this;
        worldedit = WorldEdit.getInstance();
        colourKey = NamespacedKey.fromString("selection-colour", this);
        visKey = NamespacedKey.fromString("selection-visibility", this);
        //refreshRateKey = NamespacedKey.fromString("refresh-rate", this);
        s_counter = 0;
        defaultColour = Color.YELLOW;
        defaultVisibility = Visibility.HOLDING_TOOL;
        defaultRefreshRate = 2;
        defaultStepSize = 0.1;
        defaultOptimize = false;
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

        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        String keyColour = config.getString("default-color", "yellow");
        defaultColour = Util.colourFromString(keyColour);
        if (defaultColour == null) {
            this.getLogger().warning("Colour invalid: " + keyColour);
            defaultColour = Color.YELLOW;
        }
        String keyVisibility = config.getString("default-visibility", "holding-tool");
        try {
            defaultVisibility = Visibility.valueOf(Util.toEnumString(keyVisibility));
        } catch (IllegalArgumentException ex) {
            this.getLogger().warning("Visibility invalid: " + keyVisibility);
            defaultVisibility = Visibility.HOLDING_TOOL;
        }
        defaultRefreshRate = config.getInt("refresh-rate", 2);
        if (defaultRefreshRate < 1 || defaultRefreshRate > 60) {
            this.getLogger().warning("Refresh rate invalid: " + defaultRefreshRate + " (range: 1 - 60))");
            defaultRefreshRate = 2;
        }
        defaultStepSize = config.getDouble("step-size", 0.1);
        if (defaultStepSize <= 0.0) {
            this.getLogger().warning("Step size invalid: " + defaultStepSize);
            defaultStepSize = 0.1;
        }
        defaultOptimize = config.getBoolean("optimise-lines", false);

        ShapeRenderer defaultRenderer = new ShapeRenderer();
        defaultRenderer.setColor(defaultColour);
        defaultRenderer.setForceShow(true);
        defaultRenderer.setStepSize(defaultStepSize);
        defaultRenderer.setOptimize(defaultOptimize);
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            if (++s_counter >= MAX_COUNTER) s_counter = 0;
            Bukkit.getOnlinePlayers().forEach(p -> {
                ShapeRenderer renderer = defaultRenderer;

                if (!p.hasPermission("worldedit-selection-viewer.view")) return;

                PersistentDataContainer pdc = p.getPersistentDataContainer();

                int rate = defaultRefreshRate;
                //if (pdc.has(refreshRateKey)) rate = pdc.get(refreshRateKey, PersistentDataType.INTEGER);
                if (s_counter % rate > 0) return;

                Visibility vis = defaultVisibility;
                if (pdc.has(visKey)) {
                    String str = pdc.get(visKey, PersistentDataType.STRING);
                    try {
                        vis = Visibility.valueOf(str);
                    } catch (IllegalArgumentException ex) {
                        this.getLogger().warning("Visibility invalid: " + str);
                        return;
                    }
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

        }, 0, 1);
    }

    private void loadCommands() {
        this.getCommand("wesv").setExecutor(new CommandWESV());
    }

    @Override
    public void onDisable() {

    }

}
