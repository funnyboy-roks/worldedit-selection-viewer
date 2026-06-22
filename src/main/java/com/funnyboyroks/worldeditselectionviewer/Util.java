package com.funnyboyroks.worldeditselectionviewer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Util {
    public static final Map<String, Color> COLOUR_MAP = ImmutableMap.<String, Color>builder()
        .put("WHITE", Color.WHITE)
        .put("SILVER", Color.SILVER)
        .put("GRAY", Color.GRAY)
        .put("BLACK", Color.BLACK)
        .put("RED", Color.RED)
        .put("MAROON", Color.MAROON)
        .put("YELLOW", Color.YELLOW)
        .put("OLIVE", Color.OLIVE)
        .put("LIME", Color.LIME)
        .put("GREEN", Color.GREEN)
        .put("AQUA", Color.AQUA)
        .put("TEAL", Color.TEAL)
        .put("BLUE", Color.BLUE)
        .put("NAVY", Color.NAVY)
        .put("FUCHSIA", Color.FUCHSIA)
        .put("PURPLE", Color.PURPLE)
        .put("ORANGE", Color.ORANGE)
        .build();

    // The same as COLOUR_MAP, but the value and key are flipped
    // This has no conflicts, because no two keys have the same value
    private static final Map<Color, String> REV_COLOUR_MAP = COLOUR_MAP
        .entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    public static final String[] COLOURS = COLOUR_MAP.keySet().toArray(new String[0]);

    public static Color colourFromString(String str) {
        if (str.startsWith("#")) {
            str = str.substring(1);
            return Color.fromRGB(Integer.parseInt(str, 16));
        }

        return COLOUR_MAP.getOrDefault(str.toUpperCase(), null);
    }

    public static String colourToString(Color col) {
        String named = REV_COLOUR_MAP.get(col);
        if (named == null) {
            return colourToHex(col);
        } else {
            return named.toLowerCase();
        }
    }

    public static String colourToHex(Color col) {
        return "#%06x".formatted(col.asRGB());
    }

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
            vec.x(),
            vec.y(),
            vec.z()
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
                URL url = URI.create("https://api.modrinth.com/v2/project/K9JIhdio").toURL();
                InputStreamReader reader = new InputStreamReader(url.openStream());
                JsonArray versions = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("versions");
                String version = versions.get(versions.size() - 1).getAsString();

                url = URI.create("https://api.modrinth.com/v2/version/" + version).toURL();
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
                WorldeditSelectionViewer.instance
                    .getLogger()
                    .severe("Unable to contact Modrinth API to check version!");
                return true;
            }
        });


    }

    public static String fromEnumString(String str) {
        return str.toLowerCase().replace('_', '-');
    }

    public static String toEnumString(String str) {
        return str.toUpperCase().replace('-', '_');
    }
}
