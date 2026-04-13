/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Admin subcommand that purges (deletes) unused or old pets from the repository.
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code /petadmin purge} -- deletes all unused pets (no time filter)</li>
 *   <li>{@code /petadmin purge <amount> <years|days|hours|minutes>} -- deletes pets older than
 *       the specified duration (e.g. {@code /petadmin purge 6 months})</li>
 * </ul>
 *
 * <p>The cleanup is delegated to the repository's {@code cleanup()} method, which handles
 * the actual database deletion based on the computed cutoff timestamp.</p>
 *
 * <p>Requires the {@code MyPet.admin} permission.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionPurge {

    /**
     * Builds the Brigadier command tree for the {@code purge} subcommand.
     *
     * <p>The tree has two execution paths:</p>
     * <ol>
     *   <li>No arguments: executes cleanup with timestamp {@code -1} (delete all unused pets).</li>
     *   <li>{@code <amount> <unit>}: computes a cutoff timestamp by subtracting the specified
     *       duration from the current time, then passes it to the cleanup method. Time units
     *       are generated dynamically from the {@link TimeUnit} enum.</li>
     * </ol>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code purge} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Purge",
                "/petadmin purge",
                CommandCategory.ADMIN,
                38,
                player -> Permissions.has(player, "MyPet.admin", false)
        ));

        // Build a time-unit node: /petadmin purge <amount> <years|days|hours|minutes>
        var amountArg = Commands.argument("amount", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            amountArg.then(Commands.literal(unit.name().toLowerCase())
                    .executes(ctx -> {
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        long cutoffMillis = Instant.now()
                                .minus(amount, unit.chronoUnit)
                                .toEpochMilli();
                        executeCleanup(ctx.getSource().getSender(), cutoffMillis);
                        return Command.SINGLE_SUCCESS;
                    }));
        }

        return Commands.literal("purge")
                // /petadmin purge (no args — delete all unused)
                .executes(ctx -> {
                    executeCleanup(ctx.getSource().getSender(), -1);
                    return Command.SINGLE_SUCCESS;
                })
                .then(amountArg)
                .build();
    }

    /**
     * Supported time units for specifying the purge cutoff duration.
     * Each entry maps to a {@link ChronoUnit} for use with {@link Instant#minus}.
     */
    private enum TimeUnit {
        YEARS(ChronoUnit.YEARS),
        DAYS(ChronoUnit.DAYS),
        HOURS(ChronoUnit.HOURS),
        MINUTES(ChronoUnit.MINUTES);

        final ChronoUnit chronoUnit;

        TimeUnit(ChronoUnit chronoUnit) {
            this.chronoUnit = chronoUnit;
        }
    }

    /**
     * Executes the repository cleanup operation.
     *
     * <p>If {@code timestamp} is {@code -1}, all unused pets are deleted regardless of age.
     * Otherwise, only pets older than the given epoch-millisecond cutoff are removed.
     * A human-readable formatted date is shown to the sender before the operation begins.</p>
     *
     * @param sender    the command sender (for progress and result messages)
     * @param timestamp the cutoff time in epoch milliseconds, or {@code -1} to delete all unused pets
     */
    private void executeCleanup(CommandSender sender, long timestamp) {
        sender.sendMessage(MessageUtil.prefixed(Component.text("cleaning up MyPet database...")));

        if (timestamp == -1) {
            sender.sendMessage(MessageUtil.prefixed(Component.text("delete unused MyPets...")));
        } else {
            String formatted = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()
            ).format(DateTimeFormatter.RFC_1123_DATE_TIME);
            sender.sendMessage(MessageUtil.prefixed(Component.text("delete MyPets older than " + formatted + "...")));
        }

        MyPetApi.getRepository().cleanup(timestamp).thenAccept(value -> Bukkit.getScheduler().runTask(MyPetApi.getPlugin(), () ->
                sender.sendMessage(MessageUtil.prefixed(Component.text("removed " + value + " MyPets.")))));
    }
}
