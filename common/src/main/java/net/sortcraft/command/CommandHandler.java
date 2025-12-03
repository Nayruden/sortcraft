package net.sortcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/**
 * Registers all /sort subcommands.
 */
public final class CommandHandler {
    private CommandHandler() {}

    private static final String SIGN_PREFIX = "[";
    private static final String SIGN_SUFFIX = "]";
    private static final String INPUT_SIGN_TEXT = SIGN_PREFIX + "input" + SIGN_SUFFIX;

    public static String getInputSignText() {
        return INPUT_SIGN_TEXT;
    }

    public static String formatSignText(String text) {
        return SIGN_PREFIX + text + SIGN_SUFFIX;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sort")
                        .then(Commands.literal("input")
                                .executes(ctx -> SortInputCommand.execute(ctx.getSource(), false)))
                        .then(Commands.literal("preview")
                                .executes(ctx -> SortInputCommand.execute(ctx.getSource(), true)))
                        .then(Commands.literal("diagnostics")
                                .executes(DiagCommand::execute))
                        .then(Commands.literal("whereis")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(WhereIsCommand::execute)))
                        .then(Commands.literal("category")
                                .then(Commands.argument("item", StringArgumentType.greedyString())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                        .executes(CategoryCommand::execute)))
                        .then(Commands.literal("help")
                                .executes(CommandHandler::executeHelp))
                        .then(Commands.literal("dump")
                                .executes(DumpCommand::execute))
                        .then(Commands.literal("reload")
                                .executes(ReloadCommand::execute))
        );
    }

    private static int executeHelp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        String helpMessage = String.format("""
                Sort Command Help:
                /sort input           - Sorts items from the closest input chest (chest must have a sign with '%s')
                /sort preview         - Shows a preview of what will be sorted and where
                /sort diagnostics     - Generates a diagnostics report as YAML
                /sort whereis <item>  - Finds chests that contain the specified item
                /sort category <item> - Shows the sorting category for the specified item
                /sort reload          - Reloads category configurations from config files
                /sort dump            - Generates JSON files with all item tags from the registry
                /sort help            - Shows this help message

                All commands support autocomplete. Use TAB for suggestions.
                """, INPUT_SIGN_TEXT);
        context.getSource().sendSuccess(() -> Component.literal(helpMessage), false);
        return 1;
    }
}

