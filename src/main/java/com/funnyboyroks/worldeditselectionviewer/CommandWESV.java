package com.funnyboyroks.worldeditselectionviewer;

import net.farlandsmc.componentutils.ComponentColor;
import net.farlandsmc.componentutils.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
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
import java.util.stream.Stream;

public class CommandWESV implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ComponentColor.red("You must be in-game to run this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ComponentColor.red("Usage: /wesv <visibility|colour> [...args]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "colour", "color" -> { // /wesv colour <colour|hex>
                if (args.length != 2) {
                    sender.sendMessage(ComponentColor.red("Usage: /wesv colour <colour|#hex>"));
                    return true;
                }

                Color colour;
                try {
                    colour = Util.colourFromString(args[1]);
                } catch (Exception e) {
                    sender.sendMessage(ComponentColor.red("Invalid colour."));
                    return true;
                }

                if (colour == null) {
                    sender.sendMessage(ComponentColor.red("Invalid colour."));
                    return true;
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
            }
            case "visibility" -> { // /wesv visibility <always|never|...>
                if (args.length != 2) {
                    printVisibilityUsage(sender);
                    return true;
                }

                Visibility vis;
                try {
                    vis = Visibility.valueOf(Util.toEnumString(args[1]));
                } catch (IllegalArgumentException e) {
                    printVisibilityUsage(sender);
                    return true;
                }

                PersistentDataContainer pdc = player.getPersistentDataContainer();
                pdc.set(WorldeditSelectionViewer.visKey, PersistentDataType.STRING, vis.name());

                sender.sendMessage(ComponentColor.green("Updated selection visibility to {:aqua}!", vis));
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> Stream.of("colour", "color", "visibility")
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
            case 2 -> switch (args[0]) {
                case "colour", "color" -> Arrays.stream(Util.COLOURS)
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

                case "visibility" -> Arrays.stream(Visibility.values())
                    .map(Visibility::name)
                    .map(Util::fromEnumString)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }

    public void printVisibilityUsage(CommandSender sender) {
        sender.sendMessage(
            ComponentColor.red(
                "Usage: /wesv visibility <{}>",
                Arrays.stream(Visibility.values())
                    .map(Visibility::name)
                    .map(Util::fromEnumString)
                    .collect(Collectors.joining("|"))
            )
        );
    }

    public enum Visibility {
        ALWAYS, NEVER, HOLDING_TOOL
    }
}
