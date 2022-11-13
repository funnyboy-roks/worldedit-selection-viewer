package com.funnyboyroks.worldeditselectionviewer;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Util {

    public static Region getSelection(@NotNull Player player) {
        com.sk89q.worldedit.entity.Player worldeditPlayer = BukkitAdapter.adapt(player);
        if (worldeditPlayer == null) return null;

        LocalSession session = WorldeditSelectionViewer.worldedit.getSessionManager().get(worldeditPlayer);
        if (session == null) return null;

        Region sel;
        try {
            sel = session.getSelection();
        } catch (IncompleteRegionException e) {
            return null;
        }
        if (sel == null) return null;

        if (!BukkitAdapter.asBukkitWorld(sel.getWorld()).getWorld().equals(player.getWorld())) return null;

        return sel;
    }

    static Location asLocation(BlockVector3 vec, World world) {
        return new Location(
            world,
            vec.getBlockX(),
            vec.getBlockY(),
            vec.getBlockZ()
        );
    }
}
