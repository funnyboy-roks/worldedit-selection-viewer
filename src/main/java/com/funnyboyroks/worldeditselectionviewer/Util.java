package com.funnyboyroks.worldeditselectionviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class Util {

    public static Region getSelection(@NotNull Player player) {

        LocalSession session = Util.getSession(player);

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

    public static LocalSession getSession(Player player) {

        com.sk89q.worldedit.entity.Player worldeditPlayer = BukkitAdapter.adapt(player);
        if (worldeditPlayer == null) return null;

        return WorldeditSelectionViewer.worldedit.getSessionManager().get(worldeditPlayer);
    }

    public static Location asLocation(BlockVector3 vec, World world) {
        return new Location(
            world,
            vec.getBlockX(),
            vec.getBlockY(),
            vec.getBlockZ()
        );
    }

    public static CompletableFuture<Boolean> isLatestVersion() {

        int serverVersion = Integer.parseInt(
            WorldeditSelectionViewer.instance
                .getDescription()
                .getVersion()
                .replaceAll("\\.|-SNAPSHOT|v", "")
        );

        return CompletableFuture.supplyAsync(() -> {

            try {
                URL url = new URL("https://api.modrinth.com/v2/project/K9JIhdio");
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonArray versions = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("versions");
                String version = versions.get(versions.size() - 1).getAsString();

                url = new URL("https://api.modrinth.com/v2/version/" + version);
                reader = new InputStreamReader(url.openStream());
                int latestVersion = Integer.parseInt(
                    JsonParser.parseReader(reader)
                        .getAsJsonObject()
                        .get("version_number")
                        .getAsString()
                        .replaceAll("\\.|-SNAPSHOT|v", "")
                );

                return latestVersion <= serverVersion;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


    }
}
