package com.funnyboyroks.worldeditselectionviewer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWESV implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You must be in-game to run this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /wesv colour <colour|#hex>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "colour", "color" -> {
                if (args.length == 1) {
                    sender.sendMessage("Usage: /wesv colour <colour|#hex>");
                    return true;
                }

                Color colour;
                try {
                    colour = Util.colourFromString(args[1]);
                } catch (Exception e) {
                    sender.sendMessage("Invalid Colour.");
                    return true;
                }

                if (colour == null) {
                    sender.sendMessage("Invalid Colour.");
                    return false;
                }

                PersistentDataContainer pdc = player.getPersistentDataContainer();
                pdc.set(WorldeditSelectionViewer.colourKey, PersistentDataType.STRING, Util.colourToString(colour));

                sender.sendMessage(
                    Component.text("Updated colour to ", NamedTextColor.GREEN)
                        .append(Component.text(Util.colourToString(colour), TextColor.color(colour.asRGB())))
                        .append(Component.text("!"))
                );
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> List.of("colour", "color");
            case 2 -> Arrays.stream(Util.COLOURS)
                .filter(s -> s.startsWith(args[1]))
                .map(String::toLowerCase).collect(Collectors.toList());
            default -> Collections.emptyList();
        };
    }
}
