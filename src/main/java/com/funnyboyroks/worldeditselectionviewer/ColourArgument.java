package com.funnyboyroks.worldeditselectionviewer;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.farlandsmc.componentutils.ComponentColor;
import net.farlandsmc.componentutils.ComponentUtils;

public class ColourArgument implements CustomArgumentType<Color, String> {

    private static final DynamicCommandExceptionType ERROR_INVALID_COLOUR = new DynamicCommandExceptionType(colour -> {
        return MessageComponentSerializer.message().serialize(ComponentUtils.format("'{}' is not a valid colour.", colour));
    });

	@Override
	public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.greedyString();
	}

	@Override
	public @NotNull Color parse(@NotNull StringReader reader) throws CommandSyntaxException {
        // We need to read '#<colour>' which is not valid in an unquoted string normally.
        char start = reader.read();
        String rest = reader.readUnquotedString();
        String s = start + rest;

        Color colour;
        try {
            colour = Util.colourFromString(s);
        } catch (Exception e) {
            throw ERROR_INVALID_COLOUR.create(s);
        }
        if (colour == null) throw ERROR_INVALID_COLOUR.create(s);

        return colour;
	}

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(
        @NotNull CommandContext<S> context,
        @NotNull SuggestionsBuilder builder
    ) {
        Util.COLOUR_MAP.forEach((k, v) -> {
            String s = k.toLowerCase();
            if (!s.startsWith(builder.getRemainingLowerCase())) return;
            builder.suggest(
                s,
                MessageComponentSerializer.message().serialize(ComponentColor.color(v, s))
            );
        });
        return builder.buildFuture();
    }
}
