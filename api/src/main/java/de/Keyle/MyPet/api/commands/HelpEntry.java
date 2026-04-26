package de.Keyle.MyPet.api.commands;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Represents a single entry in the {@code /mypet help} command output.
 * Each entry maps a command to its translated description, category, display order,
 * and an optional visibility predicate that controls whether the entry is shown to a given player.
 *
 * <p>Entries are registered via {@link HelpRegistry#register(HelpEntry)} and are typically
 * created during command registration in {@code BuiltInCommands.register()}.
 *
 * @param translationKey the translation key used to look up the localized help description
 *                       for this command (e.g., {@code "Name.Help.Description"})
 * @param command        the command syntax string displayed to the player (e.g., {@code "/mypet name <name>"})
 * @param category       the {@link CommandCategory} this entry belongs to, used for grouping in help output
 * @param order          the sort order within help listings; lower values appear first
 * @param visibleTo      an optional predicate that determines whether a player can see this entry;
 *                       if {@code null}, the entry is visible to all players
 */
public record HelpEntry(
        String translationKey,
        String command,
        CommandCategory category,
        int order,
        @Nullable Predicate<Player> visibleTo
) {
    /**
     * Checks whether this help entry should be visible to the given player.
     * If no visibility predicate was provided (i.e., {@link #visibleTo} is {@code null}),
     * the entry is considered visible to everyone.
     *
     * @param player the player to check visibility for
     * @return {@code true} if the entry is visible to the player, {@code false} otherwise
     */
    public boolean isVisibleTo(Player player) {
        return visibleTo == null || visibleTo.test(player);
    }
}
